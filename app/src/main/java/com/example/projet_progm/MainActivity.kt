package com.example.projet_progm

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.mainlayout)
    }

    fun goToGame(view: View?) {
        val intent = Intent(this, SearchTheChest::class.java)
        startActivity(intent)
    }
}