// Copyright 2024 Azariah Laulusa
package com.example.cameraxapp

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database

class GameOverActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_over)
        val playAgainButton = findViewById<Button>(R.id.play_again)
        val homeButton = findViewById<Button>(R.id.home)
        val currentScoreText = findViewById<TextView>(R.id.current_score)
        val database = Firebase.database
        val myCurrentRef = database.getReference("CurrentScore")

        myCurrentRef.addValueEventListener(object: ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                try {
                    val currentScore: Any? = snapshot.value
                    currentScoreText.text = "Score: $currentScore"
                } catch (e: Exception) {}
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
            }

        })

        playAgainButton.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@GameOverActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        })

        homeButton.setOnClickListener(View.OnClickListener {
            val intent = Intent(this@GameOverActivity, StartActivity::class.java)
            startActivity(intent)
            finish()
        })
    }

}
