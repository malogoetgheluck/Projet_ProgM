// BluetoothGameManager.kt
package com.example.projet_progm

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import java.io.IOException

object BluetoothGameManager {
    // Constantes pour les messages
    private const val GAME_START_PREFIX = "GAME_START:"
    private const val GAME_END_SIGNAL = "GAME_END"
    private const val MESSAGE_SEPARATOR = "|"

    /**
     * Envoie le signal de début de jeu à l'appareil pair
     */
    fun sendGameStart(socket: BluetoothSocket?, gameClass: Class<*>) {
        Thread {
            try {
                val message = "$GAME_START_PREFIX${gameClass.simpleName}"
                socket?.outputStream?.write(message.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * Envoie le signal de fin de jeu à l'appareil pair
     */
    fun sendGameEnd(socket: BluetoothSocket?) {
        Thread {
            try {
                socket?.outputStream?.write(GAME_END_SIGNAL.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     Traite les messages reçus via Bluetooth
     */
    fun handleIncomingMessage(
        context: Context,
        message: String,
        socket: BluetoothSocket?,
        handler: Handler,
        onGameStart: (Class<*>) -> Unit,
        onGameEnd: () -> Unit
    ) {
        when {
            message.startsWith(GAME_START_PREFIX) -> {
                val gameName = message.removePrefix(GAME_START_PREFIX)
                val gameClass = when (gameName) {
                    "QuestionnaireGameActivity" -> QuestionnaireGameActivity::class.java
                    "EnigmeActivity" -> EnigmeActivity::class.java
                    "SearchTheChest" -> SearchTheChest::class.java
                    "Player_VS_Enemy" -> Player_VS_Enemy::class.java
                    else -> null
                }

                gameClass?.let {
                    handler.post { onGameStart(it) }
                }
            }
            message == GAME_END_SIGNAL -> {
                handler.post { onGameEnd() }
            }
        }
    }
}