package com.pencil2d.animation.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import com.pencil2d.animation.data.model.AnimationFrame
import com.pencil2d.animation.data.model.DrawingLayer
import com.pencil2d.animation.data.model.OnionSkinConfig

object FrameCompositor {

    /**
     * Composites a list of bitmaps for layers of a single frame into one final Bitmap
     */
    fun compositeFrame(
        width: Int,
        height: Int,
        layers: List<DrawingLayer>,
        layerBitmaps: Map<String, Bitmap>,
        backgroundColor: Int = Color.WHITE
    ): Bitmap {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        if (backgroundColor != Color.TRANSPARENT) {
            canvas.drawColor(backgroundColor)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        for (layer in layers) {
            if (!layer.isVisible) continue
            val bmp = layerBitmaps[layer.id] ?: continue

            paint.alpha = (layer.opacity.coerceIn(0f, 1f) * 255).toInt()
            canvas.drawBitmap(bmp, 0f, 0f, paint)
        }

        return result
    }

    /**
     * Draws an onion skin overlay (previous or next frame) with custom alpha or color tint
     */
    fun drawOnionSkin(
        targetCanvas: Canvas,
        frameBitmap: Bitmap,
        tintColor: Int,
        alpha: Float = 0.35f,
        useColorTint: Boolean = true
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        paint.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()

        if (useColorTint) {
            paint.colorFilter = PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP)
        }

        targetCanvas.drawBitmap(frameBitmap, 0f, 0f, paint)
    }
}
