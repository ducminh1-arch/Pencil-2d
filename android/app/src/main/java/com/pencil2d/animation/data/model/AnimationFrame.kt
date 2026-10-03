package com.pencil2d.animation.data.model

import java.util.UUID

data class AnimationFrame(
    val id: String = UUID.randomUUID().toString(),
    var index: Int = 0,
    val layers: MutableList<DrawingLayer> = mutableListOf(),
    var thumbnailFileName: String? = null
) {
    fun deepCopy(): AnimationFrame {
        return AnimationFrame(
            id = UUID.randomUUID().toString(),
            index = index,
            layers = layers.map { it.deepCopy() }.toMutableList(),
            thumbnailFileName = thumbnailFileName
        )
    }

    companion object {
        fun createDefault(frameIndex: Int): AnimationFrame {
            val frame = AnimationFrame(index = frameIndex)
            frame.layers.add(DrawingLayer(name = "Background", isVisible = true, isLocked = false, opacity = 1f))
            frame.layers.add(DrawingLayer(name = "Layer 1", isVisible = true, isLocked = false, opacity = 1f))
            return frame
        }
    }
}
