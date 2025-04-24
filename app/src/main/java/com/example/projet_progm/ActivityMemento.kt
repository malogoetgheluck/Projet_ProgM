package com.example.projet_progm

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivityMemento : ComponentActivity() {
    private lateinit var musicPlayer: MusicPlayer

    private var objectList: ArrayList<MementoObjects> = ArrayList()

    private lateinit var scoreTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var welldoneTextView: TextView
    private var countDownTimer: CountDownTimer? = null
    private val totalTime: Long = 45000 // 45 seconds
    private var score: Long = 0

    private lateinit var scoreLayout : FrameLayout;

    private var loosed = false

    val numberOfObjects = 8

    private var cardReturned: ArrayList<MementoObjects> = ArrayList()

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.mementolayout)

        musicPlayer = MusicPlayer(this)
        musicPlayer.playMusic(R.raw.minigame)
        musicPlayer.loadSound("success", R.raw.success)
        musicPlayer.loadSound("failure", R.raw.gameover)

        val parentLayout = findViewById<RelativeLayout>(R.id.parentLayout)

        // Generate 5 objects dynamically
        for (id in 1..numberOfObjects) {

            var Mobj = MementoObjects(this, id) // Create a object
            objectList.add(Mobj) // Store object

            Mobj = MementoObjects(this, id)
            objectList.add(Mobj)


        }

        objectList.shuffle()

        var counter = 0;

        for (i in listOf(-3,-1,1,3)){
            for (j in listOf(-3,-1,1,3)){
                val obectView: ImageView = objectList[counter].createView(i*130, j*130) // Create ImageView from Leaf
                parentLayout.addView(obectView) // Add to screen
                counter ++
            }
        }



        // Initialize UI elements
        scoreTextView = findViewById(R.id.scoreTextView)
        timerTextView = findViewById(R.id.timerTextView)
        welldoneTextView = findViewById(R.id.welldoneTextView)

        startTimer()

        scoreLayout = findViewById(R.id.scoreLayout)
        scoreLayout.visibility = View.GONE
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!loosed) {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Get touch coordinates relative to the layout
                    val layout = findViewById<RelativeLayout>(R.id.parentLayout) // Your parent layout
                    val location = IntArray(2)
                    layout.getLocationInWindow(location)
                    val layoutX = location[0]
                    val layoutY = location[1]

                    val touchX = event.x.toInt() + layoutX // Convert to layout-relative
                    val touchY = event.y.toInt() + layoutY // Convert to layout-relative

                    onClickDetected(touchX, touchY) // Pass these layout-relative touch coordinates
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }


    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun onClickDetected(touchX: Int, touchY: Int) {
        val tolDist = 100

        for (obj in objectList){
            // Get the object's position relative to the window
            val location = IntArray(2)
            obj.imageView?.getLocationInWindow(location) // Get the position in window coordinates

            val objectX = location[0] + obj.w/2 // Object's X position in window coordinates
            val objectY = location[1] + obj.h/2 // Object's Y position in window coordinates

            // Now compare the touch position to the object’s position
            val distance = Math.sqrt(Math.pow((touchX - objectX).toDouble(), 2.0) + Math.pow((touchY - objectY).toDouble(), 2.0))

            if (distance < tolDist) {
                obj.returnCard()

                if (cardReturned.size == 0){
                    cardReturned.add(obj)
                } else {
                    if (cardReturned[0] == obj){
                        cardReturned.removeLast()
                    } else {
                        cardReturned.add(obj)
                        if (cardReturned[0].id == cardReturned[1].id){
                            val parentLayout = findViewById<RelativeLayout>(R.id.parentLayout)

                            val first = cardReturned[0]
                            val second = cardReturned[1]

                            Handler(Looper.getMainLooper()).postDelayed({
                                first.disapear(parentLayout)
                                second.disapear(parentLayout)
                            }, 500)

                            objectList.remove(cardReturned[0])
                            objectList.remove(cardReturned[1])

                            if (objectList.isEmpty()){
                                onGameOver(true)
                            }

                            cardReturned.removeLast()
                            cardReturned.removeLast()

                        } else {
                            val first = cardReturned[0]
                            val second = cardReturned[1]

                            Handler(Looper.getMainLooper()).postDelayed({
                                first.returnCard()
                                second.returnCard()
                            }, 500) // Delay in milliseconds

                            cardReturned.removeLast()
                            cardReturned.removeLast()
                        }
                    }
                }
                break
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun onGameOver(win: Boolean) {
        loosed = true
        countDownTimer?.cancel()
        scoreLayout.visibility = View.VISIBLE
        if (win){
            welldoneTextView.text = "Well done"

            musicPlayer.playSound("success")
        } else {
            welldoneTextView.text = "Another time ?"
            score = 0

            musicPlayer.playSound("failure")
        }
        scoreTextView.text = "Score: "+score

        Log.d("DEBUG", "The score is $score")

        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.userDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val existingGame = dao.loadAllByIds(intArrayOf(6)).firstOrNull()
            val newScore = score.toInt()

            if (existingGame?.highScore == null || newScore > existingGame.highScore!!) {
                dao.updateHighScore(6, newScore)
            }
        }

        handler.postDelayed(endGame, 3000)
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(totalTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTextView.text = "Time: ${millisUntilFinished / 1000}s"
                score = (999*(millisUntilFinished.toDouble()/totalTime.toDouble())).toLong()
                //Log.d("DEBUG", (millisUntilFinished.toDouble()/totalTime.toDouble()).toString())
            }

            @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
            override fun onFinish() {
                timerTextView.text = "Time's up!"
                scoreLayout.visibility = View.VISIBLE
                onGameOver(false)
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        musicPlayer.release()
    }
}