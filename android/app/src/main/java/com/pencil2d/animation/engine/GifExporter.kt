package com.pencil2d.animation.engine

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import kotlin.math.abs

object GifExporter {

    /**
     * Exports a sequence of bitmaps into an Animated GIF file
     */
    fun exportToGif(
        frames: List<Bitmap>,
        fps: Int,
        outputFile: File,
        loopCount: Int = 0 // 0 = infinite loop
    ): Boolean {
        if (frames.isEmpty()) return false

        try {
            FileOutputStream(outputFile).use { out ->
                // Write Header
                out.write("GIF89a".toByteArray(Charsets.US_ASCII))

                val width = frames[0].width
                val height = frames[0].height

                // Logical Screen Descriptor
                writeShort(out, width)
                writeShort(out, height)
                out.write(0x70) // No Global Color Table, 8 bits/pixel
                out.write(0)    // Background Color Index
                out.write(0)    // Pixel Aspect Ratio

                // Netscape Loop Application Extension
                out.write(0x21) // Extension Introducer
                out.write(0xFF) // App Extension Label
                out.write(11)   // Block Size
                out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
                out.write(3)    // Sub-block data size
                out.write(1)    // Sub-block ID
                writeShort(out, loopCount) // Loop count
                out.write(0)    // Block Terminator

                val delayCentisecs = (100f / fps.coerceAtLeast(1)).toInt().coerceAtLeast(2)

                for (frame in frames) {
                    val frameBmp = if (frame.width != width || frame.height != height) {
                        Bitmap.createScaledBitmap(frame, width, height, true)
                    } else {
                        frame
                    }
                    writeFrame(out, frameBmp, delayCentisecs)
                }

                // GIF Trailer
                out.write(0x3B)
                out.flush()
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun writeFrame(out: OutputStream, bitmap: Bitmap, delayCentisecs: Int) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Generate 256-color palette
        val (palette, indexedPixels) = quantizeImage(pixels)

        // Graphic Control Extension
        out.write(0x21) // Extension Introducer
        out.write(0xF9) // Graphic Control Label
        out.write(4)    // Byte size
        out.write(0x04) // Disposal Method: Restore to background color
        writeShort(out, delayCentisecs) // Delay time
        out.write(0)    // Transparent color index (none)
        out.write(0)    // Block Terminator

        // Image Descriptor
        out.write(0x2C) // Image Separator
        writeShort(out, 0) // Left
        writeShort(out, 0) // Top
        writeShort(out, width)
        writeShort(out, height)
        out.write(0x87) // Local Color Table Present, 256 colors (2^(7+1) = 256)

        // Write Local Color Table (256 * 3 bytes)
        for (color in palette) {
            out.write((color shr 16) and 0xFF) // R
            out.write((color shr 8) and 0xFF)  // G
            out.write(color and 0xFF)         // B
        }

        // Write LZW Image Data
        writeLzwImageData(out, indexedPixels, 8)
    }

    private fun quantizeImage(pixels: IntArray): Pair<IntArray, ByteArray> {
        // Collect popular colors or create a standard color palette
        val colorCounts = mutableMapOf<Int, Int>()
        for (i in pixels.indices step 4) { // sample every 4th pixel for speed
            val c = pixels[i] and 0x00FFFFFF
            colorCounts[c] = (colorCounts[c] ?: 0) + 1
        }

        val topColors = colorCounts.entries
            .sortedByDescending { it.value }
            .take(256)
            .map { it.key }
            .toIntArray()

        val palette = IntArray(256)
        System.arraycopy(topColors, 0, palette, 0, topColors.size)

        // Fill remaining palette slots if less than 256 colors
        if (topColors.size < 256) {
            for (i in topColors.size until 256) {
                palette[i] = Color.WHITE
            }
        }

        val indexedPixels = ByteArray(pixels.size)
        val colorCache = HashMap<Int, Byte>(512)

        for (i in pixels.indices) {
            val c = pixels[i] and 0x00FFFFFF
            val cached = colorCache[c]
            if (cached != null) {
                indexedPixels[i] = cached
            } else {
                var bestIdx = 0
                var minDist = Int.MAX_VALUE
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF

                for (pIdx in 0 until topColors.size.coerceAtMost(256)) {
                    val pc = palette[pIdx]
                    val pr = (pc shr 16) and 0xFF
                    val pg = (pc shr 8) and 0xFF
                    val pb = pc and 0xFF
                    val dist = (r - pr) * (r - pr) + (g - pg) * (g - pg) + (b - pb) * (b - pb)
                    if (dist < minDist) {
                        minDist = dist
                        bestIdx = pIdx
                        if (dist == 0) break
                    }
                }
                val byteIdx = bestIdx.toByte()
                colorCache[c] = byteIdx
                indexedPixels[i] = byteIdx
            }
        }

        return Pair(palette, indexedPixels)
    }

    private fun writeLzwImageData(out: OutputStream, indexedPixels: ByteArray, colorDepth: Int) {
        val initCodeSize = colorDepth.coerceAtLeast(2)
        out.write(initCodeSize)

        val clearCode = 1 shl initCodeSize
        val eoiCode = clearCode + 1
        var codeSize = initCodeSize + 1
        var maxCode = 1 shl codeSize

        val prefixTable = IntArray(4096)
        val suffixTable = ByteArray(4096)
        var nextCode = eoiCode + 1

        val buffer = ByteArrayOutputStream()
        var curAccum = 0
        var curBits = 0

        fun emit(code: Int) {
            curAccum = curAccum or (code shl curBits)
            curBits += codeSize
            while (curBits >= 8) {
                buffer.write(curAccum and 0xFF)
                curAccum = curAccum shr 8
                curBits -= 8
            }
        }

        fun flushBits() {
            if (curBits > 0) {
                buffer.write(curAccum and 0xFF)
                curAccum = 0
                curBits = 0
            }
        }

        emit(clearCode)

        var prefix = -1
        for (b in indexedPixels) {
            val suffix = (b.toInt() and 0xFF).toByte()
            val suffixInt = suffix.toInt() and 0xFF

            if (prefix == -1) {
                prefix = suffixInt
                continue
            }

            // Search if (prefix, suffix) is in table
            var found = -1
            for (c in (eoiCode + 1) until nextCode) {
                if (prefixTable[c] == prefix && suffixTable[c] == suffix) {
                    found = c
                    break
                }
            }

            if (found != -1) {
                prefix = found
            } else {
                emit(prefix)
                if (nextCode < 4096) {
                    prefixTable[nextCode] = prefix
                    suffixTable[nextCode] = suffix
                    nextCode++
                    if (nextCode > maxCode && codeSize < 12) {
                        codeSize++
                        maxCode = 1 shl codeSize
                    }
                } else {
                    emit(clearCode)
                    codeSize = initCodeSize + 1
                    maxCode = 1 shl codeSize
                    nextCode = eoiCode + 1
                }
                prefix = suffixInt
            }
        }

        if (prefix != -1) {
            emit(prefix)
        }
        emit(eoiCode)
        flushBits()

        // Write sub-blocks of at most 255 bytes
        val data = buffer.toByteArray()
        var offset = 0
        while (offset < data.size) {
            val chunkSize = (data.size - offset).coerceAtMost(255)
            out.write(chunkSize)
            out.write(data, offset, chunkSize)
            offset += chunkSize
        }
        out.write(0) // Sub-block terminator
    }

    private fun writeShort(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }
}
