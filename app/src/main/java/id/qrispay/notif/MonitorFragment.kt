package id.qrispay.notif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class MonitorFragment : Fragment() {

    private lateinit var statusDot: View
    private lateinit var statusTitle: TextView
    private lateinit var statusSub: TextView
    private lateinit var watchMode: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_monitor, container, false)
        statusDot = v.findViewById(R.id.statusDot)
        statusTitle = v.findViewById(R.id.statusTitle)
        statusSub = v.findViewById(R.id.statusSub)
        watchMode = v.findViewById(R.id.watchMode)

        v.findViewById<Button>(R.id.grant).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        return v
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val ctx = context ?: return
        val enabled = isNotificationAccessGranted(ctx)
        if (enabled) {
            statusDot.setBackgroundResource(R.drawable.dot_ok)
            statusTitle.text = "Akses Notifikasi Aktif"
            statusSub.text = "Pembayaran akan terverifikasi otomatis."
        } else {
            statusDot.setBackgroundResource(R.drawable.dot_off)
            statusTitle.text = "Akses Notifikasi Nonaktif"
            statusSub.text = "Aktifkan agar pembayaran terverifikasi otomatis."
        }

        val watchAll = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)
            .getBoolean("watch_all", false)
        watchMode.text = if (watchAll)
            "Semua aplikasi (mode luas)"
        else
            "Hanya e-wallet / m-banking umum"
    }

    private fun isNotificationAccessGranted(ctx: Context): Boolean {
        val flat = Settings.Secure.getString(
            ctx.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        val pkg = ctx.packageName
        return flat.split(":").any { it.contains(pkg) }
    }
}
