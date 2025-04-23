package com.yoti.mobile.android.capture.face.demo.debug

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color.TRANSPARENT
import android.graphics.Paint
import android.graphics.Paint.Style.STROKE
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Size
import android.view.View

class Overlay(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var outlines: ArrayList<Outline> = ArrayList()
    private val paint = Paint().apply {
        style = STROKE
        strokeWidth = 5F
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    fun drawBoundingBox(boundingBox: Rect, colour: Int, resolution: Size) {
        outlines.add(
                Outline(
                        mirrorBoundingBox(boundingBox, resolution),
                        Paint(paint).apply { color = colour },
                        resolution
                )
        )
        invalidate()
    }

    fun clearBoundingBox() {
        outlines.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(TRANSPARENT, PorterDuff.Mode.CLEAR)
        outlines.forEach {
            canvas.drawRect(scaleBoundingBox(it.boundingBox, it.resolution), it.paint)
        }
    }

    private fun scaleBoundingBox(boundingBox: RectF, resolution: Size) = boundingBox.apply {
        val scaleX = width.toFloat() / resolution.width
        val scaleY = height.toFloat() / resolution.height

        left *= scaleX
        top *= scaleY
        right *= scaleX
        bottom *= scaleY
    }

    /**
     * Front camera view is mirrored to match direction expected by user. When displaying bounds in
     * picture coordinates we need to mirror them to match display coordinates
     */
    private fun mirrorBoundingBox(boundingBox: Rect, resolution: Size) = RectF(
            (resolution.width - boundingBox.right).toFloat(),
            boundingBox.top.toFloat(),
            (resolution.width - boundingBox.left).toFloat(),
            boundingBox.bottom.toFloat(),

    )

    private data class Outline(
            val boundingBox: RectF,
            val paint: Paint,
            val resolution: Size
    )
}
