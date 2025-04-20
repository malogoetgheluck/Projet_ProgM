package com.example.projet_progm
import AppDatabase
import android.app.Activity
import android.content.Intent
import android.os.Bundle
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

    private val scores = mutableListOf<Long>()
    private var currentGameIndex = 0

    val gameActToPlay = mutableListOf<Class<out Activity>>()

    private val gameResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val score = result.data?.getLongExtra("score", 0L) ?: 0L
            Log.d("DEBUG", "Result intent: ${result.data}")
            Log.d("DEBUG", "Extras: ${result.data?.extras}")
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

        val activityMap = mapOf(
            "FindTheObject" to ActivityFindTheObject::class.java,
            "SearchTheChest" to ActivitySearchTheChest::class.java,
            "EnigmeActivity" to ActivitySearchTheChest::class.java,
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

            Log.d("DEBUG", "Fetched games: $gameNames")

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
            launchNextGameOrShowResult()
        }
    }

    fun launchNextGameOrShowResult() {
            if (currentGameIndex < gameActToPlay.size) {
                val intent = Intent(this@ActivitySoloMode, gameActToPlay[currentGameIndex])
                gameResultLauncher.launch(intent)
            } else {
                findViewById<TextView>(R.id.Score3).text = "Score: ${scores.getOrNull(2) ?: 0}"
                findViewById<TextView>(R.id.FinalScore).text = "Final score: ${scores.sum()}"
                findViewById<TextView>(R.id.button).visibility = View.VISIBLE
            }
    }

    fun goToMenu(view: View?) {
        finish()
    }
}