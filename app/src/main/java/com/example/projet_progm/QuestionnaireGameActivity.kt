package com.example.projet_progm


import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuestionnaireGameActivity : ComponentActivity() {

    private var questions = listOf(
        Question("If I have a troll a gnome and a spider, how many legs do I have ?", listOf("8", "10", "12", "14"), 2),
        Question("A farmer has a rooster. Knowing that a hen can lay an egg every week, how many eggs will the farmer have at the end of a year ?", listOf("52", "55", "0", "1"), 2),
        Question("What does a pyrobarbarian see in a grove ?", listOf("A rabbit", "A peaceful land of joy", "A big bonefire !!!", "Himself"), 2),
        Question("In Medieval Germany, a woman could divorce her husband through a Marital Duel. However, to make it fair, the man had a disadvantage. Which was it ?", listOf("The man needed to always hold a potato in his dominant hand", "The man needed to fight in a hole", "The woman fights the man on a horse", "The man had no weapon whereas the woman had an axe"), 1),
        Question("Combien de planètes dans le système solaire?", listOf("7", "8", "9", "10"), 1),
        Question("Quelle langue est parlée au Brésil?", listOf("Espagnol", "Portugais", "Français", "Italien"), 1)
    )

    private val shuffledQuestions = questions.shuffled()

    private var currentQuestionIndex = 0
    private var questionScore = 0
    private var multiplierScore: Double = 0.0
    private var score: Long = 0

    private lateinit var scoreTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var welldoneTextView: TextView
    private var countDownTimer: CountDownTimer? = null
    private val totalTime: Long = 45000 // 60 seconds

    private lateinit var scoreLayout : FrameLayout

    var toast: Toast? = null

    private var numberOfQuestion = 9

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

        // Initialize UI elements
        scoreTextView = findViewById(R.id.scoreTextView)
        timerTextView = findViewById(R.id.timerTextView)
        welldoneTextView = findViewById(R.id.welldoneTextView)

        startTimer()

        scoreLayout = findViewById(R.id.scoreLayout)
        scoreLayout.visibility = View.GONE
    }

    private fun displayQuestion() {
        if (currentQuestionIndex < numberOfQuestion) {
            val question = shuffledQuestions[currentQuestionIndex]
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
            endGame(true)
        }
    }

    private fun checkAnswer(selectedIndex: Int) {
        val question = shuffledQuestions[currentQuestionIndex]
        if (selectedIndex == question.correctAnswer) {
            questionScore += 111
            toast?.cancel()
            toast = Toast.makeText(this, "Well-done ! Good answer", Toast.LENGTH_SHORT)
            toast?.show()
        } else {
            toast?.cancel()
            toast = Toast.makeText(
                this,
                "Almost ! The answer was : ${question.answers[question.correctAnswer]}",
                Toast.LENGTH_SHORT
            )
            toast?.show()
        }
        currentQuestionIndex++
        displayQuestion()
    }

    private fun endGame(win: Boolean) {
        findViewById<TextView>(R.id.questionText).visibility = View.GONE
        findViewById<LinearLayout>(R.id.answersLayout).visibility = View.GONE

        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.userDao()

        countDownTimer?.cancel()
        scoreLayout.visibility = View.VISIBLE
        if (win){
            //Log.d("DEBUG",multiplierScore.toString())
            welldoneTextView.text = "Well done"
            score = (questionScore * multiplierScore).toLong()
        } else {
            welldoneTextView.text = "Another time ?"
            score = 0
        }
        scoreTextView.text = "Score: "+score

        lifecycleScope.launch(Dispatchers.IO) {
            val existingGame = dao.loadAllByIds(intArrayOf(5)).firstOrNull()
            val newScore = score.toInt()

            if (existingGame?.highScore == null || newScore > existingGame.highScore!!) {
                dao.updateHighScore(5, newScore)
            }
        }

        handler.postDelayed(endGame, 3000)
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(totalTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTextView.text = "Time: ${millisUntilFinished / 1000}s"
                multiplierScore = millisUntilFinished.toDouble()/totalTime.toDouble()
                //Log.d("DEBUG",multiplierScore.toString())
                //Log.d("DEBUG", (millisUntilFinished.toDouble()/totalTime.toDouble()).toString())
            }

            @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
            override fun onFinish() {
                timerTextView.text = "Time's up!"
                endGame(false)
            }
        }.start()
    }

    data class Question(
        val text: String,
        val answers: List<String>,
        val correctAnswer: Int
    )
}