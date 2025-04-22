package com.example.projet_progm


import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuestionnaireGameActivity : ComponentActivity() {

    private val questions = listOf(
        Question("Quelle est la capitale de la France?", listOf("Paris", "Londres", "Berlin", "Madrid"), 0),
        Question("Combien font 7 x 8?", listOf("42", "56", "64", "49"), 1),
        Question("Quel est le plus grand océan?", listOf("Atlantique", "Indien", "Pacifique", "Arctique"), 2),
        Question("Quel pays a remporté la Coupe du Monde 2018?", listOf("Allemagne", "Brésil", "France", "Argentine"), 2),
        Question("Combien de planètes dans le système solaire?", listOf("7", "8", "9", "10"), 1),
        Question("Quelle langue est parlée au Brésil?", listOf("Espagnol", "Portugais", "Français", "Italien"), 1)
    )

    private var currentQuestionIndex = 0
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
        setContentView(R.layout.activity_questionnaire_game)

        displayQuestion()

        listOf<Button>(
            findViewById(R.id.answer1),
            findViewById(R.id.answer2),
            findViewById(R.id.answer3),
            findViewById(R.id.answer4)
        ).forEachIndexed { index, button ->
            button.setOnClickListener {
                checkAnswer(index)
            }
        }
    }

    private fun displayQuestion() {
        if (currentQuestionIndex < questions.size) {
            val question = questions[currentQuestionIndex]
            findViewById<TextView>(R.id.questionText).text = question.text

            listOf<Button>(
                findViewById(R.id.answer1),
                findViewById(R.id.answer2),
                findViewById(R.id.answer3),
                findViewById(R.id.answer4)
            ).forEachIndexed { index, button ->
                button.text = question.answers[index]
            }
        } else {
            endGame()
        }
    }

    private fun checkAnswer(selectedIndex: Int) {
        val question = questions[currentQuestionIndex]
        if (selectedIndex == question.correctAnswer) {
            score += 100
        }
        currentQuestionIndex++
        displayQuestion()
    }

    private fun endGame() {
        findViewById<TextView>(R.id.questionText).text = "Jeu terminé! Score: $score"
        findViewById<LinearLayout>(R.id.answersLayout).visibility = View.GONE

        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.userDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val existingGame = dao.loadAllByIds(intArrayOf(5)).firstOrNull()
            val newScore = score.toInt()

            if (existingGame?.highScore == null || newScore > existingGame.highScore!!) {
                dao.updateHighScore(5, newScore)
            }
        }

        handler.postDelayed(endGame, 3000)
    }

    data class Question(
        val text: String,
        val answers: List<String>,
        val correctAnswer: Int
    )
}