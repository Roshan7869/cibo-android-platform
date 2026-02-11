package com.cebo.bus.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * A custom view that shows a big Green Check or Red Cross.
 * Designed for low-literacy drivers to understand "Working" vs "Not Working".
 */
class SimpleStatusView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var isWorking: Boolean = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun setStatus(working: Boolean) {
        if (isWorking != working) {
            isWorking = working
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val color = if (isWorking) Color.GREEN else Color.RED
        paint.color = color
        
        val cx = width / 2.0f
        val cy = height / 2.0f
        val radius = Math.min(width, height) / 2.0f * 0.8f
        
        canvas.drawCircle(cx, cy, radius, paint)
        
        // (Pseudocode) Draw Icon (Checkmark or Cross)
        // paint.color = Color.WHITE
        // ... draw icon ...
    }
}
