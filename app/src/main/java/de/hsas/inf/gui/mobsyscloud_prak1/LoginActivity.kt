package de.hsas.inf.gui.mobsyscloud_prak1

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var progress: ProgressBar
    //firestore instanz
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)
        progress = findViewById(R.id.progress)

        btnLogin.setOnClickListener { loginUser() }
        btnRegister.setOnClickListener { registerUser() }
    }

    override fun onStart() {
        super.onStart()
        // Wenn schon eingeloggt → direkt zur MainActivity
        val currentUser = auth.currentUser
        if (currentUser != null) {
            goToMain()
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Bitte E-Mail und Passwort eingeben", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login erfolgreich", Toast.LENGTH_SHORT).show()
                    goToMain()
                } else {
                    Toast.makeText(
                        this,
                        "Login fehlgeschlagen: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun registerUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Bitte E-Mail und Passwort eingeben", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                setLoading(false)
                if (task.isSuccessful) {

                    val user = auth.currentUser
                    if (user != null) {
                        // User-Dokument erstellen
                        val userData = hashMapOf(
                            "email" to user.email,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )

                        db.collection("users")
                            .document(user.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Log.d("AUTH", "User document created for ${user.uid}")
                            }
                            .addOnFailureListener { e ->
                                Log.e("AUTH", "Error creating user document", e)
                            }
                    }

                    Toast.makeText(this, "Registrierung erfolgreich", Toast.LENGTH_SHORT).show()
                    goToMain()

                } else {
                    Toast.makeText(
                        this,
                        "Registrierung fehlgeschlagen: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !loading
        btnRegister.isEnabled = !loading
    }
}
