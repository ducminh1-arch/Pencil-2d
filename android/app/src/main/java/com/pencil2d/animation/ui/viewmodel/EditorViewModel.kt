package com.pencil2d.animation.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pencil2d.animation.data.model.AnimationFrame
import com.pencil2d.animation.data.model.AnimationProject
import com.pencil2d.animation.data.model.CanvasRatio
import com.pencil2d.animation.data.model.DrawingLayer
import com.pencil2d.animation.data.model.DrawingTool
import com.pencil2d.animation.data.model.ExportFormat
import com.pencil2d.animation.data.model.ExportOption
import com.pencil2d.animation.data.model.OnionSkinConfig
import com.pencil2d.animation.data.model.StrokePoint
import com.pencil2d.animation.data.repository.ProjectRepository
import com.pencil2d.animation.engine.FloodFillEngine
import com.pencil2d.animation.engine.FrameCompositor
import com.pencil2d.animation.engine.GifExporter
import com.pencil2d.animation.engine.SmoothPathInterpolator
import com.pencil2d.animation.engine.VideoExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayDeque
import java.util.UUID

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)

    private val _project = MutableStateFlow<AnimationProject?>(null)
    val project: StateFlow<AnimationProject?> = _project.asStateFlow()

    private val _currentFrameIndex = MutableStateFlow(0)
    val currentFrameIndex: StateFlow<Int> = _currentFrameIndex.asStateFlow()

    private val _currentLayerIndex = MutableStateFlow(1) // Default to Layer 1 (above background)
    val currentLayerIndex: StateFlow<Int> = _currentLayerIndex.asStateFlow()

    // Tool settings
    private val _selectedTool = MutableStateFlow(DrawingTool.PEN)
    val selectedTool: StateFlow<DrawingTool> = _selectedTool.asStateFlow()

    private val _strokeColor = MutableStateFlow(Color.BLACK)
    val strokeColor: StateFlow<Int> = _strokeColor.asStateFlow()

    private val _strokeWidth = MutableStateFlow(12f)
    val strokeWidth: StateFlow<Float> = _strokeWidth.asStateFlow()

    private val _strokeOpacity = MutableStateFlow(1.0f)
    val strokeOpacity: StateFlow<Float> = _strokeOpacity.asStateFlow()

    // Onion Skin
    private val _onionSkinConfig = MutableStateFlow(OnionSkinConfig())
    val onionSkinConfig: StateFlow<OnionSkinConfig> = _onionSkinConfig.asStateFlow()

    // Playback state
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackFrameIndex = MutableStateFlow(0)
    val playbackFrameIndex: StateFlow<Int> = _playbackFrameIndex.asStateFlow()

    private var playbackJob: Job? = null

    // Layer Bitmaps cache: Key is Pair(frameId, layerId)
    private val layerBitmaps = mutableMapOf<Pair<String, String>, Bitmap>()

    // Undo / Redo history: Key is Pair(frameId, layerId)
    private val undoStacks = mutableMapOf<Pair<String, String>, ArrayDeque<Bitmap>>()
    private val redoStacks = mutableMapOf<Pair<String, String>, ArrayDeque<Bitmap>>()

    // Export progress
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    // Version counter to trigger Canvas recomposition
    private val _canvasVersion = MutableStateFlow(0L)
    val canvasVersion: StateFlow<Long> = _canvasVersion.asStateFlow()

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            val proj = repository.getProject(projectId)
            if (proj != null) {
                _project.value = proj
                _currentFrameIndex.value = 0
                _currentLayerIndex.value = (proj.frames.firstOrNull()?.layers?.size?.minus(1))?.coerceAtLeast(0) ?: 0
                preloadFrameBitmaps(proj)
                triggerCanvasRedraw()
            }
        }
    }

    private suspend fun preloadFrameBitmaps(proj: AnimationProject) = withContext(Dispatchers.IO) {
        val w = proj.canvasRatio.targetWidth
        val h = proj.canvasRatio.targetHeight

        for (frame in proj.frames) {
            for (layer in frame.layers) {
                val key = Pair(frame.id, layer.id)
                val diskBmp = repository.getLayerBitmap(proj.id, frame.id, layer.id)
                val bmp: Bitmap = if (diskBmp == null) {
                    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                } else if (diskBmp.config != Bitmap.Config.ARGB_8888 || !diskBmp.isMutable) {
                    diskBmp.copy(Bitmap.Config.ARGB_8888, true) ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                } else {
                    diskBmp
                }
                layerBitmaps[key] = bmp
            }
        }
    }

    fun getActiveBitmap(): Bitmap? {
        val proj = _project.value ?: return null
        val frame = proj.frames.getOrNull(_currentFrameIndex.value) ?: return null
        val layer = frame.layers.getOrNull(_currentLayerIndex.value) ?: return null
        val key = Pair(frame.id, layer.id)

        var bmp = layerBitmaps[key]
        if (bmp == null) {
            bmp = Bitmap.createBitmap(proj.canvasRatio.targetWidth, proj.canvasRatio.targetHeight, Bitmap.Config.ARGB_8888)
            layerBitmaps[key] = bmp
        }
        return bmp
    }

    fun getCompositeBitmap(frameIndex: Int): Bitmap? {
        val proj = _project.value ?: return null
        val frame = proj.frames.getOrNull(frameIndex) ?: return null
        val w = proj.canvasRatio.targetWidth
        val h = proj.canvasRatio.targetHeight

        val currentLayerMap = mutableMapOf<String, Bitmap>()
        for (layer in frame.layers) {
            val key = Pair(frame.id, layer.id)
            var bmp = layerBitmaps[key]
            if (bmp == null) {
                val diskBmp = repository.getLayerBitmap(proj.id, frame.id, layer.id)
                bmp = if (diskBmp == null) {
                    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                } else if (!diskBmp.isMutable) {
                    diskBmp.copy(Bitmap.Config.ARGB_8888, true) ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                } else {
                    diskBmp
                }
                layerBitmaps[key] = bmp
            }
            currentLayerMap[layer.id] = bmp
        }

        return FrameCompositor.compositeFrame(w, h, frame.layers, currentLayerMap)
    }

    fun getPreviousFrameBitmap(): Bitmap? {
        val idx = _currentFrameIndex.value - 1
        return if (idx >= 0) getCompositeBitmap(idx) else null
    }

    fun getNextFrameBitmap(): Bitmap? {
        val proj = _project.value ?: return null
        val idx = _currentFrameIndex.value + 1
        return if (idx < proj.frames.size) getCompositeBitmap(idx) else null
    }

    // ==================== Drawing Operations ====================

    fun onStrokeStart() {
        val proj = _project.value ?: return
        val frame = proj.frames.getOrNull(_currentFrameIndex.value) ?: return
        val layer = frame.layers.getOrNull(_currentLayerIndex.value) ?: return
        if (layer.isLocked || !layer.isVisible) return

        val key = Pair(frame.id, layer.id)
        val currentBmp = layerBitmaps[key] ?: return

        // Push current bitmap to undo stack
        val stack = undoStacks.getOrPut(key) { ArrayDeque() }
        if (stack.size >= 25) {
            stack.removeFirst()
        }
        stack.addLast(currentBmp.copy(Bitmap.Config.ARGB_8888, true))
        redoStacks[key]?.clear()
    }

    fun applyStroke(points: List<StrokePoint>) {
        if (points.isEmpty()) return
        val proj = _project.value ?: return
        val frame = proj.frames.getOrNull(_currentFrameIndex.value) ?: return
        val layer = frame.layers.getOrNull(_currentLayerIndex.value) ?: return
        if (layer.isLocked || !layer.isVisible) return

        val bmp = getActiveBitmap() ?: return
        val canvas = Canvas(bmp)

        val path = SmoothPathInterpolator.buildSmoothPath(points)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = _strokeWidth.value
        }

        when (_selectedTool.value) {
            DrawingTool.PEN -> {
                paint.color = _strokeColor.value
                paint.alpha = (_strokeOpacity.value.coerceIn(0f, 1f) * 255).toInt()
                canvas.drawPath(path, paint)
            }
            DrawingTool.PENCIL -> {
                paint.color = _strokeColor.value
                paint.alpha = ((_strokeOpacity.value * 0.7f).coerceIn(0f, 1f) * 255).toInt()
                canvas.drawPath(path, paint)
            }
            DrawingTool.ERASER -> {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                canvas.drawPath(path, paint)
            }
            DrawingTool.FILL -> {
                // Handled in applyFill
            }
        }

        triggerCanvasRedraw()
    }

    fun applyFill(x: Float, y: Float) {
        val proj = _project.value ?: return
        val frame = proj.frames.getOrNull(_currentFrameIndex.value) ?: return
        val layer = frame.layers.getOrNull(_currentLayerIndex.value) ?: return
        if (layer.isLocked || !layer.isVisible) return

        val bmp = getActiveBitmap() ?: return
        onStrokeStart()

        val filled = FloodFillEngine.floodFill(
            bitmap = bmp,
            startX = x.toInt().coerceIn(0, bmp.width - 1),
            startY = y.toInt().coerceIn(0, bmp.height - 1),
            fillColor = _strokeColor.value
        )
        if (filled) {
            triggerCanvasRedraw()
            saveCurrentActiveLayer()
        }
    }

    fun onStrokeFinished() {
        saveCurrentActiveLayer()
        triggerCanvasRedraw()
    }

    private fun saveCurrentActiveLayer() {
        val proj = _project.value ?: return
        val frame = proj.frames.getOrNull(_currentFrameIndex.value) ?: return
        val layer = frame.layers.getOrNull(_currentLayerIndex.value) ?: return
        val bmp = getActiveBitmap() ?: return

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveLayerBitmap(proj.id, frame.id, layer.id, bmp)
            // Generate composite thumbnail for frame
            val composite = getCompositeBitmap(_currentFrameIndex.value)
            if (composite != null) {
                repository.saveFrameThumbnail(proj.id, frame.id, composite)
                if (_currentFrameIndex.value == 0) {
                    repository.saveProjectThumbnail(proj.id, composite)
                }
            }
            repository.saveProject(proj)
        }
    }

    fun undo() {
        val proj = _project.value ?: return
        val frame = proj.frames.getOrNull(_currentFrameIndex.value) ?: return
        val layer = frame.layers.getOrNull(_currentLayerIndex.value) ?: return
        val key = Pair(frame.id, layer.id)

        val uStack = undoStacks[key] ?: return
        if (uStack.isEmpty()) return

        val currentBmp = layerBitmaps[key] ?: return
        val rStack = redoStacks.getOrPut(key) { ArrayDeque() }
        rStack.addLast(currentBmp.copy(Bitmap.Config.ARGB_8888, true))

        val prevBmp = uStack.removeLast()
        layerBitmaps[key] = prevBmp
        saveCurrentActiveLayer()
        triggerCanvasRedraw()
    }

    fun redo() {
        val proj = _project.value ?: return
        val frame = proj.frames.getOrNull(_currentFrameIndex.value) ?: return
        val layer = frame.layers.getOrNull(_currentLayerIndex.value) ?: return
        val key = Pair(frame.id, layer.id)

        val rStack = redoStacks[key] ?: return
        if (rStack.isEmpty()) return

        val currentBmp = layerBitmaps[key] ?: return
        val uStack = undoStacks.getOrPut(key) { ArrayDeque() }
        uStack.addLast(currentBmp.copy(Bitmap.Config.ARGB_8888, true))

        val nextBmp = rStack.removeLast()
        layerBitmaps[key] = nextBmp
        saveCurrentActiveLayer()
        triggerCanvasRedraw()
    }

    fun importImageFromUri(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    importImageToCurrentLayer(bitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importImageToCurrentLayer(bitmap: Bitmap) {
        val proj = _project.value ?: return
        val frame = proj.frames.getOrNull(_currentFrameIndex.value) ?: return
        val layer = frame.layers.getOrNull(_currentLayerIndex.value) ?: return
        val key = Pair(frame.id, layer.id)
        val currentBmp = layerBitmaps[key] ?: return

        val uStack = undoStacks.getOrPut(key) { ArrayDeque() }
        uStack.addLast(currentBmp.copy(Bitmap.Config.ARGB_8888, true))

        val canvas = Canvas(currentBmp)
        val w = currentBmp.width.toFloat()
        val h = currentBmp.height.toFloat()
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()
        val scale = Math.min(w / srcW, h / srcH)
        val dstW = srcW * scale
        val dstH = srcH * scale
        val left = (w - dstW) / 2f
        val top = (h - dstH) / 2f

        val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
        val dstRect = android.graphics.RectF(left, top, left + dstW, top + dstH)
        canvas.drawBitmap(bitmap, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))

        saveCurrentActiveLayer()
        triggerCanvasRedraw()
    }

    // ==================== Timeline & Frames ====================

    fun selectFrame(index: Int) {
        val proj = _project.value ?: return
        if (index in 0 until proj.frames.size && _currentFrameIndex.value != index) {
            _currentFrameIndex.value = index
        }
    }

    fun addBlankFrame() {
        val proj = _project.value ?: return
        val insertIndex = _currentFrameIndex.value + 1
        val newFrame = AnimationFrame.createDefault(insertIndex)

        // Match layers of current project
        val currentFrame = proj.frames[_currentFrameIndex.value]
        newFrame.layers.clear()
        for (layer in currentFrame.layers) {
            newFrame.layers.add(DrawingLayer(name = layer.name, isVisible = true, isLocked = false, opacity = layer.opacity))
        }

        proj.frames.add(insertIndex, newFrame)
        reindexFrames(proj)

        // Initialize bitmaps
        val w = proj.canvasRatio.targetWidth
        val h = proj.canvasRatio.targetHeight
        for (layer in newFrame.layers) {
            val key = Pair(newFrame.id, layer.id)
            layerBitmaps[key] = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        }

        _currentFrameIndex.value = insertIndex
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProject(proj)
        }
        triggerCanvasRedraw()
    }

    fun duplicateCurrentFrame() {
        val proj = _project.value ?: return
        val curIndex = _currentFrameIndex.value
        val curFrame = proj.frames[curIndex]
        val insertIndex = curIndex + 1

        val newFrame = AnimationFrame(index = insertIndex)
        for (layer in curFrame.layers) {
            val newLayer = layer.deepCopy()
            newFrame.layers.add(newLayer)

            // Clone bitmap
            val origKey = Pair(curFrame.id, layer.id)
            val origBmp = layerBitmaps[origKey]
            val key = Pair(newFrame.id, newLayer.id)
            if (origBmp != null) {
                val cloned = origBmp.copy(Bitmap.Config.ARGB_8888, true)
                layerBitmaps[key] = cloned
                repository.saveLayerBitmap(proj.id, newFrame.id, newLayer.id, cloned)
            }
        }

        proj.frames.add(insertIndex, newFrame)
        reindexFrames(proj)
        _currentFrameIndex.value = insertIndex

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProject(proj)
        }
        triggerCanvasRedraw()
    }

    fun deleteCurrentFrame() {
        val proj = _project.value ?: return
        if (proj.frames.size <= 1) return // Must keep at least 1 frame

        val curIndex = _currentFrameIndex.value
        proj.frames.removeAt(curIndex)
        reindexFrames(proj)

        _currentFrameIndex.value = curIndex.coerceAtMost(proj.frames.size - 1)
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProject(proj)
        }
        triggerCanvasRedraw()
    }

    fun moveFrame(fromIndex: Int, toIndex: Int) {
        val proj = _project.value ?: return
        if (fromIndex !in proj.frames.indices || toIndex !in proj.frames.indices) return

        val frame = proj.frames.removeAt(fromIndex)
        proj.frames.add(toIndex, frame)
        reindexFrames(proj)
        _currentFrameIndex.value = toIndex

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProject(proj)
        }
        triggerCanvasRedraw()
    }

    private fun reindexFrames(proj: AnimationProject) {
        proj.frames.forEachIndexed { i, f -> f.index = i }
    }

    // ==================== Playback ====================

    fun togglePlayback() {
        if (_isPlaying.value) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        val proj = _project.value ?: return
        if (proj.frames.isEmpty()) return

        _isPlaying.value = true
        _playbackFrameIndex.value = _currentFrameIndex.value

        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            val delayMs = (1000L / proj.fps.coerceAtLeast(1))
            while (isActive && _isPlaying.value) {
                delay(delayMs)
                val next = _playbackFrameIndex.value + 1
                if (next >= proj.frames.size) {
                    if (proj.loopPlayback) {
                        _playbackFrameIndex.value = 0
                    } else {
                        stopPlayback()
                        break
                    }
                } else {
                    _playbackFrameIndex.value = next
                }
            }
        }
    }

    fun stopPlayback() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
    }

    // ==================== Layer Management ====================

    fun selectLayer(index: Int) {
        val proj = _project.value ?: return
        val frame = proj.frames.getOrNull(_currentFrameIndex.value) ?: return
        if (index in 0 until frame.layers.size) {
            _currentLayerIndex.value = index
            triggerCanvasRedraw()
        }
    }

    fun addLayer(name: String) {
        val proj = _project.value ?: return
        val w = proj.canvasRatio.targetWidth
        val h = proj.canvasRatio.targetHeight

        for (frame in proj.frames) {
            val newLayer = DrawingLayer(name = name.ifBlank { "Layer ${frame.layers.size + 1}" })
            frame.layers.add(newLayer)
            val key = Pair(frame.id, newLayer.id)
            layerBitmaps[key] = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        }

        val curFrame = proj.frames[_currentFrameIndex.value]
        _currentLayerIndex.value = curFrame.layers.size - 1

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProject(proj)
        }
        triggerCanvasRedraw()
    }

    fun toggleLayerVisibility(layerIndex: Int) {
        val proj = _project.value ?: return
        val curFrame = proj.frames.getOrNull(_currentFrameIndex.value) ?: return
        val layer = curFrame.layers.getOrNull(layerIndex) ?: return

        layer.isVisible = !layer.isVisible
        triggerCanvasRedraw()
    }

    fun toggleLayerLock(layerIndex: Int) {
        val proj = _project.value ?: return
        val curFrame = proj.frames.getOrNull(_currentFrameIndex.value) ?: return
        val layer = curFrame.layers.getOrNull(layerIndex) ?: return

        layer.isLocked = !layer.isLocked
    }

    fun setLayerOpacity(layerIndex: Int, opacity: Float) {
        val proj = _project.value ?: return
        val curFrame = proj.frames.getOrNull(_currentFrameIndex.value) ?: return
        val layer = curFrame.layers.getOrNull(layerIndex) ?: return

        layer.opacity = opacity
        triggerCanvasRedraw()
    }

    // ==================== Onion Skinning ====================

    fun toggleOnionSkin() {
        val cfg = _onionSkinConfig.value
        _onionSkinConfig.value = cfg.copy(isEnabled = !cfg.isEnabled)
        triggerCanvasRedraw()
    }

    fun setOnionSkinOpacity(opacity: Float) {
        val cfg = _onionSkinConfig.value
        _onionSkinConfig.value = cfg.copy(opacity = opacity)
        triggerCanvasRedraw()
    }

    // ==================== Tool Configuration ====================

    fun setTool(tool: DrawingTool) {
        _selectedTool.value = tool
    }

    fun setColor(color: Int) {
        _strokeColor.value = color
    }

    fun setStrokeWidth(width: Float) {
        _strokeWidth.value = width
    }

    fun setStrokeOpacity(opacity: Float) {
        _strokeOpacity.value = opacity
    }

    fun setCanvasRatio(ratio: CanvasRatio) {
        val proj = _project.value ?: return
        if (proj.canvasRatio == ratio) return

        val newW = ratio.targetWidth
        val newH = ratio.targetHeight

        proj.canvasRatio = ratio

        // Rescale existing cached layer bitmaps to fit the new canvas ratio
        layerBitmaps.forEach { (key, oldBmp) ->
            if (oldBmp.width != newW || oldBmp.height != newH) {
                val newBmp = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(newBmp)
                val scale = Math.min(newW.toFloat() / oldBmp.width, newH.toFloat() / oldBmp.height)
                val dstW = oldBmp.width * scale
                val dstH = oldBmp.height * scale
                val left = (newW - dstW) / 2f
                val top = (newH - dstH) / 2f
                val srcRect = android.graphics.Rect(0, 0, oldBmp.width, oldBmp.height)
                val dstRect = android.graphics.RectF(left, top, left + dstW, top + dstH)
                canvas.drawBitmap(oldBmp, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
                layerBitmaps[key] = newBmp
            }
        }

        // Re-emit StateFlow so Compose updates state and recomposes immediately
        _project.value = proj.copy(canvasRatio = ratio)
        triggerCanvasRedraw()

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProject(proj)
        }
    }

    fun setFps(fps: Int) {
        val proj = _project.value ?: return
        proj.fps = fps
        _project.value = proj.copy(fps = fps)
        if (_isPlaying.value) {
            startPlayback()
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProject(proj)
        }
    }

    private fun triggerCanvasRedraw() {
        _canvasVersion.value = System.currentTimeMillis()
    }

    // ==================== Export Engine ====================

    fun exportProject(option: ExportOption, context: Context) {
        val proj = _project.value ?: return
        _isExporting.value = true
        _exportMessage.value = "Preparing frames..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val frames = mutableListOf<Bitmap>()
                for (i in proj.frames.indices) {
                    val bmp = getCompositeBitmap(i)
                    if (bmp != null) {
                        frames.add(bmp)
                    }
                }

                if (frames.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _isExporting.value = false
                        _exportMessage.value = "No frames to export"
                    }
                    return@launch
                }

                val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
                val timestamp = System.currentTimeMillis()
                val filename = "Pencil2D_${proj.title.replace(" ", "_")}_$timestamp.${option.format.extension}"
                val outputFile = File(exportDir, filename)

                var success = false
                when (option.format) {
                    ExportFormat.MP4 -> {
                        withContext(Dispatchers.Main) {
                            _exportMessage.value = "Encoding MP4 video..."
                        }
                        success = VideoExporter.exportToMp4(
                            frames = frames,
                            fps = option.fps,
                            outputFile = outputFile,
                            loopCount = option.loopCount
                        )
                    }
                    ExportFormat.GIF -> {
                        withContext(Dispatchers.Main) {
                            _exportMessage.value = "Creating Animated GIF..."
                        }
                        success = GifExporter.exportToGif(
                            frames = frames,
                            fps = option.fps,
                            outputFile = outputFile,
                            loopCount = 0
                        )
                    }
                    ExportFormat.PNG_SEQUENCE -> {
                        withContext(Dispatchers.Main) {
                            _exportMessage.value = "Saving PNG images..."
                        }
                        // Save first/active frame or sequence
                        FileOutputStream(outputFile).use { out ->
                            frames[0].compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        success = true
                    }
                }

                withContext(Dispatchers.Main) {
                    _isExporting.value = false
                    if (success && outputFile.exists()) {
                        _exportMessage.value = "Export successful!"
                        shareExportedFile(context, outputFile, option.format.mimeType)
                    } else {
                        _exportMessage.value = "Export failed. Please try again."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isExporting.value = false
                    _exportMessage.value = "Error: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun shareExportedFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share Animation")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
