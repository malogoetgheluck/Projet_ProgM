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
import java.lang.Math.toDegrees
import java.lang.Thread.sleep
import kotlin.math.abs
import kotlin.math.atan2

class ActivitySearchTheChest : ComponentActivity() {

    private var startX = 0f
    private var startY = 0f

    private var objectList: ArrayList<STCObjects> = ArrayList()

    private lateinit var scoreTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var welldoneTextView: TextView
    private var countDownTimer: CountDownTimer? = null
    private val totalTime: Long = 30000 // 60 seconds
    private var score: Long = 0

    private lateinit var scoreLayout : FrameLayout;

    private var loosed = false

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
        enableEdgeToEdge()
        setContentView(R.layout.searchthechestlayout)

        val parentLayout = findViewById<RelativeLayout>(R.id.parentLayout)

        // Generate 5 objects dynamically
        for (i in 1..10) {

            val newLeaf = STCObjects(this) // Create a Leaf object
            objectList.add(newLeaf) // Store Leaf object

            //Log.d("AHA", objectList.last().angle.toString())

            val newLeafView: ImageView = newLeaf.createView() // Create ImageView from Leaf
            parentLayout.addView(newLeafView) // Add to screen
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
        //Log.d("DEBUG","Found a touch event")
        if (!loosed){
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Store initial touch position
                    startX = event.x
                    startY = event.y
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    // Get final touch position
                    val endX = event.x
                    val endY = event.y

                    // Calculate swipe direction
                    detectSwipeDirection(startX, startY, endX, endY)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun detectSwipeDirection(startX: Float, startY: Float, endX: Float, endY: Float) {
        val deltaX = endX - startX
        val deltaY = endY - startY
        val angle = toDegrees(atan2(-deltaY, deltaX).toDouble()).toFloat()

        val normalizedAngle = if (angle < 0) angle + 360 else angle
        val tolerance = 15f // Allow slight variation

        //Log.d("DEBUG", "Swipe at ${normalizedAngle.toInt()}° detected!")
        //Log.d("DEBUG",objectList.last().angle.toString())
        //Log.d("DEBUG",(normalizedAngle - objectList.last().angle).toString())

        if (abs(normalizedAngle - objectList.last().angle) <= tolerance){
            val parentLayout = findViewById<RelativeLayout>(R.id.parentLayout)

            objectList.last().fade(parentLayout)
            objectList.removeLast()
            if (objectList.isEmpty()){
                scoreLayout.visibility = View.VISIBLE
                onGameOver(true)
            }
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
        loosed = true
        countDownTimer?.cancel()
        if (win){
            welldoneTextView.text = "Well done"
        } else {
            welldoneTextView.text = "Another time ?"
            clearList()
            score = 0
        }
        scoreTextView.text = "Score: "+score

        Log.d("DEBUG", "The score is $score")

        handler.postDelayed(endGame, 5000)
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
    }
}