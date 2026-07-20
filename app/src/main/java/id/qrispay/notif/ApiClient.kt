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
