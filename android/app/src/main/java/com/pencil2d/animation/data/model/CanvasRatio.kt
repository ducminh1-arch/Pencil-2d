package com.pencil2d.animation.data.model

enum class CanvasRatio(
    val label: String,
    val description: String,
    val targetWidth: Int,
    val targetHeight: Int,
    val aspectRatio: Float
) {
    RATIO_1_1("1:1", "Square (Instagram/Social)", 1080, 1080, 1.0f),
    RATIO_16_9("16:9", "Landscape (YouTube/Cinema)", 1920, 1080, 16f / 9f),
    RATIO_4_3("4:3", "Classic (Tablet/TV)", 1440, 1080, 4f / 3f),
    RATIO_9_16("9:16", "Portrait (TikTok/Reels/Shorts)", 1080, 1920, 9f / 16f);

    companion object {
        fun fromLabel(label: String): CanvasRatio {
            return entries.firstOrNull { it.label == label } ?: RATIO_1_1
        }
    }
}
