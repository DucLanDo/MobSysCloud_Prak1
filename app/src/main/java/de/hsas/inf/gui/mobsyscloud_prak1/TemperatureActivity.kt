package de.hsas.inf.gui.mobsyscloud_prak1

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartView
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import java.util.Locale
import kotlin.math.abs
import android.util.Log

class TemperatureActivity : AppCompatActivity(), SensorEventListener {

    // UI
    private lateinit var tvTemp: TextView
    private lateinit var panelText: LinearLayout
    private lateinit var switchBtn: Button
    private lateinit var aaChartView: AAChartView

    // Diagramm
    private lateinit var aaModel: AAChartModel
    private lateinit var tempSeries: AASeriesElement

    // Switch-Modus Text und Diagramm
    private enum class Mode { TEXT, CHART }
    private var currentMode = Mode.TEXT

    // Sensor
    private lateinit var sensorManager: SensorManager
    private var tempSensor: Sensor? = null

    // Notification
    private val CHANNEL_ID = "temp_alerts"
    private val NOTIFY_ID = 2025
    private val THRESHOLD_C = 50f
    private var lastNotifyTime = 0L
    private val MIN_INTERVAL_MS = 60_000L // 1 Minute

    private val TAG = "TEMP" // für logging

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temp)
        Log.d(TAG, "onCreate: activity_temp.xml UI geöffnet")

        // Views
        tvTemp = findViewById(R.id.tvTemp)
        panelText = findViewById(R.id.valuePanel)
        switchBtn = findViewById(R.id.switchViewButton)
        aaChartView = findViewById(R.id.aaChartView)

        //  Platzhalter:
        tvTemp.text = "Temperatur: — °C"

        applyMode()
        setupChart()

        // Sensor init (Ambient Temperature)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)

        // Notifications
        createNotificationChannel()
        requestPostNotificationsIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        tempSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            Log.d(TAG, "onResume: registerListener")
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        Log.d(TAG, "onPause: unregisterListener")
    }

    // SensorEventListener
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_AMBIENT_TEMPERATURE) return
        val tempC = event.values.firstOrNull() ?: return
        Log.d(TAG, "onSensorChanged: tempC=$tempC °C @ts=${event.timestamp}")
        updateTemperature(tempC)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* no-op */ }

    // UI + Chart
    private fun updateTemperature(tempC: Float) {
        if (!tempC.isFinite() || abs(tempC) > 1000f) return


        Log.d(TAG, "updateTemperature: UI-Text aktualisieren -> %.1f °C".format(tempC))
        val display = String.format(Locale.getDefault(), "Temperatur: %.1f °C", tempC)
        tvTemp.text = display

        if (currentMode == Mode.CHART) {
            Log.d(TAG, "updateTemperature: Chart aktualisieren (Balken) mit $tempC °C")
            updateBarChart(tempC)
        }
        maybeNotifyHighTemp(tempC)
    }
    // Balken-Diagramm
    private fun setupChart() {
        tempSeries = AASeriesElement()
            .name("°C")
            .data(arrayOf(0.0))

        aaModel = AAChartModel()
            .chartType(AAChartType.Column)
            .title("Temperatur (°C)")
            .backgroundColor("#FFFFFF")
            .dataLabelsEnabled(false)
            .yAxisTitle("°C")
            .yAxisGridLineWidth(0.5f)
            .yAxisMin(-100f)
            .yAxisMax(150f)
            .categories(arrayOf("°C"))
            .series(arrayOf(tempSeries))

        aaChartView.aa_drawChartWithChartModel(aaModel)
    }

    private fun updateBarChart(tempC: Float) {
        tempSeries.data(arrayOf(tempC.toDouble()))
        aaChartView.aa_onlyRefreshTheChartDataWithChartOptionsSeriesArray(arrayOf(tempSeries))
    }

    // Switch (XML onClick="switchView")
    fun switchView(view: View) {
        currentMode = if (currentMode == Mode.TEXT) Mode.CHART else Mode.TEXT
        applyMode()
    }

    private fun applyMode() {
        if (currentMode == Mode.TEXT) {
            panelText.visibility = View.VISIBLE
            aaChartView.visibility = View.GONE
            switchBtn.text = "Diagramm anzeigen"
        } else {
            panelText.visibility = View.GONE
            aaChartView.visibility = View.VISIBLE
            switchBtn.text = "Text anzeigen"
        }
    }

    // Notifications
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Temperatur-Warnungen",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Benachrichtigungen bei hoher Temperatur" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun requestPostNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    77
                )
            }
        }
    }

    private fun maybeNotifyHighTemp(tempC: Float) {
        val now = System.currentTimeMillis()
        if (tempC >= THRESHOLD_C && now - lastNotifyTime >= MIN_INTERVAL_MS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) return

            val n = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Hohe Temperatur")
                .setContentText(String.format(Locale.getDefault(), "Aktuell: %.1f °C", tempC))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            Log.i(TAG, "maybeNotifyHighTemp: Notification ausgelöst bei tempC=$tempC °C")

            NotificationManagerCompat.from(this).notify(NOTIFY_ID, n)
            lastNotifyTime = now
        }
    }

    // Back-Button
    fun back_main(view: View) = finish()
}
