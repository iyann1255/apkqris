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

class LogAdapter(private val items: MutableList<JSONObject> = mutableListOf()) :
    RecyclerView.Adapter<LogAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val dot: View = v.findViewById(R.id.logDot)
        val amount: TextView = v.findViewById(R.id.logAmount)
        val note: TextView = v.findViewById(R.id.logNote)
        val meta: TextView = v.findViewById(R.id.logMeta)
    }

    fun submit(arr: JSONArray) {
        items.clear()
        for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { items.add(it) }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val l = items[position]
        val amt = l.optLong("amount", 0)
        holder.amount.text = if (amt > 0) rupiah(amt) else "—"
        holder.note.text = l.optString("note", "")
        val src = l.optString("source", "")
        val at = l.optString("createdAt", "")
        holder.meta.text = listOf(src, at).filter { it.isNotBlank() && it != "null" }.joinToString(" • ")

        val note = l.optString("note", "").lowercase()
        val matched = !l.isNull("matchedTx") && l.optString("matchedTx").isNotBlank()
        val dotRes = when {
            note.contains("dibuat") -> R.drawable.dot_info   // pembuatan QRIS
            matched -> R.drawable.dot_ok                       // pembayaran cocok
            else -> R.drawable.dot_off                         // masuk, tidak cocok
        }
        holder.dot.setBackgroundResource(dotRes)
    }

    override fun getItemCount(): Int = items.size

    private fun rupiah(v: Long): String =
        "Rp " + NumberFormat.getInstance(Locale("in", "ID")).format(v)
}
