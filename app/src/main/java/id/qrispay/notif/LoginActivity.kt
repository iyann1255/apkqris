package id.qrispay.notif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var server: EditText
    private lateinit var license: EditText
    private lateinit var webhookSecret: EditText
    private lateinit var login: Button
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ApiClient.isLoggedIn(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish(); return
        }

        setContentView(R.layout.activity_login)
        server = findViewById(R.id.server)
        license = findViewById(R.id.license)
        webhookSecret = findViewById(R.id.webhookSecret)
        login = findViewById(R.id.login)
        status = findViewById(R.id.status)

        // Prefill kalau pernah diisi
        server.setText(ApiClient.serverUrl(this).ifBlank { "https://qrispay.deviynsp.biz.id" })
        license.setText(ApiClient.licenseKey(this))
        webhookSecret.setText(ApiClient.webhookSecret(this))

        login.setOnClickListener { doLogin() }
    }

    private fun showError(msg: String) {
        status.text = msg
        status.setTextColor(getColor(R.color.danger))
        status.visibility = View.VISIBLE
    }

    private fun doLogin() {
        val srv = server.text.toString().trim().trimEnd('/')
        val lic = license.text.toString().trim()
        val whs = webhookSecret.text.toString().trim()
        if (srv.isBlank() || lic.isBlank()) {
            showError("Server URL dan License Key wajib diisi.")
            return
        }

        // Simpan sementara agar ApiClient bisa memakai header, lalu validasi.
        getSharedPreferences("cfg", Context.MODE_PRIVATE).edit()
            .putString("server", srv)
            .putString("license", lic)
            .putString("webhook_secret", whs)
            .apply()

        login.isEnabled = false
        status.setTextColor(getColor(R.color.muted))
        status.text = "Memverifikasi…"
        status.visibility = View.VISIBLE

        ApiClient.getJson(this, "/api/me") { ok, body, err ->
            if (ok && body != null && body.optBoolean("success", false)) {
                ApiClient.saveSession(this, srv, lic, whs)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                login.isEnabled = true
                showError("Login gagal: ${err ?: "License Key tidak valid"}")
            }
        }
    }
}
