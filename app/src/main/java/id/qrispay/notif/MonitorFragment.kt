package id.qrispay.notif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MonitorFragment : Fragment() {

    private lateinit var statusDot: View
    private lateinit var statusTitle: TextView
    private lateinit var statusSub: TextView
    private lateinit var pingResult: TextView
    private lateinit var logEmpty: TextView
    private lateinit var logList: RecyclerView
    private val adapter = LogAdapter()

    private val handler = Handler(Looper.getMainLooper())
    private val refreshEveryMs = 60_000L
    private val autoRefresh = object : Runnable {
        override fun run() {
            loadLogs()
            handler.postDelayed(this, refreshEveryMs)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_monitor, container, false)
        statusDot = v.findViewById(R.id.statusDot)
        statusTitle = v.findViewById(R.id.statusTitle)
        statusSub = v.findViewById(R.id.statusSub)
        pingResult = v.findViewById(R.id.pingResult)
        logEmpty = v.findViewById(R.id.logEmpty)
        logList = v.findViewById(R.id.logList)

        logList.layoutManager = LinearLayoutManager(requireContext())
        logList.adapter = adapter

        v.findViewById<Button>(R.id.grant).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        v.findViewById<Button>(R.id.ping).setOnClickListener { doPing(); loadLogs() }
        return v
    }

    override fun onResume() {
        super.onResume()
        renderStatus()
        // auto-refresh tiap 1 menit
        handler.removeCallbacks(autoRefresh)
        handler.post(autoRefresh)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(autoRefresh)
    }

    private fun renderStatus() {
        val ctx = context ?: return
        val enabled = isNotificationAccessGranted(ctx)
        if (enabled) {
            statusDot.setBackgroundResource(R.drawable.dot_ok)
            statusTitle.text = "Akses Notifikasi Aktif"
            statusSub.text = "Notifikasi pembayaran diteruskan ke server."
        } else {
            statusDot.setBackgroundResource(R.drawable.dot_off)
            statusTitle.text = "Akses Notifikasi Nonaktif"
            statusSub.text = "Aktifkan agar pembayaran terverifikasi otomatis."
        }
    }

    private fun doPing() {
        val ctx = context ?: return
        if (!ApiClient.isConfigured(ctx)) {
            pingResult.text = "Belum login / dikonfigurasi."
            return
        }
        pingResult.setTextColor(ctx.getColor(R.color.muted))
        pingResult.text = "Ping & tes notifikasi…"

        // 1) Tes pembacaan notifikasi: kirim notif bertanda, listener sendiri
        //    harus menangkapnya. Catat waktu sebelumnya untuk pembanding.
        val prefs = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)
        val before = prefs.getLong("test_seen_at", 0L)
        Notifier.notify(ctx, 999001, "QRISPay Ping",
            "Tes pembacaan notifikasi ${PaymentNotificationListener.TEST_MARKER}")

        // 2) Ping server
        val t0 = System.currentTimeMillis()
        ApiClient.getJson(ctx, "/api/ping") { ok, body, _ ->
            val serverPart = if (ok && body != null && body.optBoolean("success", false)) {
                "✓ Server OK (${System.currentTimeMillis() - t0}ms)"
            } else "✗ Server tidak terhubung"

            // beri jeda agar listener sempat menerima notif tes
            handler.postDelayed({
                val c = context ?: return@postDelayed
                val after = prefs.getLong("test_seen_at", 0L)
                val notifOn = after > before
                val notifPart = if (notifOn) "✓ Notif AKTIF (terbaca)" else "✗ Notif TIDAK terbaca"
                c.let {
                    pingResult.setTextColor(
                        it.getColor(if (ok && notifOn) R.color.ok else R.color.warn)
                    )
                    pingResult.text = "$serverPart  •  $notifPart"
                }
                renderStatus()
            }, 1800)
        }
    }

    private fun loadLogs() {
        val ctx = context ?: return
        if (!ApiClient.isConfigured(ctx)) {
            logEmpty.text = "Belum login / dikonfigurasi."
            logEmpty.visibility = View.VISIBLE
            return
        }
        ApiClient.getJson(ctx, "/api/logs?limit=100") { ok, body, err ->
            if (!ok || body == null) {
                logEmpty.text = "Gagal memuat log: ${err ?: "-"}"
                logEmpty.visibility = View.VISIBLE
                return@getJson
            }
            val arr = body.optJSONArray("data") ?: org.json.JSONArray()
            adapter.submit(arr)
            logEmpty.visibility = if (arr.length() == 0) View.VISIBLE else View.GONE
            if (arr.length() == 0) logEmpty.text = "Belum ada notifikasi masuk."
        }
    }

    private fun isNotificationAccessGranted(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(
            ctx.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        return flat.split(":").any { it.contains(ctx.packageName) }
    }
}
