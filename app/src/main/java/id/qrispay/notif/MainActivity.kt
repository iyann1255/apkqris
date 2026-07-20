package id.qrispay.notif

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val dashboard = DashboardFragment()
    private val monitor = MonitorFragment()
    private val invoice = InvoiceFragment()
    private val setting = SettingFragment()
    private val profile = ProfileFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        nav.setOnItemSelectedListener { item ->
            val f: Fragment = when (item.itemId) {
                R.id.nav_dashboard -> dashboard
                R.id.nav_monitor -> monitor
                R.id.nav_invoice -> invoice
                R.id.nav_setting -> setting
                R.id.nav_profile -> profile
                else -> dashboard
            }
            show(f)
            true
        }

        if (savedInstanceState == null) {
            nav.selectedItemId = R.id.nav_dashboard
        }
    }

    private fun show(f: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, f)
            .commit()
    }
}
