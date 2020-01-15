package com.devorion.mvitest.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.FrameLayout

class GameBoard(
    context: Context,
    attributeSet: AttributeSet
) : FrameLayout(context, attributeSet) {

    private var squareRect: Rect? = null
    private val paint = Paint().apply {
        color = Color.DKGRAY
    }

    init {
        setWillNotDraw(false)
    }

    fun clearSquare() {
        drawSquare(null)
    }

    fun drawSquare(rect: Rect?) {
        squareRect = rect
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.run {
            squareRect?.let {
                canvas.drawRect(it, paint)
            }
        }
    }
}
