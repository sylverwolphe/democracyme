package com.lm.democracyme

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.w3c.dom.Text

class RegLogActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var democracyText: TextView
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reg_log)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        democracyText = findViewById(R.id.democracyText)
        loginButton = findViewById(R.id.loginButton)
        registerButton = findViewById(R.id.registerButton)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        submitButton = findViewById(R.id.submitButton)

        // Check if the user is already logged in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // If the user is already authenticated, navigate to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Finish the current activity to prevent going back to it
        } else {
            // User is not logged in, show the login/register UI
            democracyText.visibility = View.VISIBLE
            loginButton.visibility = View.VISIBLE
            registerButton.visibility = View.VISIBLE
            emailInput.visibility = View.GONE
            passwordInput.visibility = View.GONE
            submitButton.visibility = View.GONE
        }

        loginButton.setOnClickListener {
            showTextFields()
            selectButton(loginButton)
            deselectButton(registerButton)
            emailInput.requestFocus()
        }

        registerButton.setOnClickListener {
            showTextFields()
            selectButton(registerButton)
            deselectButton(loginButton)
            emailInput.requestFocus()
        }

        submitButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (loginButton.isSelected) {
                // Handle login
                loginUser(email, password)
            } else if (registerButton.isSelected) {
                // Handle registration
                registerUser(email, password)
            }
        }
    }


    private fun showTextFields() {
        democracyText.visibility = View.GONE
        loginButton.visibility = View.VISIBLE
        registerButton.visibility = View.VISIBLE
        emailInput.visibility = View.VISIBLE
        passwordInput.visibility = View.VISIBLE
        submitButton.visibility = View.VISIBLE
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    // Login successful, navigate to MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Finish the current activity to prevent going back to it

                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val userId = user?.uid

                    // Create a user document in the "DMusers" collection
                    if (userId != null) {
                        val userDocument = firestore.collection("DMusers").document(userId)
                        val userData = hashMapOf(
                            "email" to email,
                            // Add any other user data you want to store here
                        )

                        userDocument.set(userData)
                            .addOnSuccessListener {
                                // Registration successful, navigate to MainActivity
                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()

                                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Registration failed: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun selectButton(button: Button) {
        button.isSelected = true
        button.setBackgroundResource(R.drawable.selected_button)
    }

    private fun deselectButton(button: Button) {
        button.isSelected = false
        button.setBackgroundResource(R.drawable.default_button)
    }
}
