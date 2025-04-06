package com.example.projet_progm

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.main_layout)
        val playButton = findViewById<Button>(R.id.playButton)
        playButton.setOnClickListener {
            // Choisir un jeu aléatoirement
            val randomGame = listOf(
                QuestionnaireGameActivity::class.java,
                EnigmeActivity::class.java,
                SearchTheChest::class.java,
                Player_VS_Enemy::class.java
            ).random()
            startActivity(Intent(this, randomGame))
        }
    }
}


