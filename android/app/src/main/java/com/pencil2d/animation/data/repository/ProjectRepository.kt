package com.pencil2d.animation.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pencil2d.animation.data.model.AnimationFrame
import com.pencil2d.animation.data.model.AnimationProject
import com.pencil2d.animation.data.model.CanvasRatio
import com.pencil2d.animation.data.model.DrawingLayer
import com.pencil2d.animation.engine.FrameCompositor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProjectRepository(private val context: Context) {

    private val gson = Gson()
    private val projectsDir: File
        get() = File(context.filesDir, "projects").apply { if (!exists()) mkdirs() }

    suspend fun getAllProjects(): List<AnimationProject> = withContext(Dispatchers.IO) {
        val projectDirs = projectsDir.listFiles { f -> f.isDirectory } ?: emptyArray()
        val projects = mutableListOf<AnimationProject>()

        for (dir in projectDirs) {
            val metaFile = File(dir, "project.json")
            if (metaFile.exists()) {
                try {
                    val json = metaFile.readText()
                    val project = gson.fromJson(json, AnimationProject::class.java)
                    projects.add(project)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        if (projects.isEmpty()) {
            val sample = createSampleStickmanProject()
            projects.add(sample)
            val sample2 = createSampleBouncingBallProject()
            projects.add(sample2)
            val samplePhoto = createSamplePhotoPortraitProject()
            projects.add(samplePhoto)
        } else if (projects.none { it.projectType == AnimationProject.TYPE_IMAGE }) {
            val samplePhoto = createSamplePhotoPortraitProject()
            projects.add(samplePhoto)
        }

        projects.sortedByDescending { it.modifiedAt }
    }

    suspend fun getProject(projectId: String): AnimationProject? = withContext(Dispatchers.IO) {
        val pDir = File(projectsDir, projectId)
        val metaFile = File(pDir, "project.json")
        if (!metaFile.exists()) return@withContext null
        try {
            gson.fromJson(metaFile.readText(), AnimationProject::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveProject(project: AnimationProject) = withContext(Dispatchers.IO) {
        project.modifiedAt = System.currentTimeMillis()
        val pDir = File(projectsDir, project.id).apply { if (!exists()) mkdirs() }
        val metaFile = File(pDir, "project.json")
        val json = gson.toJson(project)
        metaFile.writeText(json)
    }

    suspend fun createNewProject(
        title: String,
        ratio: CanvasRatio,
        fps: Int = 12,
        type: String = AnimationProject.TYPE_ANIMATION
    ): AnimationProject = withContext(Dispatchers.IO) {
        val project = AnimationProject(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Project ${System.currentTimeMillis() % 1000}" },
            canvasRatio = ratio,
            fps = if (type == AnimationProject.TYPE_IMAGE) 1 else fps,
            projectType = type
        )

        // Add 1 initial blank frame
        val frame = AnimationFrame.createDefault(0)
        project.frames.add(frame)

        saveProject(project)
        project
    }

    suspend fun createPhotoProjectFromBitmap(
        title: String,
        bitmap: Bitmap
    ): AnimationProject = withContext(Dispatchers.IO) {
        val project = AnimationProject(
            id = UUID.randomUUID().toString(),
            title = title.ifBlank { "Photo ${System.currentTimeMillis() % 1000}" },
            canvasRatio = CanvasRatio.RATIO_1_1,
            fps = 1,
            projectType = AnimationProject.TYPE_IMAGE
        )

        val frame = AnimationFrame.createDefault(0)
        project.frames.add(frame)

        val canvasBmp = Bitmap.createBitmap(720, 720, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBmp)
        canvas.drawColor(Color.WHITE)

        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()
        val scale = Math.min(720f / srcW, 720f / srcH)
        val dstW = srcW * scale
        val dstH = srcH * scale
        val left = (720f - dstW) / 2f
        val top = (720f - dstH) / 2f

        val srcRect = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
        val dstRect = android.graphics.RectF(left, top, left + dstW, top + dstH)
        canvas.drawBitmap(bitmap, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))

        val charLayer = frame.layers[1]
        saveLayerBitmap(project.id, frame.id, charLayer.id, canvasBmp)
        saveFrameThumbnail(project.id, frame.id, canvasBmp)
        saveProjectThumbnail(project.id, canvasBmp)

        saveProject(project)
        project
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        val pDir = File(projectsDir, projectId)
        if (pDir.exists()) {
            pDir.deleteRecursively()
        }
    }

    fun getLayerBitmap(projectId: String, frameId: String, layerId: String): Bitmap? {
        val file = File(getFrameDir(projectId, frameId), "layer_${layerId}.png")
        return if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    }

    fun saveLayerBitmap(projectId: String, frameId: String, layerId: String, bitmap: Bitmap) {
        val frameDir = getFrameDir(projectId, frameId)
        val file = File(frameDir, "layer_${layerId}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    fun getFrameThumbnail(projectId: String, frameId: String): Bitmap? {
        val file = File(getFrameDir(projectId, frameId), "thumb.png")
        return if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    }

    fun saveFrameThumbnail(projectId: String, frameId: String, bitmap: Bitmap) {
        val frameDir = getFrameDir(projectId, frameId)
        val file = File(frameDir, "thumb.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
    }

    fun getProjectThumbnailFile(projectId: String): File {
        return File(File(projectsDir, projectId), "thumb.png")
    }

    fun saveProjectThumbnail(projectId: String, bitmap: Bitmap) {
        val file = getProjectThumbnailFile(projectId)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
    }

    private fun getFrameDir(projectId: String, frameId: String): File {
        return File(File(projectsDir, projectId), "frames/$frameId").apply {
            if (!exists()) mkdirs()
        }
    }

    // ==================== Sample Animations ====================

    private fun createSampleStickmanProject(): AnimationProject {
        val project = AnimationProject(
            id = "sample_stickman",
            title = "Project 1 (Stickman)",
            canvasRatio = CanvasRatio.RATIO_1_1,
            fps = 12
        )

        val w = 720
        val h = 720

        // Create 8 frames of stickman kicking a soccer ball
        for (f in 0 until 8) {
            val frame = AnimationFrame.createDefault(f)
            val bgLayer = frame.layers[0]
            val charLayer = frame.layers[1]

            // Draw character
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 14f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                style = Paint.Style.FILL
            }

            // Head
            canvas.drawCircle(360f, 220f, 60f, paint)
            // Smile
            canvas.drawArc(335f, 220f, 385f, 255f, 0f, 180f, false, paint.apply { strokeWidth = 8f })
            // Eyes
            canvas.drawCircle(345f, 210f, 6f, fillPaint)
            canvas.drawCircle(375f, 210f, 6f, fillPaint)

            paint.strokeWidth = 14f
            // Body
            canvas.drawLine(360f, 280f, 360f, 440f, paint)

            // Arms
            canvas.drawLine(360f, 320f, 290f, 380f, paint)
            canvas.drawLine(360f, 320f, 430f, 360f - (f * 4), paint)

            // Legs (kicking animation)
            canvas.drawLine(360f, 440f, 320f, 580f, paint) // Stand leg
            val kickProgress = (f / 7f)
            val kickX = 360f + 70f + (kickProgress * 90f)
            val kickY = 440f + 140f - (kickProgress * 120f)
            canvas.drawLine(360f, 440f, kickX, kickY, paint)

            // Soccer ball
            val ballX = 460f + (f * 25f)
            val ballY = 560f - (f * 35f) + (f * f * 2f)
            val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E65100")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(ballX, ballY, 28f, ballPaint)
            canvas.drawCircle(ballX, ballY, 28f, paint.apply { strokeWidth = 6f })

            saveLayerBitmap(project.id, frame.id, charLayer.id, bmp)
            saveFrameThumbnail(project.id, frame.id, bmp)

            if (f == 0) {
                saveProjectThumbnail(project.id, bmp)
            }
            project.frames.add(frame)
        }

        val pDir = File(projectsDir, project.id).apply { if (!exists()) mkdirs() }
        File(pDir, "project.json").writeText(gson.toJson(project))
        return project
    }

    private fun createSampleBouncingBallProject(): AnimationProject {
        val project = AnimationProject(
            id = "sample_bouncing_ball",
            title = "Project 2 (Bouncing Ball)",
            canvasRatio = CanvasRatio.RATIO_1_1,
            fps = 12
        )

        val w = 720
        val h = 720
        val ballHeights = listOf(200f, 250f, 340f, 460f, 580f, 460f, 340f, 250f)
        val ballScalesY = listOf(1f, 1f, 1.1f, 1.2f, 0.7f, 1.2f, 1.1f, 1f)

        for (f in ballHeights.indices) {
            val frame = AnimationFrame.createDefault(f)
            val charLayer = frame.layers[1]

            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            // Ground line
            val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#424242")
                strokeWidth = 8f
                style = Paint.Style.STROKE
            }
            canvas.drawLine(100f, 620f, 620f, 620f, groundPaint)

            // Ball
            val y = ballHeights[f]
            val scaleY = ballScalesY[f]
            val scaleX = if (scaleY < 1f) 1.3f else 0.95f

            val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#F4BE19")
                style = Paint.Style.FILL
            }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#262626")
                strokeWidth = 8f
                style = Paint.Style.STROKE
            }

            canvas.save()
            canvas.translate(360f, y)
            canvas.scale(scaleX, scaleY)
            canvas.drawCircle(0f, 0f, 45f, ballPaint)
            canvas.drawCircle(0f, 0f, 45f, borderPaint)
            canvas.restore()

            saveLayerBitmap(project.id, frame.id, charLayer.id, bmp)
            saveFrameThumbnail(project.id, frame.id, bmp)

            if (f == 0) {
                saveProjectThumbnail(project.id, bmp)
            }
            project.frames.add(frame)
        }

        val pDir = File(projectsDir, project.id).apply { if (!exists()) mkdirs() }
        File(pDir, "project.json").writeText(gson.toJson(project))
        return project
    }

    private fun createSamplePhotoPortraitProject(): AnimationProject {
        val project = AnimationProject(
            id = "sample_photo_portrait",
            title = "Project 3 (Boy Portrait)",
            canvasRatio = CanvasRatio.RATIO_1_1,
            fps = 1,
            projectType = AnimationProject.TYPE_IMAGE
        )

        val w = 720
        val h = 720
        val frame = AnimationFrame.createDefault(0)
        val charLayer = frame.layers[1]

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F1F1F")
            strokeWidth = 12f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F1F1F")
            style = Paint.Style.FILL
        }

        // Head
        canvas.drawCircle(360f, 340f, 90f, paint)

        // Hair arc filled
        val hairPath = android.graphics.Path().apply {
            moveTo(260f, 300f)
            quadTo(360f, 180f, 460f, 300f)
            close()
        }
        canvas.drawPath(hairPath, fillPaint)

        // Eyes
        canvas.drawCircle(330f, 330f, 8f, fillPaint)
        canvas.drawCircle(390f, 330f, 8f, fillPaint)

        // Smile
        canvas.drawArc(320f, 330f, 400f, 370f, 15f, 150f, false, paint.apply { strokeWidth = 8f })

        // Body and shoulders
        paint.strokeWidth = 12f
        canvas.drawLine(360f, 430f, 360f, 530f, paint)
        val shoulderPath = android.graphics.Path().apply {
            moveTo(240f, 570f)
            quadTo(360f, 500f, 480f, 570f)
        }
        canvas.drawPath(shoulderPath, paint)

        saveLayerBitmap(project.id, frame.id, charLayer.id, bmp)
        saveFrameThumbnail(project.id, frame.id, bmp)
        saveProjectThumbnail(project.id, bmp)
        project.frames.add(frame)

        val pDir = File(projectsDir, project.id).apply { if (!exists()) mkdirs() }
        File(pDir, "project.json").writeText(gson.toJson(project))
        return project
    }
}

