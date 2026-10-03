package com.pencil2d.animation.data.model

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 0.5f,
    val timestamp: Long = System.currentTimeMillis()
)

data class Stroke(
    val points: List<StrokePoint>,
    val tool: DrawingTool,
    val color: Int,
    val strokeWidth: Float,
    val opacity: Float = 1.0f
)
