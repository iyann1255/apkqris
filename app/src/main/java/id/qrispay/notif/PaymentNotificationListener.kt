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
            val watchAll = prefs().getBoolean("watch_all", false)
            if (!watchAll && WATCHED.none { pkg.contains(it) }) return

            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            val full = listOf(title, text, big).filter { it.isNotBlank() }.joinToString(" | ")
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

    private fun forward(notificationText: String, pkg: String) {
        val p = prefs()
        val server = p.getString("server", "")?.trimEnd('/') ?: ""
        val license = p.getString("license", "") ?: ""
        if (server.isBlank() || license.isBlank()) return

        thread {
            try {
                val url = URL("$server/api/webhook/mutasi")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-license-key", license)

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
