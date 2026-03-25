package com.example.sports

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class RoutePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Point(val latitude: Double, val longitude: Double)

    private val routePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2D8F63")
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D86B2B")
        style = Paint.Style.FILL
    }

    private var points: List<Point> = emptyList()

    fun submitPoints(points: List<Point>) {
        this.points = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) return

        val minLat = points.minOf { it.latitude }
        val maxLat = points.maxOf { it.latitude }
        val minLng = points.minOf { it.longitude }
        val maxLng = points.maxOf { it.longitude }
        val latRange = max(maxLat - minLat, 0.0001)
        val lngRange = max(maxLng - minLng, 0.0001)
        val inset = 28f
        val drawWidth = width - inset * 2
        val drawHeight = height - inset * 2

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = inset + (((point.longitude - minLng) / lngRange) * drawWidth).toFloat()
            val y = inset + (drawHeight - (((point.latitude - minLat) / latRange) * drawHeight)).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            if (index == points.lastIndex) {
                canvas.drawCircle(x, y, 10f, dotPaint)
            }
        }
        canvas.drawPath(path, routePaint)
    }
}
