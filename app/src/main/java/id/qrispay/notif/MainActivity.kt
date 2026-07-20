package id.qrispay.notif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val p = getSharedPreferences("cfg", Context.MODE_PRIVATE)
        val server = findViewById<EditText>(R.id.server)
        val license = findViewById<EditText>(R.id.license)
        val watchAll = findViewById<CheckBox>(R.id.watchAll)
        val save = findViewById<Button>(R.id.save)
        val grant = findViewById<Button>(R.id.grant)

        server.setText(p.getString("server", "https://qrispay.deviynsp.biz.id"))
        license.setText(p.getString("license", ""))
        watchAll.isChecked = p.getBoolean("watch_all", false)

        save.setOnClickListener {
            p.edit()
                .putString("server", server.text.toString().trim())
                .putString("license", license.text.toString().trim())
                .putBoolean("watch_all", watchAll.isChecked)
                .apply()
            Toast.makeText(this, "Tersimpan", Toast.LENGTH_SHORT).show()
        }

        grant.setOnClickListener {
            // buka setelan akses notifikasi supaya user aktifkan service ini
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }
}
