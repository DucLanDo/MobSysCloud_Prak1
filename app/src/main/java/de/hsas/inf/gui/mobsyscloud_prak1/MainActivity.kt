package de.hsas.inf.gui.mobsyscloud_prak1

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

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
}