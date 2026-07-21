package id.qrispay.notif

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.util.Locale
import kotlin.concurrent.thread

class CreateFragment : Fragment() {

    private lateinit var spinner: Spinner
    private lateinit var amount: EditText
    private lateinit var generate: Button
    private lateinit var formStatus: TextView
    private lateinit var formBox: View
    private lateinit var resultBox: View
    private lateinit var resultAmount: TextView
    private lateinit var qrImage: ImageView
    private lateinit var resultStatus: TextView
    private lateinit var resultTxId: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var pollTxId: String? = null
    private val qrisIds = ArrayList<String>()

    private val poll = object : Runnable {
        override fun run() {
            val tx = pollTxId ?: return
            val ctx = context ?: return
            ApiClient.getJson(ctx, "/api/status/$tx") { ok, body, _ ->
                if (ok && body != null && body.optBoolean("paid", false)) {
                    resultStatus.text = "✅ LUNAS — pembayaran diterima"
                    resultStatus.setTextColor(ctx.getColor(R.color.ok))
                } else if (ok && body != null && body.optString("status") == "expired") {
                    resultStatus.text = "⌛ Kedaluwarsa"
                    resultStatus.setTextColor(ctx.getColor(R.color.danger))
                } else {
                    handler.postDelayed(this, 4000)
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_create, container, false)
        spinner = v.findViewById(R.id.spinnerQris)
        amount = v.findViewById(R.id.amount)
        generate = v.findViewById(R.id.generate)
        formStatus = v.findViewById(R.id.formStatus)
        formBox = v.findViewById(R.id.formBox)
        resultBox = v.findViewById(R.id.resultBox)
        resultAmount = v.findViewById(R.id.resultAmount)
        qrImage = v.findViewById(R.id.qrImage)
        resultStatus = v.findViewById(R.id.resultStatus)
        resultTxId = v.findViewById(R.id.resultTxId)

        generate.setOnClickListener { doGenerate() }
        v.findViewById<Button>(R.id.again).setOnClickListener { resetForm() }
        return v
    }

    override fun onResume() {
        super.onResume()
        loadConfigs()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(poll)
    }

    private fun showFormError(msg: String) {
        formStatus.text = msg
        formStatus.setTextColor(requireContext().getColor(R.color.danger))
        formStatus.visibility = View.VISIBLE
    }

    private fun loadConfigs() {
        val ctx = context ?: return
        if (!ApiClient.isConfigured(ctx)) {
            showFormError("Belum login / dikonfigurasi (tab Setting).")
            return
        }
        ApiClient.getJson(ctx, "/api/qris") { ok, body, err ->
            if (!ok || body == null) { showFormError("Gagal memuat QRIS: ${err ?: "-"}"); return@getJson }
            val arr = body.optJSONArray("data")
            qrisIds.clear()
            val names = ArrayList<String>()
            if (arr != null) for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                qrisIds.add(o.optString("qrisId"))
                names.add(o.optString("name"))
            }
            if (qrisIds.isEmpty()) {
                showFormError("Belum ada QRIS. Tambahkan dulu lewat web dashboard.")
                generate.isEnabled = false
                return@getJson
            }
            formStatus.visibility = View.GONE
            generate.isEnabled = true
            val ad = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, names)
            spinner.adapter = ad
        }
    }

    private fun doGenerate() {
        val ctx = context ?: return
        val pos = spinner.selectedItemPosition
        if (pos < 0 || pos >= qrisIds.size) { showFormError("Pilih QRIS dulu."); return }
        val amt = amount.text.toString().trim().toLongOrNull() ?: 0L
        if (amt <= 0) { showFormError("Nominal tidak valid."); return }

        formStatus.visibility = View.GONE
        generate.isEnabled = false
        generate.text = "Membuat…"
        val payload = JSONObject().put("id", qrisIds[pos]).put("amount", amt)
        ApiClient.postLicense(ctx, "/api/generate/qris", payload) { ok, body, err ->
            generate.isEnabled = true
            generate.text = "Buat QRIS"
            if (!ok || body == null || !body.optBoolean("success", false)) {
                showFormError("Gagal: ${err ?: "tidak diketahui"}")
                return@postLicense
            }
            val data = body.optJSONObject("data") ?: return@postLicense
            showResult(data.optString("transactionId"), data.optLong("totalAmount", amt))
        }
    }

    private fun showResult(txId: String, total: Long) {
        val ctx = context ?: return
        formBox.visibility = View.GONE
        resultBox.visibility = View.VISIBLE
        resultAmount.text = "Rp " + NumberFormat.getInstance(Locale("in", "ID")).format(total)
        resultTxId.text = txId
        resultStatus.text = "⏳ Menunggu pembayaran…"
        resultStatus.setTextColor(ctx.getColor(R.color.warn))
        loadQr(txId)
        pollTxId = txId
        handler.removeCallbacks(poll)
        handler.postDelayed(poll, 4000)
    }

    private fun loadQr(txId: String) {
        val ctx = context ?: return
        val url = ApiClient.serverUrl(ctx) + "/api/qr/" + txId + ".png"
        thread {
            try {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 12000
                conn.readTimeout = 12000
                val bmp = BitmapFactory.decodeStream(conn.inputStream)
                conn.disconnect()
                handler.post { if (bmp != null) qrImage.setImageBitmap(bmp) }
            } catch (_: Exception) {}
        }
    }

    private fun resetForm() {
        handler.removeCallbacks(poll)
        pollTxId = null
        resultBox.visibility = View.GONE
        formBox.visibility = View.VISIBLE
        amount.setText("")
    }
}
