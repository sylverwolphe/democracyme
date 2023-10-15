package com.lm.democracyme

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.play.integrity.internal.e
import java.util.Date
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class QuestionAdapter(
    private val questions: ArrayList<String>,
    private val layoutInflater: LayoutInflater,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : RecyclerView.Adapter<QuestionAdapter.ViewHolder>() {

    // Provide a reference to the views for each data item
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val reasonsText: TextView = itemView.findViewById(R.id.reasonsText)
        val questionText: TextView = itemView.findViewById(R.id.questionText)
        val toggleDiscussion: TextView = itemView.findViewById(R.id.toggleDiscussion)
        val discussionCardView: CardView = itemView.findViewById(R.id.discussionCardView)
        val yesButton: Button = itemView.findViewById(R.id.yesButton)
        val noButton: Button = itemView.findViewById(R.id.noButton)
        val abstainButton: Button = itemView.findViewById(R.id.abstainButton)

        data class QuestionData(
            val id: String,
            val text: String
        )
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = layoutInflater.inflate(R.layout.question_item, parent, false)
        return ViewHolder(itemView)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val questionId = questions[position] // Get the question ID from the list
        val userId = auth.currentUser?.uid

        if (userId != null) {
            // Construct the path to the question text in Firestore
            val questionPath = "DMusers/$userId/questions/$questionId"

            // Fetch the question text from Firestore
            firestore.document(questionPath)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val questionText = documentSnapshot.getString("text")
                        holder.questionText.text = questionText

                        // Set the position as a tag for the buttons
                        holder.yesButton.tag = position
                        holder.noButton.tag = position
                        holder.abstainButton.tag = position

                        // Rest of your code for setting click listeners
                        // Click listener for the "Yes" button
                        holder.yesButton.setOnClickListener {
                            val position = it.tag as Int // Get the position from the button's tag
                            val questionAssociatedWithVote = questions[position]
                            showVoteDialog("Yes", questionAssociatedWithVote)
                            showToast("Yes button clicked. Question: $questionAssociatedWithVote", it.context)
                        }

                        // Click listener for the "No" button
                        holder.noButton.setOnClickListener {
                            val position = it.tag as Int // Get the position from the button's tag
                            val questionAssociatedWithVote = questions[position]
                            showVoteDialog("No", questionAssociatedWithVote)
                        }

                        // Click listener for the "Abstain" button
                        holder.abstainButton.setOnClickListener {
                            val position = it.tag as Int // Get the position from the button's tag
                            val questionAssociatedWithVote = questions[position]
                            showVoteDialog("Abstain", questionAssociatedWithVote)
                        }

                        holder.toggleDiscussion.setOnClickListener {
                            if (holder.discussionCardView.visibility == View.VISIBLE) {
                                holder.discussionCardView.visibility = View.GONE
                                holder.toggleDiscussion.text = holder.itemView.context.getString(R.string.open_discussion)
                                holder.reasonsText.visibility = View.GONE
                            } else {
                                holder.discussionCardView.visibility = View.VISIBLE
                                holder.toggleDiscussion.text = holder.itemView.context.getString(R.string.close_discussion)
                                // Fetch and display vote reasons when expanding the discussion
                                fetchAndDisplayVoteReasons(holder, questionPath)
                            }
                        }

                    }
                }
                .addOnFailureListener { e ->
                    Log.e("QuestionAdapter", "Error fetching question: $e")
                }
        }
    }

    private fun fetchAndDisplayVoteReasons(holder: ViewHolder, questionPath: String) {
        // Fetch all vote reasons associated with the question
        firestore.collection("$questionPath/votes")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val voteReasons = mutableListOf<String>()

                for (document in querySnapshot.documents) {
                    val voteData = document.data
                    val reason = voteData?.get("reason") as String?
                    if (!reason.isNullOrBlank()) {
                        voteReasons.add(reason)
                    }
                }

                // Display the vote reasons under the discussion toggle
                val voteReasonsText = voteReasons.joinToString("\n\n")
                holder.reasonsText.text = voteReasonsText

                // Make the reasons text visible
                holder.reasonsText.visibility = View.VISIBLE

            }
            .addOnFailureListener { e ->
                Log.e("QuestionAdapter", "Error fetching vote reasons: $e")
            }
    }




    private fun showToast(message: String, context: Context) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }


    private fun showVoteDialog(voteText: String, question: String) {
        val dialogView = layoutInflater.inflate(R.layout.vote_dialog, null)
        val voteReasonInput = dialogView.findViewById<EditText>(R.id.voteReasonInput)
        val submitVoteButton = dialogView.findViewById<Button>(R.id.submitVoteButton)

        val dialog = AlertDialog.Builder(dialogView.context)
            .setView(dialogView)
            .create()

        submitVoteButton.setOnClickListener {
            val voteReason = voteReasonInput.text.toString()
            val userId = auth.currentUser?.uid

            if (!userId.isNullOrBlank()) {
                // Create a unique voteId
                val voteId = firestore.collection("DMusers").document(userId)
                    .collection("questions").document(question)
                    .collection("votes").document().id

                // Create a map for the vote data
                val voteData = hashMapOf(
                    "vote" to voteText,
                    "reason" to voteReason,
                    "date" to Date()
                )

                // Add the vote data to the "votes" collection with the unique voteId
                firestore.collection("DMusers").document(userId)
                    .collection("questions").document(question)
                    .collection("votes").document(voteId)
                    .set(voteData)
                    .addOnSuccessListener {
                        // Handle success
                        dialog.dismiss()
                    }
                    .addOnFailureListener {
                        // Handle failure
                    }
            }
        }

        dialog.show()
    }

    private fun associateVoteWithQuestion(userId: String, voteId: String, question: String) {
        // Get the user's document
        val userDocument = firestore.collection("DMusers").document(userId)

        // Create a reference to the specific question
        val questionReference = userDocument.collection("questions").document(question)

        // Update the question document to include the vote reference
        questionReference.update("votes", firestore.collection("votes").document(voteId))
            .addOnSuccessListener {
                Log.d("VoteDialog", "Vote associated with question successfully.")
            }
            .addOnFailureListener {
                Log.e("VoteDialog", "Error associating vote with question")
            }
    }


    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return questions.size
    }

    // Set the questions for the adapter
    fun setQuestions(newQuestions: List<String>) {
        questions.clear()
        questions.addAll(newQuestions)
        notifyDataSetChanged()
    }
}
