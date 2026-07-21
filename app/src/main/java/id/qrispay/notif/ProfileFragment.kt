package id.qrispay.notif

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class ProfileFragment : Fragment() {

    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var avatar: TextView
    private lateinit var username: TextView
    private lateinit var roleBadge: TextView
    private lateinit var licenseKey: TextView
    private lateinit var serverUrl: TextView
    private lateinit var status: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_profile, container, false)
        refresh = v.findViewById(R.id.refresh)
        avatar = v.findViewById(R.id.avatar)
        username = v.findViewById(R.id.username)
        roleBadge = v.findViewById(R.id.roleBadge)
        licenseKey = v.findViewById(R.id.licenseKey)
        serverUrl = v.findViewById(R.id.serverUrl)
        status = v.findViewById(R.id.status)

        v.findViewById<Button>(R.id.copyKey).setOnClickListener { copyKey() }
        v.findViewById<Button>(R.id.logout).setOnClickListener { doLogout() }
        v.findViewById<Button>(R.id.openSetting).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, SettingFragment())
                .addToBackStack(null)
                .commit()
        }
        refresh.setOnRefreshListener { load() }
        return v
    }

    private fun doLogout() {
        val ctx = context ?: return
        ApiClient.logout(ctx)
        val i = android.content.Intent(ctx, LoginActivity::class.java)
        i.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
        activity?.finish()
    }

    override fun onResume() {
        super.onResume()
        val ctx = context
        if (ctx != null) serverUrl.text = ApiClient.serverUrl(ctx).ifBlank { "—" }
        load()
    }

    private fun copyKey() {
        val ctx = context ?: return
        val key = licenseKey.text?.toString()?.trim() ?: ""
        if (key.isBlank() || key == "—") {
            Toast.makeText(ctx, "License key belum tersedia", Toast.LENGTH_SHORT).show()
            return
        }
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("license_key", key))
        Toast.makeText(ctx, "License key disalin", Toast.LENGTH_SHORT).show()
    }

    private fun load() {
        val ctx = context ?: return
        if (!ApiClient.isConfigured(ctx)) {
            status.text = "Belum dikonfigurasi. Isi Server URL & License Key di tab Setting."
            licenseKey.text = ApiClient.licenseKey(ctx).ifBlank { "—" }
            refresh.isRefreshing = false
            return
        }
        refresh.isRefreshing = true
        status.text = "Memuat data…"
        ApiClient.getJson(ctx, "/api/me") { ok, body, err ->
            refresh.isRefreshing = false
            if (!ok || body == null) {
                status.text = "Gagal memuat: ${err ?: "tidak diketahui"}"
                return@getJson
            }
            val data = body.optJSONObject("data")
            if (data == null) {
                status.text = "Respon tidak valid."
                return@getJson
            }
            val name = data.optString("username", "-")
            username.text = name
            avatar.text = name.take(1).uppercase()
            licenseKey.text = data.optString("licenseKey", "—")
            roleBadge.text = if (data.optBoolean("isAdmin", false)) "Administrator" else "Member"
            status.text = ""
        }
    }
}
