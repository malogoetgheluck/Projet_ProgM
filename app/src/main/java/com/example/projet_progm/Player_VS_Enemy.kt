package com.example.projet_progm


import android.app.Activity
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.Toast
import kotlin.math.max
import kotlin.math.min

class Player_VS_Enemy : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var player: ImageView
    private lateinit var door: ImageView
    private lateinit var enemy1: ImageView
    private lateinit var enemy2: ImageView
    private lateinit var gift1: ImageView
    private lateinit var gift2: ImageView
    private var lastHitEnemy1 = 0L
    private var lastHitEnemy2 = 0L
    private val hitCooldown = 1000L // 1 seconde en millisecondes

    private var score: Long = 0

    //To quit the game with a little delay
    private val handler = Handler(Looper.getMainLooper())
    private val endGame = object : Runnable {
        override fun run() {
            val resultIntent = Intent()
            resultIntent.putExtra("score", score)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_vs_enemy)

        // Initialiser capteurs
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Récupérer les vues
        player = findViewById(R.id.player)
        door = findViewById(R.id.door)
        enemy1 = findViewById(R.id.enemy1)
        enemy2 = findViewById(R.id.enemy2)
        gift1 = findViewById(R.id.gift1)
        gift2 = findViewById(R.id.gift2)

        moveEnemies()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0] // inclinaison gauche/droite
        val y = event.values[1] // inclinaison avant/arrière
        var posX = 0f
        var posY = 0f
        // Position actuelle du joueur
        if(y>0) {
            posX = player.x - x * 5
            posY = player.y + y * 1
        }else{
            posX = player.x - x * 5
            posY = player.y + y * 10
        }

        // Empêcher de sortir de l'écran
        val maxX = (resources.displayMetrics.widthPixels - player.width).toFloat()
        val maxY = (resources.displayMetrics.heightPixels - player.height).toFloat()

        posX = min(max(posX, 0f), maxX)
        posY = min(max(posY, 0f), maxY)

        player.x = posX
        player.y = posY

        checkCollisions()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun checkCollisions() {
        fun isColliding(a: ImageView, b: ImageView): Boolean {
            return a.x < b.x + b.width && a.x + a.width > b.x &&
                    a.y < b.y + b.height && a.y + a.height > b.y
        }

        val currentTime = System.currentTimeMillis()

        if (isColliding(player, enemy1) && currentTime - lastHitEnemy1 > hitCooldown) {
            score -= 50
            lastHitEnemy1 = currentTime
            Toast.makeText(this, "Touché par un ennemi", Toast.LENGTH_SHORT).show()
            enemy1.visibility = ImageView.INVISIBLE
            enemy1.postDelayed({ enemy1.visibility = ImageView.VISIBLE }, 1000)
        }

        if (isColliding(player, enemy2) && currentTime - lastHitEnemy2 > hitCooldown) {
            score -= 50
            lastHitEnemy2 = currentTime
            Toast.makeText(this, "Touché par un ennemi", Toast.LENGTH_SHORT).show()
            enemy2.visibility = ImageView.INVISIBLE
            enemy2.postDelayed({ enemy2.visibility = ImageView.VISIBLE }, 1000)
        }

        if (isColliding(player, gift1)) {
            score += 100
            gift1.x = -1000f
            gift1.y = -1000f
        }

        if (isColliding(player, gift2)) {
            score += 100
            gift2.x = -1000f
            gift2.y = -1000f
        }

        if (isColliding(player, door)) {
            Toast.makeText(this, "Score final : $score", Toast.LENGTH_LONG).show()
            handler.postDelayed(endGame, 3000)
        }
    }
    private var enemy1Direction = 1
    private var enemy2Direction = 1

    private fun moveEnemies() {
        val speed = 5f // Vitesse de déplacement des ennemis
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()

        // Déplacer enemy1
        enemy1.x += enemy1Direction * speed
        if (enemy1.x + enemy1.width > screenWidth || enemy1.x < 0) {
            enemy1Direction *= -1 // Inverser la direction de enemy1
        }

        // Déplacer enemy2
        enemy2.x += enemy2Direction * speed
        if (enemy2.x + enemy2.width > screenWidth || enemy2.x < 0) {
            enemy2Direction *= -1 // Inverser la direction de enemy2
        }

        // Appeler à nouveau cette fonction après un petit délai pour simuler une boucle de jeu
        enemy1.postDelayed({ moveEnemies() }, 8) // ~60 fps
    }
}
