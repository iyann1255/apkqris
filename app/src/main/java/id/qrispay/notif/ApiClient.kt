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
    fun saveSession(ctx: Context, server: String, license: String) {
        prefs(ctx).edit()
            .putString("server", server.trim())
            .putString("license", license.trim())
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
}
