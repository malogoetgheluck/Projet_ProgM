package com.example.projet_progm

import Games
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityTrainingMode : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.trainingmodelayout)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Get the database instance and userDao
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.userDao()

        lifecycleScope.launch {
            // Fetch data from the database in a background thread
            try {
                val games = dao.getAll()

                // Check if data is fetched correctly
                Log.d("DEBUG", "Games from DB: $games")

                // If the list is empty, check your query and database
                if (games.isEmpty()) {
                    Log.d("DEBUG", "No games in the database!")
                }

                // Set up the adapter and pass the data to RecyclerView
                val adapter = MyAdapter(games) { game ->
                    val intent = when (game.uid) {
                        1 -> Intent(this@ActivityTrainingMode, ActivitySearchTheChest::class.java)
                        2 -> Intent(this@ActivityTrainingMode, ActivityFindTheObject::class.java)
                        3 -> Intent(this@ActivityTrainingMode, EnigmeActivity::class.java)
                        4 -> Intent(this@ActivityTrainingMode, Player_VS_Enemy::class.java)
                        5 -> Intent(this@ActivityTrainingMode, QuestionnaireGameActivity::class.java)
                        6 -> Intent(this@ActivityTrainingMode, ActivityMemento::class.java)
                        else -> return@MyAdapter
                    }
                    startActivity(intent)
                }

                // Set the adapter for the RecyclerView
                recyclerView.adapter = adapter

            } catch (e: Exception) {
                Log.e("ERROR", "Error fetching games: ${e.message}")
            }
        }
    }
}