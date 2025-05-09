package com.example.projet_progm
import AppDatabase
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.launch
import java.lang.Thread.sleep


class ActivitySoloMode : ComponentActivity(){
    private lateinit var musicPlayer: MusicPlayer

    private val scores = mutableListOf<Int>()
    private var currentGameIndex = 0

    val gameActToPlay = mutableListOf<Class<out Activity>>()

    private val gameResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val score = result.data?.getIntExtra("score", 0) ?: 0
            scores.add(score)

            // Update the appropriate score TextView here
            val scoreTextViewId = when (currentGameIndex) {
                0 -> R.id.Score1
                1 -> R.id.Score2
                2 -> R.id.Score3
                else -> null
            }
            scoreTextViewId?.let {
                findViewById<TextView>(it).text = "Score: $score"
            }
        } else {
            val score = 0
            scores.add(score)

            // Update the appropriate score TextView here
            val scoreTextViewId = when (currentGameIndex) {
                0 -> R.id.Score1
                1 -> R.id.Score2
                2 -> R.id.Score3
                else -> null
            }
            scoreTextViewId?.let {
                findViewById<TextView>(it).text = "Score: $score"
            }
        }

        currentGameIndex++
        launchNextGameOrShowResult()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.solomodelayout)

        musicPlayer = MusicPlayer(this)
        musicPlayer.playMusic(R.raw.menupage)

        val activityMap = mapOf(
            "FindTheObject" to ActivityFindTheObject::class.java,
            "SearchTheChest" to ActivitySearchTheChest::class.java,
            "EnigmeActivity" to EnigmeActivity::class.java,
            "Player_VS_Enemy" to Player_VS_Enemy::class.java,
            "QuestionnaireGameActivity" to QuestionnaireGameActivity::class.java,
            "MementoActivity" to ActivityMemento::class.java
        )

        lifecycleScope.launch {
            val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "myDatabase"
            ).build()

            val gamesList = db.userDao().getAll()
            val gameNames = mutableListOf<String>()
            val gameActivities = mutableListOf<Class<out Activity>>()

            for (game in gamesList) {
                val name = game.gameName
                val activityKey = game.gameActivity
                if (name != null && activityKey != null) {
                    val activityClass = activityMap[activityKey]
                    if (activityClass != null) {
                        gameNames.add(name)
                        gameActivities.add(activityClass)
                    } else {
                        Log.d("DEBUG", "Unknown activity key: $activityKey")
                    }
                }
            }

            //Log.d("DEBUG", "Fetched games: $gameNames")

            // -- The rest must be INSIDE the coroutine too:
            val gamesToPlay = mutableListOf<String>()

            val indexes = (gameNames.indices).shuffled().take(3)
            indexes.forEach {
                gamesToPlay.add(gameNames[it])
                gameActToPlay.add(gameActivities[it])
            }

            findViewById<TextView>(R.id.Game1).text = "Minigame n°1 : ${gamesToPlay[0]}"
            findViewById<TextView>(R.id.Game2).text = "Minigame n°2 : ${gamesToPlay[1]}"
            findViewById<TextView>(R.id.Game3).text = "Minigame n°3 : ${gamesToPlay[2]}"

            // Start the first game
            Handler(Looper.getMainLooper()).postDelayed({
                launchNextGameOrShowResult()
            }, 5000)

        }
    }

    fun launchNextGameOrShowResult() {
            if (currentGameIndex < gameActToPlay.size) {
                val intent = Intent(this@ActivitySoloMode, gameActToPlay[currentGameIndex])
                Handler(Looper.getMainLooper()).postDelayed({
                    gameResultLauncher.launch(intent)
                }, 2000) // Delay in milliseconds
            } else {
                findViewById<TextView>(R.id.Score3).text = "Score: ${scores.getOrNull(2) ?: 0}"
                findViewById<TextView>(R.id.FinalScore).text = "Final score: ${scores.sum()}"
                findViewById<TextView>(R.id.button).visibility = View.VISIBLE
            }
    }

    fun goToMenu(view: View?) {
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        musicPlayer.release()
    }

    override fun onResume() {
        super.onResume()
        musicPlayer.resumeMusic()
    }

    override fun onPause() {
        super.onPause()
        musicPlayer.pauseMusic()
    }
}