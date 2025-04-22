package com.example.projet_progm

import android.Manifest
import android.annotation.SuppressLint
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

class ActivityMultiMode : ComponentActivity() {

    companion object {
        private const val MSG_START_GAME = "START_GAME:"
        private const val MSG_GAME_END = "GAME_END"
        private const val MSG_REQUEST_SYNC = "REQUEST_SYNC"
        private const val MSG_SYNC_RESPONSE = "SYNC_RESPONSE:"
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private lateinit var scanButton: Button
    private lateinit var listView: ListView
    private lateinit var startServerButton: Button

    private var bluetoothSocket: BluetoothSocket? = null
    private var isServer: Boolean = false

    private var isWaitingForConnection = false
    private lateinit var serverSocket: BluetoothServerSocket

    private lateinit var connectionRequestDialog: AlertDialog
    private var pendingDevice: BluetoothDevice? = null
    private val connectionRequestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                "ACCEPT_CONNECTION" -> acceptConnection()
                "REJECT_CONNECTION" -> rejectConnection()
            }
        }
    }
    private val connectionResponseReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra("status")) {
                "ACCEPTED" -> {
                    Toast.makeText(this@ActivityMultiMode, "Connexion acceptée!", Toast.LENGTH_SHORT).show()
                    startBluetoothMessageListener()
                }
                "REJECTED" -> {
                    Toast.makeText(this@ActivityMultiMode, "Connexion refusée", Toast.LENGTH_SHORT).show()
                    bluetoothSocket?.close()
                }
            }
        }
    }

    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var adapter: ArrayAdapter<String>

    private var isConnectionEstablished = false
    private val connectionLock = Object()

    private val REQUEST_ENABLE_BT = 1
    private val APP_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

    private val handler = Handler(Looper.getMainLooper())
    private val selectedGames = mutableListOf<Class<*>>()
    private var currentGameIndex = 0
    private var isGameSequenceRunning = false
    private val REQUEST_GAME_ACTIVITY = 1001
    private var first_game=0

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
        val filter1 = IntentFilter().apply {
            addAction("ACCEPT_CONNECTION")
            addAction("REJECT_CONNECTION")
        }
        registerReceiver(connectionRequestReceiver, filter1)

        val filter2 = IntentFilter().apply {
            addAction("CONNECTION_RESPONSE")
        }
        registerReceiver(connectionResponseReceiver, filter2)

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

                bluetoothSocket = serverSocket.accept()
                isServer = true
                isConnectionEstablished = true

                runOnUiThread {
                    Toast.makeText(this, "Client connecté ", Toast.LENGTH_SHORT).show()
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
    private fun acceptConnection() {
        isServer = true
        isConnectionEstablished = true

        runOnUiThread {
            Toast.makeText(this, "Connexion acceptée avec ${bluetoothSocket?.remoteDevice?.name}", Toast.LENGTH_SHORT).show()
            connectionRequestDialog.dismiss()

            // Démarrer l'écoute des messages IMMÉDIATEMENT
            startBluetoothMessageListener()

            // Envoyer confirmation en 2 étapes
            sendMessage("CONNECTION_ACCEPTED:SERVER_READY")

            // Attendre 2 secondes avant de démarrer
            Handler(Looper.getMainLooper()).postDelayed({
                if (isConnectionEstablished) {
                    startGameSequence()
                }
            }, 2000)
        }
    }
    private fun rejectConnection() {
        try {
            bluetoothSocket?.close()
            runOnUiThread {
                Toast.makeText(this, "Connexion refusée", Toast.LENGTH_SHORT).show()
                if (::connectionRequestDialog.isInitialized && connectionRequestDialog.isShowing) {
                    connectionRequestDialog.dismiss()
                }
            }
        } catch (e: IOException) {
            Log.e("Bluetooth", "Error closing socket", e)
        }
    }

    /**
     * Sélectionne 3 jeux aléatoires et démarre la séquence
     */
    private fun startGameSequence() {
        if (isGameSequenceRunning) return

        // Si c'est le serveur, choisir les jeux et informer le client
        if (isServer) {
            val allGames = listOf(
                QuestionnaireGameActivity::class.java,
                EnigmeActivity::class.java,
                SearchTheChest::class.java,
                Player_VS_Enemy::class.java
            ).shuffled().take(3)

            selectedGames.clear()
            selectedGames.addAll(allGames)
            currentGameIndex = 0
            isGameSequenceRunning = true

            // Envoyer la séquence au client
            val gameSequence = selectedGames.joinToString(",") { it.simpleName }
            sendMessage("$MSG_START_GAME$gameSequence")

            startNextGame()
        } else {
            // Si c'est le client, demander la synchronisation
            sendMessage(MSG_REQUEST_SYNC)
        }
    }

    /**
     * Lance le prochain jeu dans la séquence
     */
    private fun startNextGame() {
        if (currentGameIndex < selectedGames.size) {
            val gameClass = selectedGames[currentGameIndex]

            runOnUiThread {
                val intent = Intent(this, gameClass).apply {
                    putExtra("IS_SERVER", isServer)
                }
                startActivityForResult(intent, REQUEST_GAME_ACTIVITY)
            }
        }
    }

    private fun startBluetoothMessageListener() {
        Thread {
            try {
                val inputStream = bluetoothSocket?.inputStream
                val buffer = ByteArray(1024)
                while (bluetoothSocket != null && bluetoothSocket!!.isConnected) {
                    val bytes = inputStream?.read(buffer)
                    val message = String(buffer, 0, bytes ?: 0)
                    Log.d("Bluetooth", "Message reçu : $message")
                    handleReceivedMessage(message)
                }
            } catch (e: IOException) {
                Log.e("Bluetooth", "Erreur lecture message", e)
            }
        }.start()
    }

    private fun sendMessage(message: String) {
        Thread {
            try {
                bluetoothSocket?.outputStream?.write(message.toByteArray())
            } catch (e: IOException) {
                Log.e("Bluetooth", "Erreur d'envoi", e)
            }
        }.start()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_GAME_ACTIVITY) {
            // Envoyer le signal de fin de jeu
            sendMessage(MSG_GAME_END)
            BluetoothGameManager.sendGameEnd(bluetoothSocket)
            Log.e("END_GAME", "FIN 11 ")
            // Passer au jeu suivant si c'est le serveur
                currentGameIndex++
            var i = 0
            if (currentGameIndex < selectedGames.size) {
                i=i+1

                startNextGame()
            } else {
                isGameSequenceRunning = false
                Toast.makeText(this, "Séquence terminée!", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun handleReceivedMessage(message: String) {
        when {
            message.startsWith(MSG_START_GAME) -> {
                // Le serveur envoie la séquence de jeux
                val gamesStr = message.removePrefix(MSG_START_GAME)
                val gameNames = gamesStr.split(",")

                selectedGames.clear()
                gameNames.forEach { name ->
                    when (name) {
                        "QuestionnaireGameActivity" -> selectedGames.add(QuestionnaireGameActivity::class.java)
                        "EnigmeActivity" -> selectedGames.add(EnigmeActivity::class.java)
                        "SearchTheChest" -> selectedGames.add(SearchTheChest::class.java)
                        "Player_VS_Enemy" -> selectedGames.add(Player_VS_Enemy::class.java)
                    }
                }

                currentGameIndex = 0
                isGameSequenceRunning = true

                if (!isServer && first_game==0) {
                    first_game++
                    startNextGame()
                }
            }
            message == "CONNECTION_ACCEPTED:SERVER_READY" -> {
                runOnUiThread {
                    isServer = false
                    isConnectionEstablished = true
                    Toast.makeText(this, "Synchronisé avec le serveur", Toast.LENGTH_SHORT).show()
                    sendMessage("CLIENT_READY") // Nouveau message de confirmation
                }
            }
            message == MSG_REQUEST_SYNC -> {
                // Le client demande la synchronisation
                if (isServer && isGameSequenceRunning) {
                    val gameSequence = selectedGames.joinToString(",") { it.simpleName }
                    sendMessage("$MSG_SYNC_RESPONSE$gameSequence:$currentGameIndex")
                }
            }
            message.startsWith(MSG_SYNC_RESPONSE) -> {
                // Réponse du serveur avec l'état actuel
                val parts = message.removePrefix(MSG_SYNC_RESPONSE).split(":")
                if (parts.size == 2) {
                    val gameNames = parts[0].split(",")
                    val serverIndex = parts[1].toInt()

                    selectedGames.clear()
                    gameNames.forEach { name ->
                        when (name) {
                            "QuestionnaireGameActivity" -> selectedGames.add(QuestionnaireGameActivity::class.java)
                            "EnigmeActivity" -> selectedGames.add(EnigmeActivity::class.java)
                            "SearchTheChest" -> selectedGames.add(SearchTheChest::class.java)
                            "Player_VS_Enemy" -> selectedGames.add(Player_VS_Enemy::class.java)
                        }
                    }

                    currentGameIndex = serverIndex
                    isGameSequenceRunning = true

                    if (currentGameIndex < selectedGames.size) {
                        startNextGame()
                    }
                }
            }
            message == MSG_GAME_END -> {
                // Passer au jeu suivant si c'est le serveur

                    currentGameIndex++
                    if (currentGameIndex < selectedGames.size) {
                        startNextGame()
                    } else {
                        isGameSequenceRunning = false
                        runOnUiThread {
                            Toast.makeText(this, "Tous les jeux sont terminés!", Toast.LENGTH_SHORT).show()
                        }
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
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(connectionResponseReceiver)
        try {
            bluetoothSocket?.close()
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Error closing sockets", e)
        }
    }

}