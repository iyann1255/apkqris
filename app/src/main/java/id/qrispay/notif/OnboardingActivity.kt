package id.qrispay.notif

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private val slides = listOf(
        Slide("💳", "Selamat datang di QRISPay",
            "Terima pembayaran QRIS dan verifikasi transaksi masuk secara otomatis langsung dari HP kamu."),
        Slide("🔔", "Pantau Notifikasi Pembayaran",
            "Aplikasi membaca notifikasi uang masuk dari e-wallet & m-banking (DANA, GoPay, OVO, BCA, BRImo, dll)."),
        Slide("🔑", "Login dengan License Key",
            "Cukup masukkan Server URL & License Key dari dashboard web kamu untuk menghubungkan aplikasi."),
        Slide("✅", "Otomatis & Real-time",
            "Setiap pembayaran masuk langsung dicocokkan dengan invoice. Tidak perlu cek manual lagi.")
    )

    private lateinit var pager: ViewPager2
    private lateinit var dots: LinearLayout
    private lateinit var next: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Routing: sudah login -> Main, sudah lihat intro -> Login
        if (ApiClient.isLoggedIn(this)) {
            goTo(MainActivity::class.java); return
        }
        if (ApiClient.introSeen(this)) {
            goTo(LoginActivity::class.java); return
        }

        setContentView(R.layout.activity_onboarding)
        pager = findViewById(R.id.pager)
        dots = findViewById(R.id.dots)
        next = findViewById(R.id.next)

        pager.adapter = OnboardingAdapter(slides)
        buildDots()
        setDot(0)

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setDot(position)
                next.text = if (position == slides.lastIndex) "Mulai" else "Lanjut"
            }
        })

        findViewById<TextView>(R.id.skip).setOnClickListener { finishIntro() }
        next.setOnClickListener {
            if (pager.currentItem == slides.lastIndex) finishIntro()
            else pager.currentItem = pager.currentItem + 1
        }
    }

    private fun buildDots() {
        dots.removeAllViews()
        val size = (8 * resources.displayMetrics.density).toInt()
        for (i in slides.indices) {
            val dot = View(this)
            val lp = LinearLayout.LayoutParams(size, size)
            lp.marginStart = size / 2
            lp.marginEnd = size / 2
            lp.gravity = Gravity.CENTER
            dot.layoutParams = lp
            dots.addView(dot)
        }
    }

    private fun setDot(active: Int) {
        for (i in 0 until dots.childCount) {
            dots.getChildAt(i).setBackgroundResource(
                if (i == active) R.drawable.dot_active else R.drawable.dot_inactive
            )
        }
    }

    private fun finishIntro() {
        ApiClient.setIntroSeen(this)
        goTo(LoginActivity::class.java)
    }

    private fun goTo(cls: Class<*>) {
        startActivity(Intent(this, cls))
        finish()
    }
}
