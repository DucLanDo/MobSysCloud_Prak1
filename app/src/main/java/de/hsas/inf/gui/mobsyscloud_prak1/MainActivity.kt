package de.hsas.inf.gui.mobsyscloud_prak1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun openAccelerometer(view: View) {
        startActivity(Intent(this, AccelerometerActivity::class.java))
    }

    fun openTemperature(view: View) {
       startActivity(Intent(this, TemperatureActivity::class.java))
    }
    fun openLocation(view: View) {
        startActivity(Intent(this, LocationActivity::class.java))
    }

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // Nicht eingeloggt → zur LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    fun logout(view: View) {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}