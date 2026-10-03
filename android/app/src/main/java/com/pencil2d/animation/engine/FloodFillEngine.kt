package com.pencil2d.animation.engine

import android.graphics.Bitmap
import android.graphics.Color
import java.util.ArrayDeque
import kotlin.math.abs

object FloodFillEngine {

    /**
     * Efficient queue-based flood fill on Bitmap
     */
    fun floodFill(
        bitmap: Bitmap,
        startX: Int,
        startY: Int,
        fillColor: Int,
        tolerance: Int = 15
    ): Boolean {
        val width = bitmap.width
        val height = bitmap.height

        if (startX !in 0 until width || startY !in 0 until height) return false

        val targetColor = bitmap.getPixel(startX, startY)
        if (colorMatch(targetColor, fillColor, tolerance)) {
            return false // Already filled with this color
        }

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val queue = ArrayDeque<Int>(width * 4)
        val visited = BooleanArray(width * height)

        fun pack(x: Int, y: Int): Int = (y shl 16) or (x and 0xFFFF)
        fun unpackX(pos: Int): Int = (pos and 0xFFFF).toShort().toInt()
        fun unpackY(pos: Int): Int = pos shr 16

        val startIdx = startY * width + startX
        queue.add(pack(startX, startY))
        visited[startIdx] = true

        while (!queue.isEmpty()) {
            val pos = queue.poll()
            val cx = unpackX(pos)
            val cy = unpackY(pos)
            val cIdx = cy * width + cx

            pixels[cIdx] = fillColor

            // Check 4-connected neighbors
            val neighbors = arrayOf(
                cx + 1 to cy,
                cx - 1 to cy,
                cx to cy + 1,
                cx to cy - 1
            )

            for ((nx, ny) in neighbors) {
                if (nx in 0 until width && ny in 0 until height) {
                    val nIdx = ny * width + nx
                    if (!visited[nIdx] && colorMatch(pixels[nIdx], targetColor, tolerance)) {
                        visited[nIdx] = true
                        queue.add(pack(nx, ny))
                    }
                }
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return true
    }

    private fun colorMatch(c1: Int, c2: Int, tolerance: Int): Boolean {
        if (c1 == c2) return true
        val rDiff = abs(Color.red(c1) - Color.red(c2))
        val gDiff = abs(Color.green(c1) - Color.green(c2))
        val bDiff = abs(Color.blue(c1) - Color.blue(c2))
        val aDiff = abs(Color.alpha(c1) - Color.alpha(c2))
        return (rDiff + gDiff + bDiff + aDiff) <= tolerance * 4
    }
}
