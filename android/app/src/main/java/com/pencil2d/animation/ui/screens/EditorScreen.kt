package com.pencil2d.animation.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pencil2d.animation.data.model.CanvasRatio
import com.pencil2d.animation.data.model.DrawingTool
import com.pencil2d.animation.data.model.ExportFormat
import com.pencil2d.animation.data.model.ExportOption
import com.pencil2d.animation.data.model.StrokePoint
import com.pencil2d.animation.engine.FrameCompositor
import com.pencil2d.animation.ui.components.ExportDialog
import com.pencil2d.animation.ui.viewmodel.EditorViewModel

enum class EditorTab {
    BRUSH, ERASER, CANVAS, SETTINGS
}

@Composable
fun EditorScreen(
    projectId: String,
    viewModel: EditorViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val importPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importImageFromUri(uri, context)
        }
    }

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    val project by viewModel.project.collectAsState()
    val currentFrameIndex by viewModel.currentFrameIndex.collectAsState()
    val currentLayerIndex by viewModel.currentLayerIndex.collectAsState()
    val selectedTool by viewModel.selectedTool.collectAsState()
    val strokeColor by viewModel.strokeColor.collectAsState()
    val strokeWidth by viewModel.strokeWidth.collectAsState()
    val onionSkinConfig by viewModel.onionSkinConfig.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackFrameIndex by viewModel.playbackFrameIndex.collectAsState()
    val canvasVersion by viewModel.canvasVersion.collectAsState()

    val isExporting by viewModel.isExporting.collectAsState()
    val exportMessage by viewModel.exportMessage.collectAsState()

    var activeTab by remember { mutableStateOf(EditorTab.BRUSH) }
    var showExportDialog by remember { mutableStateOf(false) }

    val currentStrokePoints = remember { mutableStateListOf<StrokePoint>() }

    val PinkPrimary = Color(0xFFFF2D55)

    Scaffold(
        topBar = {
            // Top Bar matching Phone 1, 2, 3, 4
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button (left)
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1C1C1E)
                    )
                }

                // Center Pill with Onion Skin & Undo (matching screenshot)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (project?.isPhotoProject != true) {
                            IconButton(
                                onClick = { viewModel.toggleOnionSkin() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Layers,
                                    contentDescription = "Onion Skin",
                                    tint = if (onionSkinConfig.isEnabled) Color(0xFF1C1C1E) else Color(0xFF999999),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(16.dp)
                                    .background(Color(0xFFE0E0E0))
                            )
                        }

                        IconButton(
                            onClick = { viewModel.undo() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = Color(0xFF666666),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.redo() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo",
                                tint = Color(0xFF666666),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Next or Save Photo Button (right)
                if (project?.isPhotoProject == true) {
                    Button(
                        onClick = {
                            viewModel.exportProject(
                                ExportOption(format = ExportFormat.PNG_SEQUENCE, fps = 1, loopCount = 1),
                                context
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    TextButton(onClick = { showExportDialog = true }) {
                        Text(
                            text = "Next",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Column {
                // Tool Bottom Sheets
                when (activeTab) {
                    EditorTab.BRUSH -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                            shadowElevation = 4.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Brush",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1C1C1E),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${strokeWidth.toInt()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Slider(
                                        value = strokeWidth,
                                        onValueChange = { viewModel.setStrokeWidth(it) },
                                        valueRange = 2f..60f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color(0xFF4CD964),
                                            activeTrackColor = PinkPrimary
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                // Preset colors row
                                val colors = listOf(
                                    AndroidColor.BLACK,
                                    AndroidColor.parseColor("#4F75FF"),
                                    AndroidColor.parseColor("#7C4DFF"),
                                    AndroidColor.parseColor("#D81B60"),
                                    AndroidColor.parseColor("#26A69A"),
                                    AndroidColor.parseColor("#FB8C00"),
                                    AndroidColor.parseColor("#C0CA33")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    colors.forEach { c ->
                                        val isSel = strokeColor == c
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(c))
                                                .border(
                                                    width = if (isSel) 3.dp else 1.dp,
                                                    color = if (isSel) PinkPrimary else Color(0x33000000),
                                                    shape = CircleShape
                                                )
                                                .clickable { viewModel.setColor(c) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    EditorTab.CANVAS -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                            shadowElevation = 4.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Canvas Ratio",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1C1C1E),
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CanvasRatio.entries.forEach { r ->
                                        val isSel = project?.canvasRatio == r
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSel) Color(0xFFFFE8EC) else Color(0xFFF7F7F8))
                                                .border(
                                                    width = if (isSel) 2.dp else 1.dp,
                                                    color = if (isSel) PinkPrimary else Color(0xFFE5E5EA),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    viewModel.setCanvasRatio(r)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(
                                                        width = when (r) {
                                                            CanvasRatio.RATIO_16_9 -> 36.dp
                                                            CanvasRatio.RATIO_4_3 -> 30.dp
                                                            CanvasRatio.RATIO_9_16 -> 20.dp
                                                            else -> 26.dp
                                                        },
                                                        height = when (r) {
                                                            CanvasRatio.RATIO_9_16 -> 36.dp
                                                            CanvasRatio.RATIO_16_9 -> 20.dp
                                                            CanvasRatio.RATIO_4_3 -> 24.dp
                                                            else -> 26.dp
                                                        }
                                                    )
                                                    .background(
                                                        if (isSel) PinkPrimary.copy(alpha = 0.25f) else Color(0xFFE0E0E0),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        1.5.dp,
                                                        if (isSel) PinkPrimary else Color(0xFF8E8E93),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = r.label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) PinkPrimary else Color(0xFF1C1C1E)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    EditorTab.SETTINGS -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White,
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                            shadowElevation = 4.dp
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Animation FPS",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1C1C1E)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            listOf(6, 8, 12, 24).forEach { fps ->
                                                val isSel = project?.fps == fps
                                                FilterChip(
                                                    selected = isSel,
                                                    onClick = { viewModel.setFps(fps) },
                                                    label = {
                                                        Text(
                                                            "$fps",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = PinkPrimary,
                                                        selectedLabelColor = Color.White,
                                                        containerColor = Color(0xFFF2F2F7),
                                                        labelColor = Color(0xFF1C1C1E)
                                                    ),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                            }
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "Onion Skin",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1C1C1E)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = { viewModel.toggleOnionSkin() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (onionSkinConfig.isEnabled) Color(0xFFE8F5E9) else Color(0xFFF2F2F7)
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = if (onionSkinConfig.isEnabled) "ON" else "OFF",
                                                color = if (onionSkinConfig.isEnabled) Color(0xFF2E7D32) else Color.Gray,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = { importPhotoLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF1A73E8)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1A73E8))
                                ) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Import Photo to Canvas", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    else -> {}
                }

                // 4-Tab Bottom Dock (matching all screenshots!)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(58.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DockTab(
                            icon = Icons.Default.Edit,
                            label = "Brush",
                            isSelected = activeTab == EditorTab.BRUSH,
                            onClick = {
                                activeTab = EditorTab.BRUSH
                                viewModel.setTool(DrawingTool.PEN)
                            }
                        )
                        DockTab(
                            icon = Icons.Default.CleaningServices,
                            label = "Eraser",
                            isSelected = activeTab == EditorTab.ERASER,
                            onClick = {
                                activeTab = EditorTab.ERASER
                                viewModel.setTool(DrawingTool.ERASER)
                            }
                        )
                        DockTab(
                            icon = Icons.Default.CropSquare,
                            label = "Canvas",
                            isSelected = activeTab == EditorTab.CANVAS,
                            onClick = { activeTab = EditorTab.CANVAS }
                        )
                        DockTab(
                            icon = Icons.Default.Settings,
                            label = "Settings",
                            isSelected = activeTab == EditorTab.SETTINGS,
                            onClick = { activeTab = EditorTab.SETTINGS }
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFEFEFEF)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main Canvas Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                val proj = project
                if (proj != null) {
                    val aspectRatio = proj.canvasRatio.aspectRatio

                    Box(
                        modifier = Modifier
                            .aspectRatio(aspectRatio)
                            .fillMaxSize()
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                    ) {
                        val prevBmp = remember(currentFrameIndex, canvasVersion, onionSkinConfig.isEnabled) {
                            if (onionSkinConfig.isEnabled) viewModel.getPreviousFrameBitmap() else null
                        }
                        val compositeBmp = remember(if (isPlaying) playbackFrameIndex else currentFrameIndex, canvasVersion) {
                            viewModel.getCompositeBitmap(if (isPlaying) playbackFrameIndex else currentFrameIndex)
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(selectedTool, currentFrameIndex, currentLayerIndex) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val bmp = viewModel.getActiveBitmap() ?: return@detectDragGestures
                                            val scaleX = bmp.width.toFloat() / size.width
                                            val scaleY = bmp.height.toFloat() / size.height
                                            viewModel.onStrokeStart()
                                            currentStrokePoints.clear()
                                            val pt = StrokePoint(offset.x * scaleX, offset.y * scaleY)
                                            currentStrokePoints.add(pt)
                                            viewModel.applyStroke(currentStrokePoints)
                                        },
                                        onDrag = { change, _ ->
                                            val bmp = viewModel.getActiveBitmap() ?: return@detectDragGestures
                                            val scaleX = bmp.width.toFloat() / size.width
                                            val scaleY = bmp.height.toFloat() / size.height
                                            val pt = StrokePoint(change.position.x * scaleX, change.position.y * scaleY)
                                            currentStrokePoints.add(pt)
                                            viewModel.applyStroke(currentStrokePoints)
                                            change.consume()
                                        },
                                        onDragEnd = {
                                            currentStrokePoints.clear()
                                            viewModel.onStrokeFinished()
                                        },
                                        onDragCancel = {
                                            currentStrokePoints.clear()
                                            viewModel.onStrokeFinished()
                                        }
                                    )
                                }
                        ) {
                            drawIntoCanvas { composeCanvas ->
                                val nativeCanvas = composeCanvas.nativeCanvas
                                val targetRect = android.graphics.Rect(0, 0, size.width.toInt(), size.height.toInt())

                                // 1. Draw Onion Skin (Previous frame faint overlay)
                                if (!isPlaying && prevBmp != null) {
                                    val paint = android.graphics.Paint().apply { alpha = 70 }
                                    val srcRect = android.graphics.Rect(0, 0, prevBmp.width, prevBmp.height)
                                    nativeCanvas.drawBitmap(prevBmp, srcRect, targetRect, paint)
                                }

                                // 2. Draw Active/Playback frame
                                if (compositeBmp != null) {
                                    val srcRect = android.graphics.Rect(0, 0, compositeBmp.width, compositeBmp.height)
                                    nativeCanvas.drawBitmap(compositeBmp, srcRect, targetRect, null)
                                }
                            }
                        }

                        // Center Play Button Overlay (matching Phone 4, only for multi-frame animations)
                        if (project?.isPhotoProject != true && (project?.frameCount ?: 0) > 1) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(58.dp)
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color(0xEEFFFFFF))
                                    .clickable { viewModel.togglePlayback() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color(0xFF333333),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (project?.isPhotoProject == true) {
                // Dedicated Photo Drawing Dock - Aligned cleanly in a straight row
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F0FE)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("🖼️", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Photo & Art",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A73E8),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { importPhotoLauncher.launch("image/*") },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1A73E8))
                            ) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = Color(0xFF1A73E8)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Replace",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1A73E8),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.exportProject(
                                        ExportOption(format = ExportFormat.PNG_SEQUENCE, fps = 1, loopCount = 1),
                                        context
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8))
                            ) {
                                Icon(
                                    Icons.Default.SaveAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Save PNG",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            } else {
                // Timeline Frame Scrubber (matching Phone 1: for multi-frame animations)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Square Play Button on Left (matching screenshot 1)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE5E5EA))
                                .clickable { viewModel.togglePlayback() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color(0xFF555555),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Frames Strip
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val frames = project?.frames ?: emptyList()
                            itemsIndexed(frames) { index, _ ->
                                val isSelected = index == (if (isPlaying) playbackFrameIndex else currentFrameIndex)
                                val thumb = remember(index, canvasVersion) {
                                    viewModel.getCompositeBitmap(index)
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { viewModel.selectFrame(index) }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) PinkPrimary else Color(0xFFD1D1D6),
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (thumb != null) {
                                            Image(
                                                bitmap = thumb.asImageBitmap(),
                                                contentDescription = "Frame $index",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) PinkPrimary else Color(0xFF8E8E93)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Plus Button (+)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFBBBBBB), RoundedCornerShape(8.dp))
                                .clickable { viewModel.addBlankFrame() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Frame",
                                tint = Color(0xFF777777),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExportDialog && project != null) {
        ExportDialog(
            currentFps = project!!.fps,
            isExporting = isExporting,
            exportMessage = exportMessage,
            onExport = { opt ->
                viewModel.exportProject(opt, context)
            },
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun DockTab(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF1C1C1E) else Color(0xFF8E8E93),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF1C1C1E) else Color(0xFF8E8E93)
        )
    }
}
