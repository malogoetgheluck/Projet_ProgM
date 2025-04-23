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

class MementoObjects(private val context: Context, private val obj: Int) {

    val id = obj

    var w = 200
    var h = 200

    var posx = 0
    var posy = 0

    val options = BitmapFactory.Options().apply {
        inScaled = false
        inDither = false
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }

    val recto = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.mobj, options), w, h, false)

    val bitmap: Bitmap = when (id) {
        1 -> BitmapFactory.decodeResource(context.resources, R.drawable.mobj10, options)
        2 -> BitmapFactory.decodeResource(context.resources, R.drawable.mobj11, options)
        3 -> BitmapFactory.decodeResource(context.resources, R.drawable.mobj12, options)
        4 -> BitmapFactory.decodeResource(context.resources, R.drawable.mobj13, options)
        5 -> BitmapFactory.decodeResource(context.resources, R.drawable.mobj14, options)
        6 -> BitmapFactory.decodeResource(context.resources, R.drawable.mobj15, options)
        7 -> BitmapFactory.decodeResource(context.resources, R.drawable.mobj16, options)
        else -> BitmapFactory.decodeResource(context.resources, R.drawable.mobj17, options)
    }
    val verso = Bitmap.createScaledBitmap(bitmap, w, h, false)

    var actualSide = "recto"

    var imageView: ImageView? = null

    fun createView(posX: Int, posY: Int): ImageView {
        imageView = ImageView(context).apply {
            layoutParams = RelativeLayout.LayoutParams(w, h).apply {
                leftMargin = context.resources.displayMetrics.widthPixels / 2 + posX - w / 2
                posx = context.resources.displayMetrics.widthPixels / 2 + posX
                topMargin = context.resources.displayMetrics.heightPixels / 2 + posY - w / 2 + 300
                posy = context.resources.displayMetrics.heightPixels / 2 + posY + 300
            }

            setImageBitmap(recto)

            // Ensure sharp pixels by using MATRIX scale type
            scaleType = ImageView.ScaleType.MATRIX
            setLayerType(View.LAYER_TYPE_SOFTWARE, null) // Disable GPU filtering
        }
        return imageView!!
    }

    fun returnCard(){
        if (actualSide=="recto"){
            imageView?.setImageBitmap(verso)
            actualSide = "verso"
        } else {
            imageView?.setImageBitmap(recto)
            actualSide = "recto"
        }
    }

    fun disapear(parentLayout: ViewGroup){
        imageView?.let { leafView ->
            parentLayout.removeView(leafView)
        } ?: Log.e("DEBUG", "imageView is NULL, cannot fade the leaf!")
    }
}