package id.qrispay.notif

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class InvoiceFragment : Fragment() {

    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var list: RecyclerView
    private lateinit var status: TextView
    private val adapter = TxAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_invoice, container, false)
        refresh = v.findViewById(R.id.refresh)
        list = v.findViewById(R.id.list)
        status = v.findViewById(R.id.status)

        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        adapter.onAccept = { txId -> confirmAccept(txId) }

        refresh.setOnRefreshListener { load() }
        return v
    }

    private fun confirmAccept(txId: String) {
        val ctx = context ?: return
        if (txId.isBlank()) return
        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle("Acc Manual")
            .setMessage("Tandai transaksi ini LUNAS secara manual?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Ya, Lunas") { _, _ -> doAccept(txId) }
            .show()
    }

    private fun doAccept(txId: String) {
        val ctx = context ?: return
        val payload = org.json.JSONObject().put("transactionId", txId)
        ApiClient.postWebhook(ctx, "/api/mark-paid", payload) { ok, body, err ->
            if (ok && body != null && body.optBoolean("success", false)) {
                Toast.makeText(ctx, "Transaksi ditandai LUNAS", Toast.LENGTH_SHORT).show()
                load()
            } else {
                Toast.makeText(ctx, "Gagal acc: ${err ?: "-"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun showStatus(msg: String) {
        status.text = msg
        status.visibility = if (msg.isBlank()) View.GONE else View.VISIBLE
    }

    private fun load() {
        val ctx = context ?: return
        if (!ApiClient.isConfigured(ctx)) {
            showStatus("Belum dikonfigurasi. Isi Server URL & License Key di tab Setting.")
            refresh.isRefreshing = false
            return
        }
        refresh.isRefreshing = true
        ApiClient.getJson(ctx, "/api/transactions?limit=100") { ok, body, err ->
            refresh.isRefreshing = false
            if (!ok || body == null) {
                showStatus("Gagal memuat: ${err ?: "tidak diketahui"}")
                return@getJson
            }
            val arr = body.optJSONArray("data")
            if (arr == null || arr.length() == 0) {
                adapter.submit(org.json.JSONArray())
                showStatus("Belum ada transaksi.")
                return@getJson
            }
            showStatus("")
            adapter.submit(arr)
        }
    }
}
