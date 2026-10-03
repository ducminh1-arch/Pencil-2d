package com.pencil2d.animation.engine

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

object VideoExporter {

    private const val MIME_TYPE = "video/avc" // H.264
    private const val BIT_RATE = 4_000_000     // 4 Mbps
    private const val I_FRAME_INTERVAL = 1     // 1 second

    /**
     * Exports a list of Bitmaps into an MP4 video using native MediaCodec and MediaMuxer
     */
    fun exportToMp4(
        frames: List<Bitmap>,
        fps: Int,
        outputFile: File,
        loopCount: Int = 1
    ): Boolean {
        if (frames.isEmpty()) return false

        // Ensure width and height are even numbers (requirement for H.264)
        var width = frames[0].width
        var height = frames[0].height
        if (width % 2 != 0) width -= 1
        if (height % 2 != 0) height -= 1

        val expandedFrames = mutableListOf<Bitmap>()
        repeat(loopCount.coerceAtLeast(1)) {
            expandedFrames.addAll(frames)
        }

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps.coerceAtLeast(1))
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }

            codec = MediaCodec.createEncoderByType(MIME_TYPE)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val frameDurationUs = 1_000_000L / fps.coerceAtLeast(1)
            var presentationTimeUs = 0L

            for (i in expandedFrames.indices) {
                val orig = expandedFrames[i]
                val scaled = if (orig.width != width || orig.height != height) {
                    Bitmap.createScaledBitmap(orig, width, height, true)
                } else {
                    orig
                }

                val yuvData = bitmapToYuv420p(scaled, width, height)

                // Feed input buffer
                val inputIndex = codec.dequeueInputBuffer(10_000L)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)
                    inputBuffer?.clear()
                    inputBuffer?.put(yuvData)
                    codec.queueInputBuffer(
                        inputIndex,
                        0,
                        yuvData.size,
                        presentationTimeUs,
                        if (i == expandedFrames.size - 1) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                    )
                    presentationTimeUs += frameDurationUs
                }

                // Drain output buffer
                var outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                while (outputIndex >= 0) {
                    val encodedData = codec.getOutputBuffer(outputIndex)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        bufferInfo.size = 0
                    }

                    if (bufferInfo.size != 0 && encodedData != null) {
                        if (!muxerStarted) {
                            val newFormat = codec.outputFormat
                            videoTrackIndex = muxer.addTrack(newFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                    }

                    codec.releaseOutputBuffer(outputIndex, false)
                    outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0L)
                }
            }

            // Finish draining
            var isEos = false
            while (!isEos) {
                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000L)
                if (outputIndex >= 0) {
                    val encodedData = codec.getOutputBuffer(outputIndex)
                    if (bufferInfo.size != 0 && encodedData != null && muxerStarted) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isEos = true
                    }
                } else if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    break
                }
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (e: Exception) {
                // ignore
            }
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun bitmapToYuv420p(bitmap: Bitmap, width: Int, height: Int): ByteArray {
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)

        val yuv = ByteArray(width * height * 3 / 2)
        var yIndex = 0
        var uIndex = width * height
        var vIndex = width * height + (width * height / 4)

        for (j in 0 until height) {
            for (i in 0 until width) {
                val c = argb[j * width + i]
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF

                // RGB to YUV conversion formula
                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                yuv[yIndex++] = y.coerceIn(0, 255).toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                    val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128
                    yuv[uIndex++] = u.coerceIn(0, 255).toByte()
                    yuv[vIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }

        return yuv
    }
}
