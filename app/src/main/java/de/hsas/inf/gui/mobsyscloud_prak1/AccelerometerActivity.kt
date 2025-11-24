package de.hsas.inf.gui.mobsyscloud_prak1

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
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
import kotlin.math.abs
import android.util.Log

import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date
import kotlin.math.abs


// AAChartCore-Kotlin
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartView
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement

class AccelerometerActivity : AppCompatActivity(), SensorEventListener { //bekommt Sensor-Callbacks (onSensorChanged, onAccuracyChanged

    //  Sensor
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // UI
    private lateinit var tvX: TextView
    private lateinit var tvY: TextView
    private lateinit var tvZ: TextView
    private lateinit var panelText: LinearLayout // Container für die Text-Anzeige.

    private lateinit var switchBtn: Button
    private lateinit var aaChartView: AAChartView
    private lateinit var aaModel: AAChartModel

    //  Modus Text und Diagramm
    private enum class Mode { TEXT, CHART }
    private var currentMode = Mode.TEXT

    private val alpha = 0.2f
    private var fx = 0f
    private var fy = 0f
    private var fz = 0f

    // Upside-down Erkennung + Notification
    private val TOL_NEAR_ZERO = 2.0f      //Toleranzen
    private val Y_TARGET = -9.81f
    private val Y_TOL = 2.0f
    private var isUpsideDown = false

    private val CHANNEL_ID = "accel_posture"
    private val NOTIFY_ID = 1010

    private val TAG = "ACCEL" // für logging
    //firebasestore instanz
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private var lastAccelTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accel)
        Log.d(TAG, "onCreate: activity_accel.xml erstellt")

        // Views holen
        tvX = findViewById(R.id.tvX)
        tvY = findViewById(R.id.tvY)
        tvZ = findViewById(R.id.tvZ)
        panelText = findViewById(R.id.valuePanel)
        switchBtn = findViewById(R.id.switchViewButton)
        aaChartView = findViewById(R.id.aaChartView)

        // Switch initialisieren
        applyMode()
        setupAAChart()

        // Sensor initialisieren
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Notification-Setup
        createNotificationChannel()
        requestPostNotificationsIfNeeded()
    }

    // onClick="switchView"
    fun switchView(view: View) {
        currentMode = if (currentMode == Mode.TEXT) Mode.CHART else Mode.TEXT
        Log.d(TAG, "switchView: neuer Modus=$currentMode")
        applyMode()
    }

    private fun applyMode() { //steuert die Button-Anzeige
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

    private fun setupAAChart() {
        // Balkendiagramm mit Kategorien X, Y, Z
        aaModel = AAChartModel()
            .chartType(AAChartType.Column)
            .title("Beschleunigung (m/s²)")
            .backgroundColor("#FFFFFF")
            .dataLabelsEnabled(false)
            .yAxisTitle("m/s²")
            .yAxisGridLineWidth(0.5f)
            .yAxisMin(-12f)
            .yAxisMax(12f)
            .categories(arrayOf("X", "Y", "Z"))
            .series(arrayOf(
                AASeriesElement()
                    .name("m/s²")
                    .data(arrayOf(0.0, 0.0, 0.0))
            ))
        aaChartView.aa_drawChartWithChartModel(aaModel)
    }

    // Aktualisiert die 3 Balken mit den gefilterten X/Y/Z-Werten
    private fun updateBarChart(x: Float, y: Float, z: Float) {
        val series = arrayOf(
            AASeriesElement()
                .name("Achsen")
                .data(arrayOf(x.toDouble(), y.toDouble(), z.toDouble()))
        )
        aaChartView.aa_onlyRefreshTheChartDataWithChartOptionsSeriesArray(series)
        Log.d(TAG, "updateBarChart: X=%.2f Y=%.2f Z=%.2f".format(x, y, z))
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            Log.d(TAG, "onResume: registerListener SENSOR_DELAY_UI")
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        Log.d(TAG, "onPause: unregisterListener")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        lastAccelTime = System.currentTimeMillis()

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]


        fx += alpha * (x - fx)
        fy += alpha * (y - fy)
        fz += alpha * (z - fz)

        // Text anzeige
        tvX.text = "X: %.2f m/s²".format(fx)
        tvY.text = "Y: %.2f m/s²".format(fy)
        tvZ.text = "Z: %.2f m/s²".format(fz)
        Log.d(TAG, "UI: Text aktualisiert")

        // Diagramm Ansicht
        if (currentMode == Mode.CHART) {
            updateBarChart(fx, fy, fz)
        }

        // Upside-down erkennen ->  Notification
        val nowUpsideDown =
            (abs(fx) < TOL_NEAR_ZERO) &&
                    (abs(fz) < TOL_NEAR_ZERO) &&
                    (fy in (Y_TARGET - Y_TOL)..(Y_TARGET + Y_TOL))

        if (nowUpsideDown && !isUpsideDown) {
            isUpsideDown = true
            postUpsideDownNotification()
        } else if (!nowUpsideDown && isUpsideDown) {
            isUpsideDown = false
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {  }

    // Back-Button
    fun back_main(view: View) = finish()

    // Notifications

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Haltungserkennung",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Benachrichtigung, wenn das Gerät auf dem Kopf steht" }
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
                    42
                )
            }
        }
    }

    private fun postUpsideDownNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Handy steht auf dem Kopf")
            .setContentText("x≈0, y≈−9.81, z≈0 erkannt")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFY_ID, n)
        Log.i(TAG, "Notification gesendet (id=$NOTIFY_ID)")
    }
    //speichert daten in firebase
    fun saveAcceleration(view: View) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Bitte zuerst einloggen", Toast.LENGTH_SHORT).show()
            return
        }

        // Aktuelle gefilterte Werte (fx, fy, fz)
        val x = fx
        val y = fy
        val z = fz


        if (lastAccelTime == 0L) {
            Toast.makeText(this, "Noch keine Messung vorhanden.", Toast.LENGTH_SHORT).show()
            return
        }

        val data = hashMapOf(
            "userId" to user.uid,
            "x" to x,
            "y" to y,
            "z" to z,
            "timestamp" to Date(lastAccelTime)
        )

        db.collection("accelerations")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Beschleunigung gespeichert", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Fehler beim Speichern: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    fun openAccelHistory(view: View) {
        val intent = Intent(this, AccelHistoryActivity::class.java)
        startActivity(intent)
    }
}
