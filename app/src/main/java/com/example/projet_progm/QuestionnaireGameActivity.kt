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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuestionnaireGameActivity : ComponentActivity() {
    private lateinit var musicPlayer: MusicPlayer

    private var questions = listOf(
        Question("If I have a troll a gnome and a spider, how many legs do I have ?", listOf("8", "10", "12", "14"), 2),
        Question("A farmer has a rooster. Knowing that a hen can lay an egg every week, how many eggs will the farmer have at the end of a year ?", listOf("52", "55", "0", "1"), 2),
        Question("What does a pyrobarbarian see in a grove ?", listOf("A rabbit", "A peaceful land of joy", "A big bonefire !!!", "Himself"), 2),
        Question("In Medieval Germany, a woman could divorce her husband through a Marital Duel. However, to make it fair, the man had a disadvantage. Which was it ?", listOf("The man needed to always hold a potato in his dominant hand", "The man needed to fight in a hole", "The woman fights the man on a horse", "The man had no weapon whereas the woman had an axe"), 1),
        Question("There once was a great artificer, known through all lands for the majesty of his explosions, like they were millions of fireworks. But one day, the artificer fell sick and decided to go for a last journey with an idea in mind. Why did he go ?", listOf("To see friends", "To clear a dungeon", "To amuse people", "To do his final blow"), 3),
        Question("The extended family of Skullgnawer the goblin is coming to his place and he wants to cook his special spider egg omelet. Considering that the recipe uses 12 eggs, 410g of glowshrooms and one cup of goblin spit for 4 people, how much ingredients will he need to gather for his 537 relatives ?", listOf("6444 eggs, 220,170 kg of glowshrooms and 537 cups of goblin spit", "1611 eggs, 55,042 kg of glowshrooms and 134,25 cups of goblin spit", "1549 eggs, 82,014 kg of glowshrooms and 1 cups of goblin spit", "math makes Skullgnawer skull hurt, he doesn't want to cook anymore"), 1),
        Question("What does a pyrobarbarian see in a grove ?", listOf("A rabbit", "A peaceful land of joy", "A big bonefire !!!", "Himself"), 2),
        Question("What does a pyrobarbarian see in a grove ?", listOf("A rabbit", "A peaceful land of joy", "A big bonefire !!!", "Himself"), 2),
        Question(
            "Which of these weapons is typically used by a knight?",
            listOf("Wand", "Longsword", "Dagger", "Throwing Axe"),
            1
        ),
        Question(
            "What happens when you look a basilisk in the eyes?",
            listOf("You fall asleep", "You become invisible", "You turn to stone", "You learn its secrets"),
            2
        ),
        Question(
            "Which creature is known for hoarding treasure?",
            listOf("Goblin", "Dragon", "Griffin", "Troll"),
            1
        ),
        Question(
            "A necromancer has 6 skeletons. He raises 3 more. One falls apart. How many remain?",
            listOf("8", "9", "6", "7"),
            0
        ),
        Question(
            "Which of these creatures can fly?",
            listOf("Ogre", "Wyvern", "Giant", "Troll"),
            1
        ),
        Question(
            "What does an elf usually prefer to eat?",
            listOf("Roast boar", "Mushroom stew", "Raw fish", "Fire-roasted bat"),
            1
        ),
        Question(
            "Which race is most likely to live underground?",
            listOf("Elf", "Dwarf", "Human", "Centaur"),
            1
        ),
        Question(
            "What does a mimic usually disguise itself as?",
            listOf("Sword", "Wizard", "Treasure chest", "Door"),
            2
        ),
        Question(
            "What metal is famously effective against werewolves?",
            listOf("Gold", "Silver", "Iron", "Bronze"),
            1
        ),
        Question(
            "Which spell would help you see in the dark?",
            listOf("Fireball", "Invisibility", "Darkvision", "Silence"),
            2
        ),
        Question(
            "How many horns does a typical unicorn have?",
            listOf("None", "One", "Two", "It depends on the region"),
            1
        ),
        Question(
            "Which creature is known for solving riddles before allowing passage?",
            listOf("Dragon", "Sphinx", "Cyclops", "Orc"),
            1
        ),
        Question(
            "If a wizard casts a spell every full moon, how many spells does he cast in a year?",
            listOf("12", "13", "24", "52"),
            1
        ),
        Question(
            "Which of these creatures can breathe underwater?",
            listOf("Goblin", "Mermaid", "Giant", "Griffin"),
            1
        ),
        Question(
            "Which of these professions is LEAST likely to wield a sword?",
            listOf("Paladin", "Bard", "Wizard", "Knight"),
            2
        ),
        Question(
            "How do you pacify an angry troll?",
            listOf("With a song", "With a riddle", "With food", "With fire"),
            2
        ),
        Question(
            "How many heads does a hydra grow back if you cut off one?",
            listOf("None", "One", "Two", "Three"),
            2
        ),
        Question(
            "Which of the following is NOT a magical artifact?",
            listOf("Cloak of Invisibility", "Amulet of Silence", "Boots of Bananas", "Staff of Fire"),
            2
        ),
        Question(
            "If a ranger tracks 3 goblins, loses 1, and finds 2 more, how many is he tracking now?",
            listOf("3", "4", "5", "6"),
            1
        ),
        Question(
            "Which creature prefers riddles over battle?",
            listOf("Dragon", "Basilisk", "Sphinx", "Minotaur"),
            2
        ),
        Question(
            "What is the traditional way to enter a dwarven stronghold?",
            listOf("Knock three times", "Answer a riddle", "Say 'friend' and enter", "Bribe the guard"),
            2
        )
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
    private val totalTime: Long = 60000 // 60 seconds

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

        musicPlayer = MusicPlayer(this)
        musicPlayer.playMusic(R.raw.minigame)
        musicPlayer.loadSound("success", R.raw.success)
        musicPlayer.loadSound("failure", R.raw.gameover)

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

            musicPlayer.playSound("success")
        } else {
            welldoneTextView.text = "Another time ?"
            score = 0

            musicPlayer.playSound("failure")
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

    override fun onDestroy() {
        super.onDestroy()
        musicPlayer.release()
    }
}