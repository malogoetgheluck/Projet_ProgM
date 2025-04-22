package com.example.projet_progm

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Thread.sleep

class ActivityFindTheObject : ComponentActivity() {

    private var startX = 0f
    private var startY = 0f

    private var objectList: ArrayList<FTOObjects> = ArrayList()
    private lateinit var objectToFind:FTOObjects
    private lateinit var displayObject:FTOObjects

    private lateinit var scoreTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var welldoneTextView: TextView
    private var countDownTimer: CountDownTimer? = null
    private val totalTime: Long = 30000 // 30 seconds
    private var score: Long = 0
    private var error = 0

    private lateinit var scoreLayout : FrameLayout;

    private var loosed = false

    private val UPDATE_MILLIS: Long = 16L // ~60 FPS
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            // update your logic or UI here
            for (i in 0..objectList.size-1){
                objectList[i].update()
            }

            objectToFind.update()

            //Log.d("DEBUG","UPDATED")

            // schedule the next update
            handler.postDelayed(this, UPDATE_MILLIS)
        }
    }

    //To quit the game with a little delay
    private val endGame = object : Runnable {
        override fun run() {
            val resultIntent = Intent().apply {
                putExtra("score", score)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.searchthechestlayout)

        val parentLayout = findViewById<RelativeLayout>(R.id.parentLayout)

        val objToFind = (1..6).random()

        for (i in 1..6) {

            if (objToFind == i){
                displayObject = FTOObjects(this, false, objToFind)
                displayObject.setPos(resources.displayMetrics.widthPixels/2, 350)
                parentLayout.addView(displayObject.createView())

                objectToFind = FTOObjects(this, true, objToFind)
                parentLayout.addView(objectToFind.createView(),0)
            } else {
                val number = (6..9).random()
                for (j in 0..number){
                    val tempObject = FTOObjects(this, true, i)
                    parentLayout.addView(tempObject.createView())
                    objectList.add(tempObject)
                }
            }
        }

        // Initialize UI elements
        scoreTextView = findViewById(R.id.scoreTextView)
        timerTextView = findViewById(R.id.timerTextView)
        welldoneTextView = findViewById(R.id.welldoneTextView)

        startTimer()

        scoreLayout = findViewById(R.id.scoreLayout)
        scoreLayout.visibility = View.GONE

        // Initialize the gameloop
        handler.postDelayed(runnable, UPDATE_MILLIS)
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

        // Get the object's position relative to the window
        val location = IntArray(2)
        objectToFind.imageView?.getLocationInWindow(location) // Get the position in window coordinates

        val objectX = location[0] + objectToFind.w/2 // Object's X position in window coordinates
        val objectY = location[1] + objectToFind.h/2 // Object's Y position in window coordinates

        // Now compare the touch position to the object’s position
        val distance = Math.sqrt(Math.pow((touchX - objectX).toDouble(), 2.0) + Math.pow((touchY - objectY).toDouble(), 2.0))

        if (distance < tolDist) {
            onGameOver(true) // Object clicked successfully
        } else {
            error += 1 // Increase error count
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun clearList(){
        val parentLayout = findViewById<RelativeLayout>(R.id.parentLayout)
        while (objectList.isNotEmpty()){
            objectList.last().disapear(parentLayout)
            objectList.removeLast()
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun onGameOver(win: Boolean) {
        handler.removeCallbacks(runnable)
        loosed = true
        countDownTimer?.cancel()
        scoreLayout.visibility = View.VISIBLE
        if (win){
            welldoneTextView.text = "Well done"
            clearList()
        } else {
            welldoneTextView.text = "Another time ?"
            clearList()
            score = 0
        }
        scoreTextView.text = "Score: "+score

        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.userDao()

        lifecycleScope.launch(Dispatchers.IO) {
            val existingGame = dao.loadAllByIds(intArrayOf(2)).firstOrNull()
            val newScore = score.toInt()

            if (existingGame?.highScore == null || newScore > existingGame.highScore!!) {
                dao.updateHighScore(2, newScore)
            }
        }

        handler.postDelayed(endGame, 3000)
    }

    private fun startTimer(time: Long = totalTime) {
        countDownTimer = object : CountDownTimer(time, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerTextView.text = "Time: ${millisUntilFinished / 1000}s"
                score = (999 * (millisUntilFinished.toDouble() / totalTime.toDouble())*3/(3+error)).toLong()
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
    }
}