package com.pencil2d.animation.engine

import android.graphics.Path
import com.pencil2d.animation.data.model.StrokePoint

object SmoothPathInterpolator {

    /**
     * Builds a smooth Android Path from a list of StrokePoints using Quadratic Bezier interpolation
     */
    fun buildSmoothPath(points: List<StrokePoint>): Path {
        val path = Path()
        if (points.isEmpty()) return path

        if (points.size == 1) {
            val p = points[0]
            path.moveTo(p.x, p.y)
            path.lineTo(p.x + 0.1f, p.y + 0.1f)
            return path
        }

        path.moveTo(points[0].x, points[0].y)

        if (points.size == 2) {
            path.lineTo(points[1].x, points[1].y)
            return path
        }

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f

            if (i == 1) {
                path.lineTo(midX, midY)
            } else {
                path.quadTo(prev.x, prev.y, midX, midY)
            }
        }

        val last = points.last()
        path.lineTo(last.x, last.y)

        return path
    }
}
