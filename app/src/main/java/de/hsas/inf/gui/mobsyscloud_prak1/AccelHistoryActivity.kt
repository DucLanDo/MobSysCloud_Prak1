package de.hsas.inf.gui.mobsyscloud_prak1

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

class AccelHistoryActivity : AppCompatActivity() {

    private lateinit var lvAccel: ListView
    private lateinit var tvEmpty: TextView
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accel_history)

        lvAccel = findViewById(R.id.lvAccel)
        tvEmpty = findViewById(R.id.tvEmptyAccel)

        loadData()
    }

    private fun loadData() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Bitte zuerst einloggen", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db.collection("accelerations")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { snapshot ->
                handleResult(snapshot)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Fehler beim Laden: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun handleResult(snapshot: QuerySnapshot) {
        if (snapshot.isEmpty) {
            tvEmpty.visibility = View.VISIBLE
            lvAccel.visibility = View.GONE
            return
        }

        val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

        val items = snapshot.documents.map { doc ->
            val x = doc.getDouble("x") ?: 0.0
            val y = doc.getDouble("y") ?: 0.0
            val z = doc.getDouble("z") ?: 0.0
            val ts = doc.get("timestamp")
            val date: Date? = when (ts) {
                is com.google.firebase.Timestamp -> ts.toDate()
                is Date -> ts
                else -> null
            }
            val timeStr = date?.let { fmt.format(it) } ?: "ohne Zeitstempel"

            "Zeit: $timeStr\nX: %.2f, Y: %.2f, Z: %.2f m/s²".format(x, y, z)
        }

        lvAccel.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            items
        )
        lvAccel.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
    }

    fun back_main(view: View) {
        finish()
    }
}
