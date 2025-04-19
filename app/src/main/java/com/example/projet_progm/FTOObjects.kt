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

class FTOObjects(private val context: Context, moving: Boolean, temp: Int) {

    var w = 300;
    var h = 300;

    val speed = (5..7).random();

    var pos_x = (1..context.resources.displayMetrics.widthPixels-w).random();
    var pos_y = (1000..context.resources.displayMetrics.heightPixels-h).random();

    var angle = (1..4).random().toFloat()*90-45

    var imageView: ImageView? = null

    val moving = moving
    val sprite = temp

        fun createView(): ImageView {
            imageView = ImageView(context).apply {
                layoutParams = RelativeLayout.LayoutParams(w, h).apply {
                    leftMargin = pos_x
                    topMargin = pos_y
                }

                val options = BitmapFactory.Options().apply {
                    inScaled = false
                    inDither = false
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }

                val bitmap: Bitmap = when (sprite) {
                    1 -> BitmapFactory.decodeResource(context.resources, R.drawable.obj1, options)
                    2 -> BitmapFactory.decodeResource(context.resources, R.drawable.obj2, options)
                    3 -> BitmapFactory.decodeResource(context.resources, R.drawable.obj3, options)
                    4 -> BitmapFactory.decodeResource(context.resources, R.drawable.obj4, options)
                    5 -> BitmapFactory.decodeResource(context.resources, R.drawable.obj5, options)
                    else -> BitmapFactory.decodeResource(context.resources, R.drawable.obj6, options)
                }

                // Create a larger version of the bitmap without blurring
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, w, h, false)

                setImageBitmap(scaledBitmap)

                // Ensure sharp pixels by using MATRIX scale type
                scaleType = ImageView.ScaleType.FIT_XY
                setLayerType(View.LAYER_TYPE_SOFTWARE, null) // Disable GPU filtering

            }
            return imageView!!
        }

    fun setPos(x: Int, y: Int){
        this.pos_x = x - w/2
        this.pos_y = y - w/2
    }

    fun update() {
        //Log.d("DEBUG", (kotlin.math.cos(Math.toRadians(angle.toDouble()))*speed).toInt().toString())
        if (moving) {
            pos_x += (kotlin.math.cos(Math.toRadians(angle.toDouble()))*speed).toInt()
            pos_y += (kotlin.math.sin(Math.toRadians(angle.toDouble()))*speed).toInt()

            if (pos_x<1) {pos_x = context.resources.displayMetrics.widthPixels-w}
            if (pos_x>context.resources.displayMetrics.widthPixels-w) {pos_x = 1}

            if (pos_y<1000) {pos_y = context.resources.displayMetrics.heightPixels-h}
            if (pos_y>context.resources.displayMetrics.heightPixels-h) {pos_y = 1000}

            imageView?.let {
                val params = it.layoutParams as RelativeLayout.LayoutParams
                params.leftMargin = pos_x
                params.topMargin = pos_y
                it.layoutParams = params
            }
        }
    }

    fun disapear(parentLayout: ViewGroup){
        imageView?.let { leafView ->
            parentLayout.removeView(leafView)
        } ?: Log.e("DEBUG", "imageView is NULL, cannot fade the leaf!")
    }
}