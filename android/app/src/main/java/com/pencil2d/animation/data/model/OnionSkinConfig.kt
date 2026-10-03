package com.pencil2d.animation.data.model

import android.graphics.Color

data class OnionSkinConfig(
    var isEnabled: Boolean = true,
    var prevFramesCount: Int = 1,
    var nextFramesCount: Int = 1,
    var prevFrameTint: Int = Color.argb(180, 230, 50, 50), // Red overlay for previous frames
    var nextFrameTint: Int = Color.argb(180, 50, 180, 50), // Green overlay for next frames
    var opacity: Float = 0.35f
)

enum class ExportFormat(val extension: String, val mimeType: String, val label: String) {
    MP4("mp4", "video/mp4", "MP4 Video"),
    GIF("gif", "image/gif", "Animated GIF"),
    PNG_SEQUENCE("zip", "application/zip", "PNG Sequence (ZIP)")
}

data class ExportOption(
    val format: ExportFormat = ExportFormat.MP4,
    val fps: Int = 12,
    val loopCount: Int = 1, // Repeat animation N times in exported video
    val resolutionScale: Float = 1.0f
)
