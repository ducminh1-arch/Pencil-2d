package com.pencil2d.animation.data.model

import java.util.UUID

data class AnimationProject(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "My Animation",
    var canvasRatio: CanvasRatio = CanvasRatio.RATIO_1_1,
    var fps: Int = 12,
    var loopPlayback: Boolean = true,
    var projectType: String = TYPE_ANIMATION,
    var createdAt: Long = System.currentTimeMillis(),
    var modifiedAt: Long = System.currentTimeMillis(),
    val frames: MutableList<AnimationFrame> = mutableListOf(),
    var thumbnailFileName: String? = null
) {
    val frameCount: Int
        get() = frames.size

    val durationSeconds: Float
        get() = if (fps > 0) frameCount.toFloat() / fps else 0f

    val isPhotoProject: Boolean
        get() = projectType == TYPE_IMAGE

    companion object {
        const val TYPE_ANIMATION = "animation"
        const val TYPE_IMAGE = "image"
    }
}
