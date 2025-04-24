package com.example.projet_progm


import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EnigmeActivity : ComponentActivity() {
    private lateinit var musicPlayer: MusicPlayer

    private lateinit var scoreTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var welldoneTextView: TextView
    private var countDownTimer: CountDownTimer? = null
    private val totalTime: Long = 60000 // 60 seconds

    private lateinit var scoreLayout : FrameLayout

    private val charades = listOf(
        Charade("I am an odd number. Take away a letter and I become even. What number am I ?", "seven"),
        Charade("What always runs but never walks. Often murmurs, never talks. Has a bed but never sleeps. An open mouth that never eats ?", "river"),
        Charade("I don’t have eyes, but once I did see. I once had thoughts, now white and empty.","skull"),
        Charade("My life can be measured in hours, I only serve to be devoured. Slim, I am quick. Fat, I am slow. Wind is my foe.", "candle"),
        Charade("What is it that given one, you’ll have either two or none ?", "choice"),
        Charade("If you drop me, I’m sure to crack. Give me a smile, and I’ll always smile back.", "mirror"),
        Charade("Never resting, never still. Moving silently from hill to hill. It does not walk, run or trot. All is cool where it is not.", "sunshine"),
        Charade("A father’s child, a mother’s child, yet no one’s son. Who am I?", "daughter"),
        Charade("What has cities, but no houses; forests, but no trees; and water, but no fish?", "map"),
        Charade("Pronounced as one letter, and written with three, two letters there are, and two only in me. I’m double, I’m single, I’m black, blue, and grey, I’m read from both ends, and the same either way.", "eye"),
        Charade("The cost of making only the maker knows, valueless if bought, but sometimes traded. A poor man may give one as easily as a king. When one is broken pain and deceit are assured.", "promise"),
        Charade("I am born in fear, raised in truth, and I come to my own in deed. When comes a time that I’m called forth, I come to serve the cause of need.", "courage"),
        Charade("What goes through a door but never goes in or comes out?", "keyhole"),
        Charade("What does man love more than life, fear more than death or mortal strife. What the poor have, the rich lack, and what contented men desire, what the miser spends and the spendthrift saves and all men carry to their graves?", "nothing"),
        Charade("Three lives have I. Gentle enough to soothe the skin, light enough to caress the sky, hard enough to crack rocks.", "water"),
        Charade("Often held but never touched, always wet but never rusts, often bites but seldom bit, to use me well you must have wit.", "tongue"),
        Charade("You saw me where I never was and where I could not be. And yet within that very place, my face you often see.", "reflection"),
        Charade("Alive without breath, as cold as death, clad in mail never clinking, never thirsty, ever drinking", "fish"),
        Charade("I go around in circles, but always straight ahead. Never complain, no matter where I am led.", "wheel"),
        Charade("A thousand coloured folds stretch toward the sky, atop a tender strand, rising from the land, ‘til killed by maiden’s hand, perhaps a token of love, perhaps to say goodbye.", "flower"),
        Charade("What's the best football club in history", "real"),
    )

    private val shuffledCharades = charades.shuffled()

    private var currentRiddleIndex = 0
    private var questionScore = 0
    private var multiplierScore: Double = 0.0
    private var score: Long = 0

    private var numberOfQuestion = 3

    var toast: Toast? = null

    //To quit the game with a little delay
    private val handler = Handler(Looper.getMainLooper())
    private val endGame = object : Runnable {
        override fun run() {
            val resultIntent = Intent()
            resultIntent.putExtra("score", score.toInt())
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_questionnaire_enigme)

        musicPlayer = MusicPlayer(this)
        musicPlayer.playMusic(R.raw.minigame)
        musicPlayer.loadSound("success", R.raw.success)
        musicPlayer.loadSound("failure", R.raw.gameover)

        displayCharade()
        val validateButton = findViewById<Button>(R.id.validateButton)
        validateButton.setOnClickListener {
            checkAnswer()
        }

        // Initialize UI elements
        scoreTextView = findViewById(R.id.scoreTextView)
        timerTextView = findViewById(R.id.timerTextView)
        welldoneTextView = findViewById(R.id.welldoneTextView)

        startTimer()

        scoreLayout = findViewById(R.id.scoreLayout)
        scoreLayout.visibility = View.GONE
    }

    private fun displayCharade() {
        if (currentRiddleIndex < numberOfQuestion) {
            findViewById<TextView>(R.id.riddleText).text = shuffledCharades[currentRiddleIndex].text
            findViewById<EditText>(R.id.answerInput).text.clear()
        } else {
            endGame(true)
        }
    }

    private fun checkAnswer() {
        val userAnswer = findViewById<EditText>(R.id.answerInput).text.toString().trim().lowercase()
        val correctAnswer = shuffledCharades[currentRiddleIndex].answer.lowercase()

        if (correctAnswer in userAnswer) {
            questionScore += 333
            toast?.cancel()
            toast = Toast.makeText(this, "Well-done ! Good answer", Toast.LENGTH_SHORT)
            toast?.show()
        } else {
            toast?.cancel()
            toast = Toast.makeText(
                this,
                "Almost ! The answer was : ${shuffledCharades[currentRiddleIndex].answer}",
                Toast.LENGTH_SHORT
            )
            toast?.show()
        }

        currentRiddleIndex++
        displayCharade()
    }

    private fun endGame(win: Boolean) {
        findViewById<TextView>(R.id.riddleText).visibility = View.GONE
        findViewById<EditText>(R.id.answerInput).visibility = View.GONE
        findViewById<Button>(R.id.validateButton).visibility = View.GONE

        countDownTimer?.cancel()
        scoreLayout.visibility = View.VISIBLE
        if (win){
            //Log.d("DEBUG",multiplierScore.toString())
            welldoneTextView.text = "Well done"
            score = (questionScore * multiplierScore).toLong()

            musicPlayer.playSound("success")
        } else {
            welldoneTextView.text = "Another time ?"
            score = 0

            musicPlayer.playSound("failure")
        }
        scoreTextView.text = "Score: "+score

        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.userDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val existingGame = dao.loadAllByIds(intArrayOf(3)).firstOrNull()
            val newScore = score.toInt()

            if (existingGame?.highScore == null || newScore > existingGame.highScore!!) {
                dao.updateHighScore(3, newScore)
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

    data class Charade(
        val text: String,
        val answer: String
    )

    override fun onDestroy() {
        super.onDestroy()
        musicPlayer.release()
    }
}