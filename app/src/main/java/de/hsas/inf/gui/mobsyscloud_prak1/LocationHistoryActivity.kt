package de.hsas.inf.gui.mobsyscloud_prak1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationHistoryActivity : AppCompatActivity() {

    private lateinit var lvLocations: ListView
    private lateinit var tvEmpty: TextView

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_history)

        lvLocations = findViewById(R.id.lvLocations)
        tvEmpty = findViewById(R.id.tvEmpty)

        loadLocations()
    }

    private fun loadLocations() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Bitte zuerst einloggen", Toast.LENGTH_SHORT).show()
            // Zur Sicherheit zurück zur LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        db.collection("locations")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                handleResult(snapshot)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Fehler beim Laden: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun handleResult(snapshot: QuerySnapshot) {
        if (snapshot.isEmpty) {
            tvEmpty.visibility = View.VISIBLE
            lvLocations.visibility = View.GONE
            return
        }

        val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

        val items = snapshot.documents.map { doc ->
            val lat = doc.getDouble("latitude") ?: 0.0
            val lon = doc.getDouble("longitude") ?: 0.0
            val plusCode = doc.getString("plusCode") ?: "-"
            val ts = doc.get("timestamp")

            val date: Date? = when (ts) {
                is com.google.firebase.Timestamp -> ts.toDate()
                is Date -> ts
                else -> null
            }

            val timeStr = date?.let { fmt.format(it) } ?: "ohne Zeitstempel"

            // Text, der im ListView angezeigt wird:
            "Zeit: $timeStr\nPlusCode: $plusCode\nLat: %.5f, Lon: %.5f".format(lat, lon)
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            items
        )

        lvLocations.adapter = adapter
        lvLocations.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
    }

    fun back_main(view: View) {
        finish() // endet activity
    }
}
