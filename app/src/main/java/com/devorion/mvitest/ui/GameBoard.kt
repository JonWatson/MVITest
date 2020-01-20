package com.devorion.mvitest.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.FrameLayout
import com.jakewharton.rxbinding2.view.touches
import io.reactivex.Observable

class GameBoard(
    context: Context,
    attributeSet: AttributeSet
) : FrameLayout(context, attributeSet) {

    val touchObservable: Observable<Point>
    private var relativeSquareRect: Rect? = null
    private var relativeBoardSize = 1
    private val paint = Paint().apply {
        color = Color.DKGRAY
    }

    init {
        setWillNotDraw(false)
        touchObservable = touches().map {
            val boardRatio = width / relativeBoardSize
            Point((it.x / boardRatio).toInt(), (it.y / boardRatio).toInt())
        }
    }

    fun clearSquare() {
        drawSquare(null, 1)
    }

    fun drawSquare(relativeSquareRect: Rect?, relativeBoardSize: Int) {
        this.relativeSquareRect = relativeSquareRect
        this.relativeBoardSize = relativeBoardSize
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.run {
            relativeSquareRect?.let { rect ->
                val boardRatio = width / relativeBoardSize
                val adjustedRect =
                    Rect(
                        rect.left * boardRatio,
                        rect.top * boardRatio,
                        rect.right * boardRatio,
                        rect.bottom * boardRatio
                    )
                canvas.drawRect(adjustedRect, paint)
            }
        }
    }
}
