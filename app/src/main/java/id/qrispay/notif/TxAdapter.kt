package id.qrispay.notif

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class TxAdapter(private val items: MutableList<JSONObject> = mutableListOf()) :
    RecyclerView.Adapter<TxAdapter.VH>() {

    /** Dipanggil saat tombol Acc ditekan untuk transaksi pending. */
    var onAccept: ((String) -> Unit)? = null

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val amount: TextView = v.findViewById(R.id.txAmount)
        val id: TextView = v.findViewById(R.id.txId)
        val date: TextView = v.findViewById(R.id.txDate)
        val statusChip: TextView = v.findViewById(R.id.txStatus)
        val accept: android.widget.Button = v.findViewById(R.id.txAccept)
    }

    fun submit(arr: JSONArray) {
        items.clear()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { items.add(it) }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = items[position]
        holder.amount.text = rupiah(t.optLong("amount", 0))
        holder.id.text = t.optString("transactionId", "—")
        holder.date.text = t.optString("createdAt", "—")

        val status = t.optString("status", "pending").lowercase()
        holder.statusChip.text = status
        when (status) {
            "paid" -> {
                holder.statusChip.setBackgroundResource(R.drawable.chip_paid)
                holder.statusChip.setTextColor(0xFF34D399.toInt())
            }
            "expired", "cancelled", "canceled", "failed" -> {
                holder.statusChip.setBackgroundResource(R.drawable.chip_expired)
                holder.statusChip.setTextColor(0xFFF43F5E.toInt())
            }
            else -> {
                holder.statusChip.setBackgroundResource(R.drawable.chip_pending)
                holder.statusChip.setTextColor(0xFFFBBF24.toInt())
            }
        }

        // Tombol Acc manual hanya untuk transaksi pending
        if (status == "pending") {
            holder.accept.visibility = View.VISIBLE
            holder.accept.setOnClickListener {
                onAccept?.invoke(t.optString("transactionId", ""))
            }
        } else {
            holder.accept.visibility = View.GONE
            holder.accept.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun rupiah(v: Long): String {
        val nf = NumberFormat.getInstance(Locale("in", "ID"))
        return "Rp " + nf.format(v)
    }
}
