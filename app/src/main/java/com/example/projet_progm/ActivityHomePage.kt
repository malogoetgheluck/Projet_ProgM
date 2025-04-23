package com.example.projet_progm

import AppDatabase
import Games
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivityHomePage : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.mainlayout)

        val db = Room.databaseBuilder(
                applicationContext,
                AppDatabase::class.java, "myDatabase"
            ).fallbackToDestructiveMigration(false).build()

        val userDao = db.userDao()

        // Use a coroutine to insert the games
        lifecycleScope.launch(Dispatchers.IO) {
            var newGame = Games(uid = 1, gameName = "Search the chest", gameActivity = "SearchTheChest", highScore = null)
            userDao.insertAll(newGame)

            newGame = Games(uid = 2, gameName = "Find the object", gameActivity = "FindTheObject", highScore = null)
            userDao.insertAll(newGame)

            newGame = Games(uid = 3, gameName = "Riddles", gameActivity = "EnigmeActivity", highScore = null)
            userDao.insertAll(newGame)

            newGame = Games(uid = 4, gameName = "Dodge the enemies", gameActivity = "Player_VS_Enemy", highScore = 0)
            userDao.insertAll(newGame)

            newGame = Games(uid = 5, gameName = "Questions", gameActivity = "QuestionnaireGameActivity", highScore = null)
            userDao.insertAll(newGame)

            newGame = Games(uid = 6, gameName = "Memento", gameActivity = "MementoActivity", highScore = null)
            userDao.insertAll(newGame)

            // Optional: read back users
            val users = userDao.getAll()
            users.forEach {
                //Log.d("DEBUG", "Game: ${it.gameName} ${it.gameActivity}")
            }
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

    fun goToMemento(view: View){
        val intent = Intent(this, ActivityMemento::class.java)
        startActivity(intent)
    }
}