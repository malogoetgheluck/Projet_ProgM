package com.example.projet_progm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import java.io.IOException
import java.util.UUID
import android.os.Handler
import android.os.Looper
import android.bluetooth.BluetoothServerSocket
import android.media.MediaPlayer

class ActivityMultiMode : ComponentActivity() {

    companion object {
        private const val MSG_SERVER_SEQUENCE = "SERVER_SEQUENCE:"
        private const val MSG_SEQUENCE_COMPLETE = "SEQUENCE_COMPLETE"
        private const val MSG_REQUEST_FINAL_SCORES = "REQUEST_FINAL_SCORES"
        private const val MSG_FINAL_SCORES = "FINAL_SCORES:"
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var scanButton: Button
    private lateinit var listView: ListView
    private lateinit var startServerButton: Button

    private var bluetoothSocket: BluetoothSocket? = null
    private var isServer: Boolean = false

    private var isWaitingForConnection = false
    private  var serverSocket: BluetoothServerSocket? = null

    private lateinit var connectionRequestDialog: AlertDialog
    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var adapter: ArrayAdapter<String>

    private var isConnectionEstablished = false

    private val REQUEST_ENABLE_BT = 1
    private val APP_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

    private val selectedGames = mutableListOf<Class<*>>()
    private var currentGameIndex = 0
    private var isGameSequenceRunning = false
    private val REQUEST_GAME_ACTIVITY = 1001
    private var serverScore = 0
    private var clientScore = 0
    private var opponentCompleted = false
    private var myCompleted = false
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.multimodelayout)

        scanButton = findViewById(R.id.scan_button)
        listView = findViewById(R.id.listView)
        startServerButton = findViewById(R.id.startServerButton)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        listView.adapter = adapter
        checkPermissions()

        scanButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                startDiscovery()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), 101)
                Toast.makeText(this, "Permission Bluetooth requise", Toast.LENGTH_SHORT).show()
            }
        }
        listView.setOnItemClickListener { _, _, position, _ ->
            val device = deviceList[position]
            connectToDevice(device)
        }
        startServerButton.setOnClickListener {
            startBluetoothServer()
        }
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth non supporté", Toast.LENGTH_LONG).show()
            scanButton.isEnabled = false
        } else if (!bluetoothAdapter.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        } else {
            scanButton.isEnabled = true
        }
        // Enregistrer le receiver pour les réponses de connexion


        startBluetoothMessageListener()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
        } else {
            scanButton.isEnabled = true
        }
    }
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun startDiscovery() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter.cancelDiscovery()
        }

        deviceList.clear()
        adapter.clear()

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        bluetoothAdapter?.startDiscovery()
    }

    private val receiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null && !deviceList.contains(device)) {
                    deviceList.add(device)
                    adapter.add("${device.name ?: "Inconnu"} - ${device.address}")
                    Log.d("Bluetooth", "Appareil trouvé: ${device?.name} - ${device?.address}")
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        if (isWaitingForConnection) {
            Toast.makeText(this, "Veuillez annuler l'attente serveur d'abord", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            try {
                val socket = device.createRfcommSocketToServiceRecord(APP_UUID)

                runOnUiThread {
                    Toast.makeText(this, "Tentative de connexion...", Toast.LENGTH_SHORT).show()
                }

                socket.connect()
                bluetoothSocket = socket

                // Marquer comme client
                isServer = false
                isConnectionEstablished = true

                runOnUiThread {
                    Toast.makeText(this, "Connecté ", Toast.LENGTH_SHORT).show()
                    startBluetoothMessageListener()

                    // Envoyer un accusé de réception
                    sendMessage("HANDSHAKE_CLIENT_READY")
                }

            } catch (e: IOException) {
                Log.e("Bluetooth", "Échec connexion", e)
                runOnUiThread {
                    Toast.makeText(this, "Échec: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
    @SuppressLint("MissingPermission")
    private fun startBluetoothServer() {
        Thread {
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("GameServer", APP_UUID)!!
                isWaitingForConnection = true

                runOnUiThread {
                    Toast.makeText(this, "En attente de connexion...", Toast.LENGTH_SHORT).show()
                }

                bluetoothSocket = serverSocket?.accept()
                isServer = true
                isConnectionEstablished = true

                runOnUiThread {
                    Toast.makeText(this, "Connecté ", Toast.LENGTH_SHORT).show()
                    startBluetoothMessageListener()

                    // Attendre le handshake client
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isConnectionEstablished) {
                            startGameSequence()
                        }
                    }, 2000)
                }

            } catch (e: IOException) {
                Log.e("Bluetooth", "Erreur serveur", e)
                runOnUiThread {
                    Toast.makeText(this, "Erreur serveur", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }


    private fun startGameSequence() {
        if (isServer) {
            startServerSequence()
        }
    }

    private fun startNextGame() {
        if (currentGameIndex < selectedGames.size) {
            val gameClass = selectedGames[currentGameIndex]
            runOnUiThread {
                val intent = Intent(this, gameClass).apply {
                    putExtra("IS_SERVER", isServer)
                    putExtra("GAME_INDEX", currentGameIndex)
                }
                startActivityForResult(intent, REQUEST_GAME_ACTIVITY)
            }
        } else {
            isGameSequenceRunning = false
            runOnUiThread {
                Toast.makeText(this, "Séquence de jeux terminée!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startBluetoothMessageListener() {
        Thread {
            try {
                val reader = bluetoothSocket?.inputStream?.bufferedReader()
                while (bluetoothSocket?.isConnected == true) {
                    val message = reader?.readLine() ?: break
                    Log.d("Bluetooth", "Message reçu complet: $message")
                    runOnUiThread {
                        handleReceivedMessage(message)
                    }
                }
            } catch (e: IOException) {
                Log.e("Bluetooth", "Erreur lecture message", e)
            }
        }.start()
    }

    private fun sendMessage(message: String) {
        if (bluetoothSocket?.isConnected != true) return

        Thread {
            try {
                // Ajouter un caractère de fin de message
                val messageToSend = "$message\n"  // \n comme séparateur
                bluetoothSocket?.outputStream?.write(messageToSend.toByteArray())
                Log.d("Bluetooth", "Message envoyé: ${message.trim()}")
            } catch (e: IOException) {
                Log.e("Bluetooth", "Erreur d'envoi", e)
            }
        }.start()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GAME_ACTIVITY && resultCode == Activity.RESULT_OK) {
            val score = data?.getIntExtra("score", 0) ?: 0

            if (isServer) {
                serverScore += score
            } else {
                clientScore += score }
            sendMessage("SCORE_UPDATE:$score")

            currentGameIndex++

            if (currentGameIndex < selectedGames.size) {
                if (isServer) startNextServerGame() else startNextClientGame()
            } else {
                endGameSequence()
            }
        }
    }

    private fun handleReceivedMessage(message: String) {
        when {
            message.startsWith(MSG_SERVER_SEQUENCE) -> {
                val sequence = message.removePrefix(MSG_SERVER_SEQUENCE)
                startClientSequence(sequence)
            }
            message.startsWith("SCORE_UPDATE:") -> {
                val scoreStr = message.removePrefix("SCORE_UPDATE:")
                try {
                    val score = scoreStr.toInt()
                    if (isServer) {
                        clientScore += score
                    } else {
                        serverScore += score
                    }
                } catch (e: NumberFormatException) {
                    Log.e("Score", "Format de score invalide: $scoreStr")
                }
            }
            message == MSG_SEQUENCE_COMPLETE -> {
                opponentCompleted = true
                runOnUiThread {
                    Toast.makeText(this, "L'autre joueur a terminé", Toast.LENGTH_SHORT).show()
                }
                if (!isServer) {
                    checkBothCompleted()
                }
            }
            message == MSG_REQUEST_FINAL_SCORES -> {
                val myFinalScore = if (isServer) serverScore else clientScore
                sendMessage("$MSG_FINAL_SCORES$myFinalScore")
            }

            message.startsWith(MSG_FINAL_SCORES) -> {
                try {
                    val opponentFinalScore = message.removePrefix(MSG_FINAL_SCORES).toInt()
                    if (isServer) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            sendMessage("$MSG_FINAL_SCORES$serverScore")
                        }, 300)
                    }

                    showFinalScores(opponentFinalScore)

                } catch (e: NumberFormatException) {
                    Log.e("Scores", "Format de score invalide", e)
                }
            }
            message == "CONNECTION_ACCEPTED" -> {
                runOnUiThread {
                    Toast.makeText(this@ActivityMultiMode, "Connecté au serveur!", Toast.LENGTH_SHORT).show()
                    // Le client est maintenant prêt à recevoir les instructions
                    isServer = false
                    isWaitingForConnection = false
                }
            }
        }
    }

    private fun startServerSequence() {
        myCompleted = false
        opponentCompleted = false
        if (isGameSequenceRunning) return

        val allGames = listOf(
            QuestionnaireGameActivity::class.java,
            EnigmeActivity::class.java,
            //ActivitySearchTheChest::class.java,
            Player_VS_Enemy::class.java,
            ActivityMemento::class.java,
            //ActivityFindTheObject::class.java
        ).shuffled().take(3)

        selectedGames.clear()
        selectedGames.addAll(allGames)
        currentGameIndex = 0
        isGameSequenceRunning = true

        val gameSequence = selectedGames.joinToString(",") { it.simpleName }
        sendMessage("$MSG_SERVER_SEQUENCE$gameSequence")

        startNextServerGame()
    }

    private fun startClientSequence(receivedSequence: String) {
        myCompleted = false
        opponentCompleted = false
        if (isGameSequenceRunning) return

        val gameNames = receivedSequence.split(",")

        selectedGames.clear()
        gameNames.forEach { name ->
            when (name) {
                "QuestionnaireGameActivity" -> selectedGames.add(QuestionnaireGameActivity::class.java)
                "EnigmeActivity" -> selectedGames.add(EnigmeActivity::class.java)
                "ActivitySearchTheChest" -> selectedGames.add(ActivitySearchTheChest::class.java)
                "Player_VS_Enemy" -> selectedGames.add(Player_VS_Enemy::class.java)
                "ActivityFindTheObject" -> selectedGames.add(ActivityFindTheObject::class.java)
                "ActivityMemento" -> selectedGames.add(ActivityMemento::class.java)
            }
        }

        currentGameIndex = 0
        isGameSequenceRunning = true

        startNextClientGame()
    }
    private fun startNextServerGame() {
        if (currentGameIndex < selectedGames.size) {
            val gameClass = selectedGames[currentGameIndex]
            runOnUiThread {
                val intent = Intent(this, gameClass).apply {
                    putExtra("IS_SERVER", true)
                    putExtra("GAME_INDEX", currentGameIndex)
                }
                startActivityForResult(intent, REQUEST_GAME_ACTIVITY)
            }
        } else {
            endGameSequence()
        }
    }

    private fun startNextClientGame() {
        if (currentGameIndex < selectedGames.size) {
            val gameClass = selectedGames[currentGameIndex]
            runOnUiThread {
                val intent = Intent(this, gameClass).apply {
                    putExtra("IS_SERVER", false)
                    putExtra("GAME_INDEX", currentGameIndex)
                }
                startActivityForResult(intent, REQUEST_GAME_ACTIVITY)
            }
        } else {
            endGameSequence()
        }
    }
    private fun showFinalScores(opponentScore: Int) {
        runOnUiThread {
            val myScore = if (isServer) serverScore else clientScore
            val myRole = "Vous"
            val opponentRole = "l'autre joueur"
            val gameResult = when {
                myScore > opponentScore -> "win"
                myScore < opponentScore -> "lose"
                else -> "draw"
            }

            playResultMusic(gameResult)
            val resultMessage = buildResultMessage(myScore, opponentScore, myRole, opponentRole)
            AlertDialog.Builder(this)
                .setTitle("Résultats finaux")
                .setMessage(resultMessage)
                .setPositiveButton("OK") { dialog, _ ->
                    stopResultMusic()
                    dialog.dismiss()
                }
                .setOnCancelListener {
                    stopResultMusic()
                }
                .show()
            handler.postDelayed({
                stopResultMusic()
                try {
                    bluetoothSocket?.close()
                    serverSocket?.close()
                } catch (e: IOException) {
                    Log.e("Bluetooth", "Error closing sockets", e)
                }
                redirectToHomePage()
            }, 4000)
        }
    }

    private fun redirectToHomePage() {
        if (!isFinishing && !isDestroyed) {
            val intent = Intent(this, ActivityHomePage::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            finish()
        }
    }
    private fun playResultMusic(result: String) {
        stopResultMusic()

        val musicResId = when(result) {
            "win" -> R.raw.victory_music
            "lose" -> R.raw.defeat_music
            else -> null
        }

        musicResId?.let {
            mediaPlayer = MediaPlayer.create(this, it).apply {
                isLooping = false
                start()
            }
        }
    }

    private fun stopResultMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
    }

    private fun buildResultMessage(myScore: Int, opponentScore: Int, myRole: String, opponentRole: String): String {
        return """
        Scores finaux:
        
        $myRole: $myScore
        $opponentRole: $opponentScore
        
        ${determineWinner(myScore, opponentScore)}
    """.trimIndent()
    }

    private fun determineWinner(myScore: Int, opponentScore: Int): String {
        return when {
            myScore > opponentScore -> "Vous avez gagné !"
            myScore < opponentScore -> "Vous avez perdu."
            else -> "Égalité !"
        }
    }
    private fun endGameSequence() {
        isGameSequenceRunning = false
        myCompleted = true
        sendMessage(MSG_SEQUENCE_COMPLETE)
        if (isServer) {
            checkBothCompleted()
        }
    }
    private fun checkBothCompleted() {
        if (myCompleted && opponentCompleted) {
            if (isServer) {
                sendMessage(MSG_REQUEST_FINAL_SCORES)
            }
            myCompleted = false
            opponentCompleted = false
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        stopResultMusic()
        try {
            bluetoothSocket?.close()
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Error closing sockets", e)
        }
    }

}