package id.qrispay.notif

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.text.NumberFormat
import java.util.Locale

class DashboardFragment : Fragment() {

    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var greeting: TextView
    private lateinit var paidAmount: TextView
    private lateinit var paidCountLine: TextView
    private lateinit var statQris: TextView
    private lateinit var statTotal: TextView
    private lateinit var statPaid: TextView
    private lateinit var statPending: TextView
    private lateinit var status: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_dashboard, container, false)
        refresh = v.findViewById(R.id.refresh)
        greeting = v.findViewById(R.id.greeting)
        paidAmount = v.findViewById(R.id.paidAmount)
        paidCountLine = v.findViewById(R.id.paidCountLine)
        statQris = v.findViewById(R.id.statQris)
        statTotal = v.findViewById(R.id.statTotal)
        statPaid = v.findViewById(R.id.statPaid)
        statPending = v.findViewById(R.id.statPending)
        status = v.findViewById(R.id.status)

        refresh.setOnRefreshListener { load() }
        return v
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val ctx = context ?: return
        if (!ApiClient.isConfigured(ctx)) {
            status.text = "Belum dikonfigurasi. Isi Server URL & License Key di tab Setting."
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
            greeting.text = "Halo, ${data.optString("username", "-")} 👋"
            val stats = data.optJSONObject("stats")
            if (stats != null) {
                paidAmount.text = rupiah(stats.optLong("paidAmount", 0))
                val paid = stats.optInt("txPaid", 0)
                paidCountLine.text = "$paid transaksi lunas"
                statQris.text = stats.optInt("qris", 0).toString()
                statTotal.text = stats.optInt("txTotal", 0).toString()
                statPaid.text = paid.toString()
                statPending.text = stats.optInt("txPending", 0).toString()
            }
            status.text = "Tarik ke bawah untuk menyegarkan."
        }
    }

    private fun rupiah(v: Long): String {
        val nf = NumberFormat.getInstance(Locale("in", "ID"))
        return "Rp " + nf.format(v)
    }
}
