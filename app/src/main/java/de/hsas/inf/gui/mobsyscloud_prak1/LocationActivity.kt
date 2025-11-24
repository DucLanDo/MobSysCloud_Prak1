package de.hsas.inf.gui.mobsyscloud_prak1

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.openlocationcode.OpenLocationCode
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class LocationActivity : AppCompatActivity() {

    // UI
    private lateinit var tvLat: TextView
    private lateinit var tvLon: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvPlace: TextView

    // Fused Location
    private lateinit var fused: FusedLocationProviderClient
    private lateinit var request: LocationRequest
    private lateinit var callback: LocationCallback

    private  val TAG = "LOC" // fürs logging
    private var lastLocation: Location? = null
    private var lastPlusCode: String? = null

    //firestore instanz
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val askPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { startLocationIfPermitted() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loc)
        Log.d(TAG, "onCreate: activity_loc.xml erstellt")

        // Views
        tvLat = findViewById(R.id.tvLat)
        tvLon = findViewById(R.id.tvLon)
        tvTime = findViewById(R.id.tvTime)
        tvPlace = findViewById(R.id.tvPlace)

        // Platzhalter
        tvLat.text = "Breite: —"
        tvLon.text = "Länge: —"
        tvTime.text = "Zeit: —"
        tvPlace.text = "Standort: —"

        // Fused Location
        fused = LocationServices.getFusedLocationProviderClient(this)
        request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(0f)
            .build()
        Log.d(TAG, "onCreate: LocationRequest konfiguriert")

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { updateUI(it) }

            }
        }

        startLocationIfPermitted()
    }

    override fun onResume() {
        super.onResume()
        startLocationIfPermitted()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        fused.removeLocationUpdates(callback)
        Log.d(TAG, "onPause: removeLocationUpdates")
    }

    // Back-Button
    fun back_main(view: View) = finish()

    private fun startLocationIfPermitted() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            askPermissions.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
            return
        }

        // Kontinuierliche Updates
        fused.requestLocationUpdates(request, callback, mainLooper)

        // Startwert
        val cts = com.google.android.gms.tasks.CancellationTokenSource()
        fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null) updateUI(loc) else fused.lastLocation.addOnSuccessListener { last ->
                    if (last != null) updateUI(last)
                }
            }
    }

    private fun updateUI(loc: Location) {
        // letzte Location merken
        lastLocation = loc

        tvLat.text = String.format("Breite: %.6f", loc.latitude)
        tvLon.text = String.format("Länge: %.6f", loc.longitude)

        val fmt = SimpleDateFormat("HH:mm:ss (dd.MM.yyyy)", Locale.getDefault())
        tvTime.text = String.format("Zeit: %s", fmt.format(loc.time))

        // Plus Code aus Koordinaten berechnen
        val plusCode = OpenLocationCode.encode(loc.latitude, loc.longitude, 10)
        lastPlusCode = plusCode
        tvPlace.text = "Standort: $plusCode"

        Log.d(TAG, "updateUI: PlusCode=$plusCode")
    }

    fun saveLocation(view: View) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Bitte zuerst einloggen", Toast.LENGTH_SHORT).show()
            return
        }

        val loc = lastLocation
        val plusCode = lastPlusCode

        if (loc == null || plusCode == null) {
            Toast.makeText(this, "Kein Standort verfügbar. Bitte zuerst Standort aktualisieren.", Toast.LENGTH_LONG).show()
            return
        }

        val data = hashMapOf(
            "userId" to user.uid,
            "latitude" to loc.latitude,
            "longitude" to loc.longitude,
            "plusCode" to plusCode,
            "timestamp" to Date(loc.time)
        )

        db.collection("locations")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Standort gespeichert", Toast.LENGTH_SHORT).show()
                Log.d("LOC", "Location saved with id=${it.id}")
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Fehler beim Speichern: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                Log.e("LOC", "Error saving location", e)
            }
    }

}
