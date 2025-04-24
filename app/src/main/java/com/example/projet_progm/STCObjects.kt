package com.example.projet_progm

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout

class STCObjects(private val context: Context) {

    var w = 700;
    var h = 700;

    var angle = (1..4).random().toFloat()*90-45

    private var imageView: ImageView? = null

    fun createView(): ImageView {
        imageView = ImageView(context).apply {
            layoutParams = RelativeLayout.LayoutParams(w, h).apply {
                leftMargin = context.resources.displayMetrics.widthPixels / 2 - w / 2
                topMargin = context.resources.displayMetrics.heightPixels / 2 - w / 2 + 300
            }

            val options = BitmapFactory.Options().apply {
                inScaled = false
                inDither = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bitmap: Bitmap = when ((1..4).random()) {
                1 -> BitmapFactory.decodeResource(context.resources, R.drawable.stcbone, options)
                2 -> BitmapFactory.decodeResource(context.resources, R.drawable.stcclothe, options)
                3 -> BitmapFactory.decodeResource(context.resources, R.drawable.stcwood, options)
                else -> BitmapFactory.decodeResource(context.resources, R.drawable.stcironjunk, options)
            }

            // Create a larger version of the bitmap without blurring
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, w, h, false)

            setImageBitmap(scaledBitmap)

            // Ensure sharp pixels by using MATRIX scale type
            scaleType = ImageView.ScaleType.MATRIX
            setLayerType(View.LAYER_TYPE_SOFTWARE, null) // Disable GPU filtering

            pivotX = w / 2f
            pivotY = h / 2f
            rotation = angle + (-10..10).random()-135

            if (angle == 45f || angle == 225f) {
                rotation = angle + (-10..10).random()+45
            }
        }
        return imageView!!
    }

    fun fade(parentLayout: ViewGroup) {
        //Log.d("DEBUG", angle.toString())

        imageView?.let { leafView ->
            val distance = 300f // Adjust this for how far it should move

            // Create translation animation (moving forward in its rotation direction)
            val translationX = ObjectAnimator.ofFloat(
                leafView,
                View.TRANSLATION_X,
                leafView.translationX + distance * kotlin.math.sin(Math.toRadians((angle+90).toDouble())).toFloat()
            )
            val translationY = ObjectAnimator.ofFloat(
                leafView,
                View.TRANSLATION_Y,
                leafView.translationY + distance * kotlin.math.cos(Math.toRadians((angle+90).toDouble())).toFloat()
            )

            // Create fade-out animation
            val fadeOut = ObjectAnimator.ofFloat(leafView, View.ALPHA, 1f, 0f)

            // Play animations together
            android.animation.AnimatorSet().apply {
                duration = 500 // 1 second
                playTogether(translationX, translationY, fadeOut)
                start()

                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        super.onAnimationEnd(animation)
                        parentLayout.removeView(leafView) // Remove the leaf after animation
                    }
                })
            }
        } ?: Log.e("DEBUG", "imageView is NULL, cannot fade the leaf!")
    }

    fun disapear(parentLayout: ViewGroup){
        imageView?.let { leafView ->
            parentLayout.removeView(leafView)
        } ?: Log.e("DEBUG", "imageView is NULL, cannot fade the leaf!")
    }
}