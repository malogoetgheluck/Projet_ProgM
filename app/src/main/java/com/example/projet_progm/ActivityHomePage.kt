package com.example.projet_progm

import AppDatabase
import Games
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Switch
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivityHomePage : ComponentActivity() {
    private lateinit var musicPlayer: MusicPlayer
    private lateinit var parameterLayout: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.mainlayout)

        parameterLayout = findViewById(R.id.parameterLayout)

        musicPlayer = MusicPlayer(this)
        musicPlayer.playMusic(R.raw.homepage)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "myDatabase"
        ).fallbackToDestructiveMigration(false).build()

        val userDao = db.userDao()

        // Use a coroutine to insert the games
        lifecycleScope.launch(Dispatchers.IO) {
            var newGame = Games(
                uid = 1,
                gameName = "Remove the weapon",
                gameActivity = "SearchTheChest",
                highScore = null
            )
            userDao.insertAll(newGame)

            newGame = Games(
                uid = 2,
                gameName = "Find the object",
                gameActivity = "FindTheObject",
                highScore = null
            )
            userDao.insertAll(newGame)

            newGame = Games(
                uid = 3,
                gameName = "Riddles",
                gameActivity = "EnigmeActivity",
                highScore = null
            )
            userDao.insertAll(newGame)

            newGame = Games(
                uid = 4,
                gameName = "Dodge the enemies",
                gameActivity = "Player_VS_Enemy",
                highScore = null
            )
            userDao.insertAll(newGame)

            newGame = Games(
                uid = 5,
                gameName = "Questions",
                gameActivity = "QuestionnaireGameActivity",
                highScore = null
            )
            userDao.insertAll(newGame)

            newGame = Games(
                uid = 6,
                gameName = "Memento",
                gameActivity = "MementoActivity",
                highScore = null
            )
            userDao.insertAll(newGame)

            // Optional: read back users
            val users = userDao.getAll()
            users.forEach {
                //Log.d("DEBUG", "Game: ${it.gameName} ${it.gameActivity}")
            }
        }

        //Parameters
        val musicSwitch = findViewById<Switch>(R.id.musicSwitch)
        val soundSwitch = findViewById<Switch>(R.id.soundSwitch)
        val sharedPref = getSharedPreferences("BTDPrefs", MODE_PRIVATE)

        musicSwitch.isChecked = sharedPref.getBoolean("music_enabled", true)
        soundSwitch.isChecked = sharedPref.getBoolean("sound_enabled", true)

        musicSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("music_enabled", isChecked).apply()
            musicPlayer.pauseMusic()
            musicPlayer.resumeMusic()
        }

        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("sound_enabled", isChecked).apply()
        }
    }

    fun goToSoloGame(view: View?) {
        val intent = Intent(this, ActivitySoloMode::class.java)
        startActivity(intent)
    }

    fun goToTrainingGame(view: View?) {
        val intent = Intent(this, ActivityTrainingMode::class.java)
        startActivity(intent)
    }

    fun goToMultiMode(view: View?) {
        val intent = Intent(this, ActivityMultiMode::class.java)
        startActivity(intent)
    }

    fun openParameters(view: View) {
        if (parameterLayout.visibility == View.VISIBLE) {
            parameterLayout.visibility = View.GONE
        } else {
            parameterLayout.visibility = View.VISIBLE
        }
    }

    fun closeParameters(view: View) {
        parameterLayout.visibility = View.GONE
    }

    fun leaveGame(view: View) {
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

    fun resetHighscores(view: View) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.userDao()
            for (i in 1..6) {
                val currentScore = dao.getHighScore(i)
                if (currentScore != null) {
                    dao.updateHighScore(i, 0)
                }
            }
        }
    }
}
