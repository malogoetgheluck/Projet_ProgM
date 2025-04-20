package com.example.projet_progm


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.projet_progm.QuestionnaireGameActivity

class EnigmeActivity : ComponentActivity() {

    private val charades = listOf(
        Charade(
            "Je suis grand quand je suis jeune et petit quand je suis vieux. Qui suis-je?", "une bougie"),
        Charade("quel est le meilleur club football de l'histoire", "real"),
        Charade("test je fait n 'importe quoi ","test")
    )

    private var currentRiddleIndex = 0
    private var score: Long = 0

    //To quit the game with a little delay
    private val handler = Handler(Looper.getMainLooper())
    private val endGame = object : Runnable {
        override fun run() {
            val resultIntent = Intent()
            resultIntent.putExtra("score", score)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_questionnaire_enigme)
        displayCharade()
        val validateButton = findViewById<Button>(R.id.validateButton)
        validateButton.setOnClickListener {
            checkAnswer()
        }
    }

    private fun displayCharade() {
        if (currentRiddleIndex < charades.size) {
            findViewById<TextView>(R.id.riddleText).text = charades[currentRiddleIndex].text
            findViewById<EditText>(R.id.answerInput).text.clear()
        } else {
            endGame()
        }
    }

    private fun checkAnswer() {
        val userAnswer = findViewById<EditText>(R.id.answerInput).text.toString().trim().lowercase()
        val correctAnswer = charades[currentRiddleIndex].answer.lowercase()

        if (userAnswer == correctAnswer) {
            score += 100
            Toast.makeText(this, "Bravo! Bonne réponse", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "Presque! La réponse était: ${charades[currentRiddleIndex].answer}",
                Toast.LENGTH_LONG
            ).show()
        }

        currentRiddleIndex++
        displayCharade()
    }

    private fun endGame() {
        findViewById<TextView>(R.id.riddleText).text = "Jeu terminé! Score: $score"
        findViewById<EditText>(R.id.answerInput).visibility = View.GONE
        findViewById<Button>(R.id.validateButton).visibility = View.GONE
        /*
        android.os.Handler().postDelayed({
            finish()
        }, 3000)*/

        handler.postDelayed(endGame, 3000)
    }

    data class Charade(
        val text: String,
        val answer: String
    )
}