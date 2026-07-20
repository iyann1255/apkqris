package id.qrispay.notif

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class Slide(val icon: String, val title: String, val desc: String)

class OnboardingAdapter(private val slides: List<Slide>) :
    RecyclerView.Adapter<OnboardingAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: TextView = v.findViewById(R.id.slideIcon)
        val title: TextView = v.findViewById(R.id.slideTitle)
        val desc: TextView = v.findViewById(R.id.slideDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = slides[position]
        holder.icon.text = s.icon
        holder.title.text = s.title
        holder.desc.text = s.desc
    }

    override fun getItemCount(): Int = slides.size
}
