package id.qrispay.notif

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class SettingFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_setting, container, false)
        val p = requireContext().getSharedPreferences("cfg", Context.MODE_PRIVATE)

        val server = v.findViewById<EditText>(R.id.server)
        val license = v.findViewById<EditText>(R.id.license)
        val watchAll = v.findViewById<CheckBox>(R.id.watchAll)

        server.setText(p.getString("server", "https://qrispay.deviynsp.biz.id"))
        license.setText(p.getString("license", ""))
        watchAll.isChecked = p.getBoolean("watch_all", false)

        v.findViewById<Button>(R.id.save).setOnClickListener {
            p.edit()
                .putString("server", server.text.toString().trim())
                .putString("license", license.text.toString().trim())
                .putBoolean("watch_all", watchAll.isChecked)
                .apply()
            Toast.makeText(requireContext(), "Tersimpan", Toast.LENGTH_SHORT).show()
        }
        v.findViewById<Button>(R.id.grant).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        return v
    }
}
