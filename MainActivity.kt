//package com.lm.democracyme
//
//import android.app.AlertDialog
//import android.os.Bundle
//import android.view.View
//import android.widget.Button
//import android.widget.EditText
//import android.widget.ImageView
//import androidx.appcompat.app.AppCompatActivity
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import java.util.Date
//
//class MainActivity : AppCompatActivity() {
//    private lateinit var auth: FirebaseAuth
//    private lateinit var firestore: FirebaseFirestore
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        auth = FirebaseAuth.getInstance()
//        firestore = FirebaseFirestore.getInstance()
//
//        val addImage = findViewById<ImageView>(R.id.addImage)
//
//        addImage.setOnClickListener {
//            showYnQuestionDialog()
//        }
//    }
//
//    private fun showYnQuestionDialog() {
//        val dialogView = layoutInflater.inflate(R.layout.dialog_y_n, null)
//        val ynQuestionInput = dialogView.findViewById<EditText>(R.id.yNQuestionInput)
//        val submitButton = dialogView.findViewById<Button>(R.id.submitYnButton)
//
//        val dialog = AlertDialog.Builder(this)
//            .setView(dialogView)
//            .create()
//
//        ynQuestionInput.requestFocus()
//
//        submitButton.setOnClickListener {
//            val ynQuestion = ynQuestionInput.text.toString()
//            val userId = auth.currentUser?.uid
//
//            if (!userId.isNullOrBlank()) {
//                val userDocument = firestore.collection("DMusers").document(userId)
//                val questionData = hashMapOf(
//                    "text" to ynQuestion,
//                    "date" to Date()
//                )
//
//                // Add the question to Firestore
//                userDocument.collection("questions").add(questionData)
//                    .addOnSuccessListener {
//                        // Handle success
//                        dialog.dismiss()
//                    }
//                    .addOnFailureListener {
//                        // Handle failure
//                    }
//            }
//
//            dialog.dismiss()
//        }
//
//        dialog.show()
//    }
//}

package com.lm.democracyme

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var questionsRecyclerView: RecyclerView
    private lateinit var questionAdapter: QuestionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val addImage = findViewById<ImageView>(R.id.addImage)
        questionsRecyclerView = findViewById(R.id.questionList)

        // Create and set the layout manager for the RecyclerView
        questionsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Create an adapter for the RecyclerView
        questionAdapter = QuestionAdapter(ArrayList(), layoutInflater, auth, firestore)
        questionsRecyclerView.adapter = questionAdapter

        addImage.setOnClickListener {
            showYnQuestionDialog()
        }

        // Load questions from Firestore
        loadQuestionsFromFirestore()
    }

    private fun showYnQuestionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_y_n, null)
        val ynQuestionInput = dialogView.findViewById<EditText>(R.id.yNQuestionInput)
        val submitButton = dialogView.findViewById<Button>(R.id.submitYnButton)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        ynQuestionInput.requestFocus()

        submitButton.setOnClickListener {
            val ynQuestion = ynQuestionInput.text.toString()
            val userId = auth.currentUser?.uid

            if (!userId.isNullOrBlank()) {
                val userDocument = firestore.collection("DMusers").document(userId)
                val questionData = hashMapOf(
                    "text" to ynQuestion,
                    "date" to Date()
                )

                // Add the question to Firestore
                userDocument.collection("questions").add(questionData)
                    .addOnSuccessListener {
                        // Handle success
                        dialog.dismiss()
                        // Reload questions after adding a new one
                        loadQuestionsFromFirestore()
                    }
                    .addOnFailureListener {
                        // Handle failure
                    }
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun loadQuestionsFromFirestore() {
        val userId = auth.currentUser?.uid

        if (!userId.isNullOrBlank()) {
            val userDocument = firestore.collection("DMusers").document(userId)
            userDocument.collection("questions")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val questionIDs = ArrayList<String>() // Change to store IDs

                    for (document in querySnapshot.documents) {
                        val questionID = document.id // Retrieve the document ID
                        questionIDs.add(questionID)
                    }

                    // Update the RecyclerView with the fetched question IDs
                    questionAdapter.setQuestions(questionIDs)
                }
                .addOnFailureListener {
                    // Handle failure
                }
        }
    }

}
