package com.pencil2d.animation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.pencil2d.animation.data.model.DrawingLayer
import com.pencil2d.animation.ui.theme.PencilYellowDark
import com.pencil2d.animation.ui.theme.PencilYellowPrimary

@Composable
fun LayersDialog(
    layers: List<DrawingLayer>,
    activeLayerIndex: Int,
    onSelectLayer: (Int) -> Unit,
    onToggleVisibility: (Int) -> Unit,
    onToggleLock: (Int) -> Unit,
    onOpacityChanged: (Int, Float) -> Unit,
    onAddLayer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newLayerName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
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
                        text = "LAYERS",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Row {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Layer")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Layers listed top to bottom (highest index drawn on top)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(layers.reversed()) { revIndex, layer ->
                        val actualIndex = layers.size - 1 - revIndex
                        val isSelected = actualIndex == activeLayerIndex

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectLayer(actualIndex) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PencilYellowPrimary.copy(alpha = 0.2f) else Color(0xFFF9F7F1)
                            ),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                                width = 2.dp,
                                brush = androidx.compose.ui.graphics.SolidColor(PencilYellowDark)
                            ) else null
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Layers,
                                            contentDescription = null,
                                            tint = if (isSelected) PencilYellowDark else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = layer.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Visibility button
                                        IconButton(
                                            onClick = { onToggleVisibility(actualIndex) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Visibility",
                                                tint = if (layer.isVisible) Color(0xFF333333) else Color.LightGray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Lock button
                                        IconButton(
                                            onClick = { onToggleLock(actualIndex) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                                contentDescription = "Lock",
                                                tint = if (layer.isLocked) Color.Red else Color.LightGray,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                // Opacity slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Opacity ${(layer.opacity * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.width(75.dp)
                                    )
                                    Slider(
                                        value = layer.opacity,
                                        onValueChange = { onOpacityChanged(actualIndex, it) },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.weight(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = PencilYellowDark,
                                            activeTrackColor = PencilYellowPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PencilYellowPrimary)
                ) {
                    Text("Done", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Layer") },
            text = {
                OutlinedTextField(
                    value = newLayerName,
                    onValueChange = { newLayerName = it },
                    label = { Text("Layer Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onAddLayer(newLayerName)
                    newLayerName = ""
                    showAddDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
