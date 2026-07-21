package id.qrispay.notif

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Membaca notifikasi e-wallet / m-banking lalu meneruskan teksnya ke webhook
 * QRISPay. Server yang mengekstrak nominal & mencocokkan transaksi.
 */
class PaymentNotificationListener : NotificationListenerService() {

    companion object {
        const val TEST_MARKER = "QRISPAY_TEST_PING"

        // package aplikasi pembayaran yang dipantau
        val WATCHED = listOf(
            "id.dana", "com.gojek.gopay", "com.gojek.app", "com.gopay.merchant",
            "ovo.id", "com.shopeepay.id", "com.shopee.id", "com.linkaja.app",
            "com.telkom.mwallet", "com.bca", "com.bri", "id.co.bri.brimo",
            "id.bmri.livin", "com.bni", "com.btpn.dc", "com.jago.digitalBanking"
        )
        // kata kunci uang masuk
        val CREDIT_HINTS = listOf(
            "menerima", "diterima", "masuk", "pembayaran", "berhasil menerima",
            "credit", "kredit", "top up", "topup", "dana masuk", "transaksi masuk"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: return

            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            val full = listOf(title, text, big).filter { it.isNotBlank() }.joinToString(" | ")

            // Notifikasi dari APP SENDIRI (mis. "QRIS baru dibuat", atau tes ping):
            // jangan pernah diteruskan sebagai pembayaran — cukup catat tes-nya.
            if (pkg == packageName) {
                if (full.contains(TEST_MARKER)) {
                    prefs().edit().putLong("test_seen_at", System.currentTimeMillis()).apply()
                    Log.i("QRISPayNotif", "test notif diterima listener")
                }
                return
            }

            val watchAll = prefs().getBoolean("watch_all", false)
            if (!watchAll && WATCHED.none { pkg.contains(it) }) return
            if (full.isBlank()) return

            val lower = full.lowercase()
            val looksCredit = CREDIT_HINTS.any { lower.contains(it) }
            // kalau bukan watchAll, hanya kirim yang terlihat seperti uang masuk
            if (!watchAll && !looksCredit) return

            forward(full, pkg)
        } catch (e: Exception) {
            Log.e("QRISPayNotif", "err", e)
        }
    }

    private fun prefs() =
        getSharedPreferences("cfg", Context.MODE_PRIVATE)

    // ---- Poll transaksi baru -> notifikasi pembuatan QRIS ----
    private val pollHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val pollEveryMs = 60_000L
    private val pollTask = object : Runnable {
        override fun run() {
            pollNewInvoices()
            pollHandler.postDelayed(this, pollEveryMs)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        pollHandler.removeCallbacks(pollTask)
        pollHandler.post(pollTask)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        pollHandler.removeCallbacks(pollTask)
    }

    override fun onDestroy() {
        pollHandler.removeCallbacks(pollTask)
        super.onDestroy()
    }

    private fun pollNewInvoices() {
        val p = prefs()
        val server = p.getString("server", "")?.trimEnd('/') ?: ""
        val license = p.getString("license", "") ?: ""
        if (server.isBlank() || license.isBlank()) return

        thread {
            try {
                val conn = URL("$server/api/transactions?limit=20").openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("x-license-key", license)
                conn.setRequestProperty("Accept", "application/json")
                val code = conn.responseCode
                if (code !in 200..299) { conn.disconnect(); return@thread }
                val txt = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val arr = JSONObject(txt).optJSONArray("data") ?: return@thread
                val prev = p.getString("last_tx_created", null)
                var maxSeen = prev
                val newOnes = ArrayList<JSONObject>()
                for (i in 0 until arr.length()) {
                    val t = arr.optJSONObject(i) ?: continue
                    val created = t.optString("createdAt", "")
                    if (created.isBlank()) continue
                    if (prev != null && created > prev) newOnes.add(t)
                    if (maxSeen == null || created > maxSeen) maxSeen = created
                }
                if (maxSeen != null) p.edit().putString("last_tx_created", maxSeen).apply()

                // prev == null -> baseline pertama, jangan spam notifikasi
                if (prev != null) {
                    for (t in newOnes) {
                        val amt = t.optLong("amount", 0)
                        val id = t.optString("transactionId", "")
                        Notifier.notify(
                            this, id.hashCode(),
                            "QRIS baru dibuat",
                            "Invoice Rp " + java.text.NumberFormat.getInstance(java.util.Locale("in", "ID")).format(amt)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("QRISPayNotif", "poll fail", e)
            }
        }
    }

    private fun forward(notificationText: String, pkg: String) {
        val p = prefs()
        val server = p.getString("server", "")?.trimEnd('/') ?: ""
        val webhookSecret = p.getString("webhook_secret", "") ?: ""
        if (server.isBlank() || webhookSecret.isBlank()) return

        thread {
            try {
                val url = URL("$server/api/webhook/mutasi")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-webhook-secret", webhookSecret)

                val body = JSONObject()
                    .put("notification_text", notificationText)
                    .put("package", pkg)
                conn.outputStream.use { it.write(body.toString().toByteArray()) }

                val code = conn.responseCode
                Log.i("QRISPayNotif", "sent ($code): $notificationText")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("QRISPayNotif", "forward fail", e)
            }
        }
    }
}
