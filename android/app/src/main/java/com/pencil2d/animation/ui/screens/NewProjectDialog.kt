package com.pencil2d.animation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pencil2d.animation.data.model.AnimationProject
import com.pencil2d.animation.data.model.CanvasRatio
import com.pencil2d.animation.ui.theme.PencilYellowDark
import com.pencil2d.animation.ui.theme.PencilYellowPrimary

@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, ratio: CanvasRatio, fps: Int, type: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(AnimationProject.TYPE_ANIMATION) }
    var title by remember {
        mutableStateOf(
            if (selectedType == AnimationProject.TYPE_ANIMATION) "Animation ${System.currentTimeMillis() % 1000}"
            else "Drawing ${System.currentTimeMillis() % 1000}"
        )
    }
    var selectedRatio by remember { mutableStateOf(CanvasRatio.RATIO_1_1) }
    var selectedFps by remember { mutableIntStateOf(12) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedType == AnimationProject.TYPE_ANIMATION) "NEW ANIMATION" else "NEW DRAWING / PHOTO",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Project Type Selector: Animation vs Photo/Drawing
                Text(
                    text = "PROJECT TYPE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Animation Card
                    val isAnim = selectedType == AnimationProject.TYPE_ANIMATION
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isAnim) PencilYellowPrimary.copy(alpha = 0.25f)
                                else Color(0xFFF3EFE6)
                            )
                            .border(
                                width = if (isAnim) 2.dp else 1.dp,
                                color = if (isAnim) PencilYellowDark else Color(0xFFDED9CE),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedType = AnimationProject.TYPE_ANIMATION
                                title = "Animation ${System.currentTimeMillis() % 1000}"
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🎬", fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Animation",
                                fontWeight = if (isAnim) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isAnim) PencilYellowDark else Color(0xFF333333)
                            )
                            Text(text = "Multi-frame", fontSize = 10.sp, color = Color(0xFF777777))
                        }
                    }

                    // Photo/Drawing Card
                    val isPhoto = selectedType == AnimationProject.TYPE_IMAGE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isPhoto) Color(0xFFE8F0FE)
                                else Color(0xFFF3EFE6)
                            )
                            .border(
                                width = if (isPhoto) 2.dp else 1.dp,
                                color = if (isPhoto) Color(0xFF1A73E8) else Color(0xFFDED9CE),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedType = AnimationProject.TYPE_IMAGE
                                title = "Drawing ${System.currentTimeMillis() % 1000}"
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🖼️", fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Photo & Art",
                                fontWeight = if (isPhoto) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isPhoto) Color(0xFF1A73E8) else Color(0xFF333333)
                            )
                            Text(text = "Single frame", fontSize = 10.sp, color = Color(0xFF777777))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "CANVAS SIZE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Canvas Ratio Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CanvasRatio.entries.forEach { ratio ->
                        val isSelected = ratio == selectedRatio
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) PencilYellowPrimary.copy(alpha = 0.25f)
                                    else Color(0xFFF3EFE6)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PencilYellowDark else Color(0xFFDED9CE),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedRatio = ratio }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val (boxW, boxH) = when (ratio) {
                                    CanvasRatio.RATIO_1_1 -> 24.dp to 24.dp
                                    CanvasRatio.RATIO_16_9 -> 32.dp to 18.dp
                                    CanvasRatio.RATIO_4_3 -> 28.dp to 21.dp
                                    CanvasRatio.RATIO_9_16 -> 18.dp to 32.dp
                                }
                                Box(
                                    modifier = Modifier
                                        .size(boxW, boxH)
                                        .background(
                                            if (isSelected) PencilYellowDark else Color(0xFF8C867B),
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = ratio.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isSelected) PencilYellowDark else Color(0xFF333333)
                                )
                            }
                        }
                    }
                }

                if (selectedType == AnimationProject.TYPE_ANIMATION) {
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "FRAME RATE (FPS)",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(6, 12, 16, 24).forEach { fps ->
                            val isSelected = fps == selectedFps
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) PencilYellowPrimary
                                        else Color(0xFFF3EFE6)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) PencilYellowDark else Color(0xFFDED9CE),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedFps = fps }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$fps fps",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF1C1C1E) else Color(0xFF444444),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onCreate(title, selectedRatio, selectedFps, selectedType) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedType == AnimationProject.TYPE_ANIMATION) PencilYellowPrimary else Color(0xFF1A73E8)
                    )
                ) {
                    Text(
                        text = if (selectedType == AnimationProject.TYPE_ANIMATION) "Create Animation" else "Create Drawing Canvas",
                        color = if (selectedType == AnimationProject.TYPE_ANIMATION) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
