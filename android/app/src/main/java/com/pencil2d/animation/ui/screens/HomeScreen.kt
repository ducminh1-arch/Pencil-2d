package com.pencil2d.animation.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pencil2d.animation.data.model.AnimationProject
import com.pencil2d.animation.data.repository.ProjectRepository
import com.pencil2d.animation.ui.theme.PencilYellowDark
import com.pencil2d.animation.ui.theme.PencilYellowPrimary
import com.pencil2d.animation.ui.viewmodel.ProjectListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ProjectListViewModel,
    onOpenProject: (String) -> Unit
) {
    val projects by viewModel.filteredProjects.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showNewProjectDialog by remember { mutableStateOf(false) }

    // Launcher for importing photo from gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importPhotoFromUri(uri) { newId ->
                onOpenProject(newId)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAF8F2))
    ) {
        // Graph Paper Grid Background (matching App Store reference screenshot 5)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 28.dp.toPx()
            val lineColor = Color(0xFFE8E4D8)
            var x = 0f
            while (x < size.width) {
                drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Centered Title matching App Store
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MAKE EASY ANIMATION",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color(0xFF1C1C1E)
                )
            }

            // Categories Filter Bar with clean quick-import chip (All in single row, no scroll!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterPill(
                        text = "All",
                        isSelected = selectedFilter == "all",
                        selectedColor = PencilYellowPrimary,
                        selectedTextColor = Color.Black,
                        onClick = { viewModel.setFilter("all") }
                    )
                    FilterPill(
                        text = "🎬 Animation",
                        isSelected = selectedFilter == "animation",
                        selectedColor = Color(0xFFFF2D55),
                        selectedTextColor = Color.White,
                        onClick = { viewModel.setFilter("animation") }
                    )
                    FilterPill(
                        text = "🖼️ Photo",
                        isSelected = selectedFilter == "image",
                        selectedColor = Color(0xFF1A73E8),
                        selectedTextColor = Color.White,
                        onClick = { viewModel.setFilter("image") }
                    )
                }

                // Quick Import Action Button
                Surface(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE8F0FE),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1A73E8))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Import Photo",
                            tint = Color(0xFF1A73E8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ Import",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A73E8)
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PencilYellowDark)
                }
            } else if (projects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 60.dp)
                    ) {
                        Text(text = if (selectedFilter == "image") "🖼️" else "📂", fontSize = 42.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedFilter == "image") "No photo projects yet" else "No projects in this category",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF666666)
                        )
                        if (selectedFilter == "image") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Import Photo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(projects, key = { it.id }) { project ->
                        NotebookProjectCard(
                            project = project,
                            onClick = { onOpenProject(project.id) },
                            onDelete = { viewModel.deleteProject(project.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Button (+)
        FloatingActionButton(
            onClick = { showNewProjectDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(54.dp),
            containerColor = Color(0xFFF4BE19),
            contentColor = Color(0xFF1C1C1E),
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(4.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "New Project", modifier = Modifier.size(26.dp))
        }
    }

    if (showNewProjectDialog) {
        NewProjectDialog(
            onDismiss = { showNewProjectDialog = false },
            onCreate = { title, ratio, fps, type ->
                showNewProjectDialog = false
                viewModel.createProject(title, ratio, fps, type) { newId ->
                    onOpenProject(newId)
                }
            }
        )
    }
}

@Composable
fun NotebookProjectCard(
    project: AnimationProject,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ProjectRepository(context) }
    var menuExpanded by remember { mutableStateOf(false) }

    val thumbnailBitmap = remember(project.id, project.modifiedAt) {
        val thumbFile = repository.getProjectThumbnailFile(project.id)
        if (thumbFile.exists()) {
            BitmapFactory.decodeFile(thumbFile.absolutePath)
        } else {
            null
        }
    }

    val isPhoto = project.isPhotoProject

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFF222222), RoundedCornerShape(12.dp))
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Card Title (e.g. Project 1)
            Text(
                text = project.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.padding(start = 10.dp, top = 8.dp, bottom = 2.dp),
                maxLines = 1
            )

            // Canvas Thumbnail Box with Type Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap.asImageBitmap(),
                        contentDescription = project.title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.Draw,
                        contentDescription = "Thumbnail",
                        modifier = Modifier.size(36.dp),
                        tint = Color(0xFFB0A99A)
                    )
                }

                // Type Badge in top-left corner
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isPhoto) Color(0xD91A73E8) else Color(0xD9FF2D55))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isPhoto) "🖼️ PHOTO" else "🎬 ANIMATION",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Bottom Info: Type/FPS tag and menu dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isPhoto) "PNG • 1 Frame" else "MP4 : ${project.fps} fps",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPhoto) Color(0xFF1A73E8) else Color(0xFF777777)
                )

                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color(0xFF888888),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isPhoto) "Open Photo" else "Open Animation") },
                            onClick = {
                                menuExpanded = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterPill(
    text: String,
    isSelected: Boolean,
    selectedColor: Color,
    selectedTextColor: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) selectedColor else Color(0xFFF0F0F0),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDDDDD))
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) selectedTextColor else Color(0xFF333333),
                maxLines = 1
            )
        }
    }
}
