package id.qrispay.notif

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/** Helper HTTP sederhana untuk ambil data dari server (auth: x-license-key). */
object ApiClient {

    private val main = Handler(Looper.getMainLooper())

    fun prefs(ctx: Context) = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    fun serverUrl(ctx: Context): String =
        (prefs(ctx).getString("server", "") ?: "").trimEnd('/')

    fun licenseKey(ctx: Context): String =
        prefs(ctx).getString("license", "") ?: ""

    fun webhookSecret(ctx: Context): String =
        prefs(ctx).getString("webhook_secret", "") ?: ""

    fun isConfigured(ctx: Context): Boolean =
        serverUrl(ctx).isNotBlank() && licenseKey(ctx).isNotBlank()

    // ---- Session / onboarding state ----
    fun isLoggedIn(ctx: Context): Boolean =
        prefs(ctx).getBoolean("logged_in", false) && isConfigured(ctx)

    fun introSeen(ctx: Context): Boolean =
        prefs(ctx).getBoolean("intro_seen", false)

    fun setIntroSeen(ctx: Context) {
        prefs(ctx).edit().putBoolean("intro_seen", true).apply()
    }

    /** Simpan kredensial & tandai sudah login. */
    fun saveSession(ctx: Context, server: String, license: String, webhookSecret: String = "") {
        prefs(ctx).edit()
            .putString("server", server.trim())
            .putString("license", license.trim())
            .putString("webhook_secret", webhookSecret.trim())
            .putBoolean("logged_in", true)
            .apply()
    }

    /** Hapus status login (kredensial dibiarkan agar mudah login ulang). */
    fun logout(ctx: Context) {
        prefs(ctx).edit().putBoolean("logged_in", false).apply()
    }

    /**
     * GET path (mis. "/api/me"). Callback dipanggil di main thread:
     *   onResult(success: Boolean, body: JSONObject?, error: String?)
     */
    fun getJson(ctx: Context, path: String, onResult: (Boolean, JSONObject?, String?) -> Unit) {
        val server = serverUrl(ctx)
        val license = licenseKey(ctx)
        if (server.isBlank() || license.isBlank()) {
            onResult(false, null, "Server URL / License Key belum diisi (tab Setting).")
            return
        }
        thread {
            var ok = false
            var body: JSONObject? = null
            var err: String? = null
            try {
                val conn = URL("$server$path").openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 12000
                conn.readTimeout = 12000
                conn.setRequestProperty("x-license-key", license)
                conn.setRequestProperty("Accept", "application/json")
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
                conn.disconnect()
                if (code in 200..299) {
                    body = JSONObject(text)
                    ok = true
                } else {
                    err = "HTTP $code"
                }
            } catch (e: Exception) {
                err = e.message ?: "Gagal koneksi"
            }
            main.post { onResult(ok, body, err) }
        }
    }

    /** POST JSON ke `path` dengan auth license key (x-license-key). */
    fun postLicense(ctx: Context, path: String, payload: JSONObject,
                    onResult: (Boolean, JSONObject?, String?) -> Unit) {
        val server = serverUrl(ctx)
        val license = licenseKey(ctx)
        if (server.isBlank() || license.isBlank()) {
            onResult(false, null, "Server URL / License Key belum diisi (tab Setting).")
            return
        }
        thread {
            var ok = false; var body: JSONObject? = null; var err: String? = null
            try {
                val conn = URL("$server$path").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 12000
                conn.readTimeout = 12000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-license-key", license)
                conn.setRequestProperty("Accept", "application/json")
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
                conn.disconnect()
                if (code in 200..299) { body = JSONObject(text); ok = true }
                else {
                    err = try { JSONObject(text).optString("message", "HTTP $code") }
                          catch (e: Exception) { "HTTP $code" }
                }
            } catch (e: Exception) { err = e.message ?: "Gagal koneksi" }
            main.post { onResult(ok, body, err) }
        }
    }

    /**
     * POST JSON ke `path` dengan auth webhook secret (x-webhook-secret).
     * Dipakai untuk aksi yang mengubah status pembayaran (mis. manual acc /api/mark-paid).
     */
    fun postWebhook(ctx: Context, path: String, payload: JSONObject,
                    onResult: (Boolean, JSONObject?, String?) -> Unit) {
        val server = serverUrl(ctx)
        val secret = webhookSecret(ctx)
        if (server.isBlank()) {
            onResult(false, null, "Server URL belum diisi (tab Setting).")
            return
        }
        if (secret.isBlank()) {
            onResult(false, null, "Webhook Secret belum diisi (tab Setting). Diperlukan untuk Acc manual.")
            return
        }
        thread {
            var ok = false
            var body: JSONObject? = null
            var err: String? = null
            try {
                val conn = URL("$server$path").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 12000
                conn.readTimeout = 12000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-webhook-secret", secret)
                conn.setRequestProperty("Accept", "application/json")
                conn.outputStream.use { it.write(payload.toString().toByteArray()) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
                conn.disconnect()
                if (code in 200..299) {
                    body = JSONObject(text); ok = true
                } else {
                    err = "HTTP $code"
                }
            } catch (e: Exception) {
                err = e.message ?: "Gagal koneksi"
            }
            main.post { onResult(ok, body, err) }
        }
    }
}
