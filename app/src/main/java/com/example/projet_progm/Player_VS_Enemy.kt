package com.example.projet_progm


import android.app.Activity
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class Player_VS_Enemy : AppCompatActivity(), SensorEventListener {
    private lateinit var musicPlayer: MusicPlayer

    private lateinit var scoreTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var welldoneTextView: TextView
    private var countDownTimer: CountDownTimer? = null
    private val totalTime: Long = 30000 // 30 seconds

    private lateinit var scoreLayout : FrameLayout;

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var player: ImageView
    private lateinit var door: ImageView
    private lateinit var enemy1: ImageView
    private lateinit var enemy2: ImageView
    private lateinit var gift1: ImageView
    private lateinit var gift2: ImageView
    private lateinit var gift3: ImageView
    private var lastHitEnemy1 = 0L
    private var lastHitEnemy2 = 0L
    private val hitCooldown = 1000L // 1 seconde en millisecondes

    private var score: Long = 0
    private var multiplicateurScore: Double = 0.0

    private var enemy1Direction = 0
    private var enemy2Direction = 0

    private var enemy1Positions = listOf(0,0)
    private var enemy2Positions = listOf(0,0)

    private var gameEnded = false

    //To quit the game with a little delay
    private val handler = Handler(Looper.getMainLooper())
    private val endGame = object : Runnable {
        override fun run() {
            val resultIntent = Intent()
            resultIntent.putExtra("score", score.toInt())
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    private val runnable = object : Runnable {
        override fun run() {
            moveEnemies()

            // schedule the next update
            handler.postDelayed(this, 8)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_vs_enemy)

        musicPlayer = MusicPlayer(this)
        musicPlayer.playMusic(R.raw.minigame)
        musicPlayer.loadSound("success", R.raw.success)
        musicPlayer.loadSound("failure", R.raw.gameover)

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
        gift3 = findViewById(R.id.gift3)

        enemy1.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                enemy1.viewTreeObserver.removeOnGlobalLayoutListener(this)

                gift1.x = (100..resources.displayMetrics.widthPixels - enemy1.width - 100).random()
                    .toFloat()
                gift1.y =
                    (1000..resources.displayMetrics.heightPixels - enemy1.height - 100).random()
                        .toFloat()
                gift2.x = (100..resources.displayMetrics.widthPixels - enemy1.width - 100).random()
                    .toFloat()
                gift2.y =
                    (1000..resources.displayMetrics.heightPixels - enemy1.height - 100).random()
                        .toFloat()
                gift3.x = (100..resources.displayMetrics.widthPixels - enemy1.width - 100).random()
                    .toFloat()
                gift3.y =
                    (1000..resources.displayMetrics.heightPixels - enemy1.height - 100).random()
                        .toFloat()

                enemy1Positions = listOf(
                    (100..resources.displayMetrics.widthPixels - enemy1.width - 100).random(),
                    (1000..resources.displayMetrics.heightPixels - enemy1.height - 200).random()
                )

                enemy1Direction = listOf(45, 135, 225, 315).random()

                enemy1.x = enemy1Positions[0].toFloat()
                enemy1.y = enemy1Positions[1].toFloat()

                // Same for enemy2
                enemy2Positions = listOf(
                    (100..resources.displayMetrics.widthPixels - enemy2.width - 100).random(),
                    (1000..resources.displayMetrics.heightPixels - enemy2.height - 200).random()
                )

                enemy2Direction = listOf(45, 135, 225, 315).random()

                enemy2.x = enemy2Positions[0].toFloat()
                enemy2.y = enemy2Positions[1].toFloat()

                handler.postDelayed(runnable, 8)
            }
        })

        // Initialize UI elements
        scoreTextView = findViewById(R.id.scoreTextView)
        timerTextView = findViewById(R.id.timerTextView)
        welldoneTextView = findViewById(R.id.welldoneTextView)

        startTimer()

        scoreLayout = findViewById(R.id.scoreLayout)
        scoreLayout.visibility = View.GONE
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
        if (gameEnded){return}
        val x = event.values[0] // inclinaison gauche/droite
        val y = event.values[1] // inclinaison avant/arrière
        var posX = 0f
        var posY = 0f
        // Position actuelle du joueur
        if(y>0) {
            posX = player.x - x * 5
            posY = player.y + y * 10
        }else{
            posX = player.x - x * 5
            posY = player.y + y * 10
        }

        // Empêcher de sortir de l'écran
        val maxX = (resources.displayMetrics.widthPixels - player.width).toFloat()
        val maxY = (resources.displayMetrics.heightPixels - player.height).toFloat()
        val minY = 1000.toFloat()

        posX = min(max(posX, 0f), maxX)
        posY = min(max(posY, minY), maxY)

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
            score += 333
            gift1.x = -1000f
            gift1.y = -1000f
        }

        if (isColliding(player, gift2)) {
            score += 333
            gift2.x = -1000f
            gift2.y = -1000f
        }

        if (isColliding(player, gift3)) {
            score += 333
            gift3.x = -1000f
            gift3.y = -1000f
        }

        if (isColliding(player, door)) {
            onGameOver(true)
        }
    }

    private fun moveEnemies() {
        val speed = 5f

        // Update positions
        enemy1.x += (Math.cos(Math.toRadians(enemy1Direction.toDouble())) * speed).toFloat()
        enemy1.y += (Math.sin(Math.toRadians(enemy1Direction.toDouble())) * speed).toFloat()

        enemy2.x += (Math.cos(Math.toRadians(enemy2Direction.toDouble())) * speed).toFloat()
        enemy2.y += (Math.sin(Math.toRadians(enemy2Direction.toDouble())) * speed).toFloat()

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        // Bounce logic for enemy1
        if (enemy1.x < 0 || enemy1.x > screenWidth - enemy1.width) {
            enemy1Direction = (180 - enemy1Direction) % 360
        }
        if (enemy1.y < 1000 || enemy1.y > screenHeight - enemy1.height) {
            enemy1Direction = (360 - enemy1Direction) % 360
        }

        // Bounce logic for enemy2
        if (enemy2.x < 0 || enemy2.x > screenWidth - enemy2.width) {
            enemy2Direction = (180 - enemy2Direction) % 360
        }
        if (enemy2.y < 1000 || enemy2.y > screenHeight - enemy2.height) {
            enemy2Direction = (360 - enemy2Direction) % 360
        }

        //Log.d("AHA", enemy1.x.toString())
        //Log.d("AHA", enemy1.y.toString())
    }

    private fun startTimer(time: Long = totalTime) {
        countDownTimer = object : CountDownTimer(time, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTextView.text = "Time: ${millisUntilFinished / 1000}s"
                multiplicateurScore =  millisUntilFinished.toDouble()/totalTime.toDouble()
            }

            @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
            override fun onFinish() {
                timerTextView.text = "Time's up!"
                scoreLayout.visibility = View.VISIBLE
                onGameOver(false)
            }
        }.start()
    }

    fun onGameOver(win: Boolean){
        gameEnded = true

        countDownTimer?.cancel()

        scoreLayout.visibility = View.VISIBLE

        if (win){
            //Log.d("DEBUG",multiplierScore.toString())
            welldoneTextView.text = "Well done"
            score = max((score * multiplicateurScore).toLong(),0L)

            musicPlayer.playSound("success")
        } else {
            welldoneTextView.text = "Another time ?"
            score = 0

            musicPlayer.playSound("failure")
        }
        scoreTextView.text = "Score: "+score

        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.userDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val existingGame = dao.loadAllByIds(intArrayOf(4)).firstOrNull()
            val newScore = score.toInt()

            if (existingGame?.highScore == null || newScore > existingGame.highScore!!) {
                dao.updateHighScore(4, newScore)
            }
        }
        handler.removeCallbacks(runnable)
        handler.postDelayed(endGame, 3000)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
        musicPlayer.release()
    }
}
