package com.pencil2d.animation.data.model

import java.util.UUID

data class DrawingLayer(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var isVisible: Boolean = true,
    var isLocked: Boolean = false,
    var opacity: Float = 1.0f,
    // File name of the bitmap stored in project frame folder (or null if empty)
    var bitmapFileName: String? = null
) {
    fun deepCopy(): DrawingLayer {
        return DrawingLayer(
            id = UUID.randomUUID().toString(),
            name = name,
            isVisible = isVisible,
            isLocked = isLocked,
            opacity = opacity,
            bitmapFileName = bitmapFileName
        )
    }
}
