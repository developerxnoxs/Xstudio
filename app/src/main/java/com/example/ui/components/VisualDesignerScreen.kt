package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.engine.ComponentType
import com.example.engine.VisualUiNode
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualDesignerScreen(
    nodes: List<VisualUiNode>,
    selectedNodeId: String?,
    onSelectNode: (String?) -> Unit,
    onAddNode: (ComponentType) -> Unit,
    onUpdateNode: (VisualUiNode) -> Unit,
    onDeleteNode: (String) -> Unit,
    onSyncCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedNode = nodes.find { it.id == selectedNodeId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        // Top Component Palette Bar
        Surface(
            color = StudioSurface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Component Palette (Tap to Add)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    FilledTonalButton(
                        onClick = onSyncCode,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync to Kotlin", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ComponentType.values()) { type ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StudioSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAddNode(type) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(type.displayName, fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Live Visual Canvas Surface
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(StudioSurface)
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "📱 Interactive UI Canvas Preview",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StudioGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(nodes, key = { it.id }) { node ->
                    val isSelected = node.id == selectedNodeId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) StudioGreen else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectNode(if (isSelected) null else node.id) }
                            .padding(4.dp)
                    ) {
                        RenderVisualNodeItem(node = node)
                    }
                }
            }
        }

        // Property Inspector Drawer (Bottom Sheet / Panel)
        if (selectedNode != null) {
            Surface(
                color = StudioSurface,
                tonalElevation = 6.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Inspector: ${selectedNode.type.displayName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = StudioGreen
                        )
                        Row {
                            IconButton(
                                onClick = { onDeleteNode(selectedNode.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StudioRed, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { onSelectNode(null) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = selectedNode.label,
                            onValueChange = { onUpdateNode(selectedNode.copy(label = it)) },
                            label = { Text("Label / Text") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        if (selectedNode.type == ComponentType.CARD || selectedNode.type == ComponentType.ELEVATED_CARD) {
                            OutlinedTextField(
                                value = selectedNode.secondaryText,
                                onValueChange = { onUpdateNode(selectedNode.copy(secondaryText = it)) },
                                label = { Text("Subtitle") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }

                    if (selectedNode.type == ComponentType.SLIDER) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Default Value: ${(selectedNode.sliderValue * 100).toInt()}%", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Slider(
                                value = selectedNode.sliderValue,
                                onValueChange = { onUpdateNode(selectedNode.copy(sliderValue = it)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderVisualNodeItem(node: VisualUiNode) {
    when (node.type) {
        ComponentType.TEXT -> {
            Text(
                text = node.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        ComponentType.BUTTON -> {
            Button(
                onClick = { /* Interactive */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = StudioGreen, contentColor = Color(0xFF003919))
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(node.label, fontWeight = FontWeight.Bold)
            }
        }
        ComponentType.OUTLINED_BUTTON -> {
            OutlinedButton(
                onClick = { /* Interactive */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(node.label)
            }
        }
        ComponentType.ELEVATED_CARD, ComponentType.CARD -> {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(node.cornerRadiusDp.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = StudioSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(node.paddingDp.dp)) {
                    Text(node.label, fontWeight = FontWeight.Bold, color = Color.White)
                    if (node.secondaryText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(node.secondaryText, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            }
        }
        ComponentType.TEXT_FIELD -> {
            var text by remember { mutableStateOf(node.secondaryText) }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(node.label) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StudioGreen,
                    unfocusedBorderColor = StudioBorder
                )
            )
        }
        ComponentType.SWITCH -> {
            var checked by remember { mutableStateOf(node.isChecked) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(node.label, color = Color.White)
                Switch(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = StudioGreen)
                )
            }
        }
        ComponentType.CHECKBOX -> {
            var checked by remember { mutableStateOf(node.isChecked) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    colors = CheckboxDefaults.colors(checkedColor = StudioGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(node.label, color = Color.White)
            }
        }
        ComponentType.SLIDER -> {
            var value by remember { mutableFloatStateOf(node.sliderValue) }
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(node.label, color = Color.White, fontSize = 12.sp)
                    Text("${(value * 100).toInt()}%", color = StudioGreen, fontSize = 12.sp)
                }
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    colors = SliderDefaults.colors(thumbColor = StudioGreen, activeTrackColor = StudioGreen)
                )
            }
        }
        ComponentType.FAB -> {
            ExtendedFloatingActionButton(
                onClick = { /* FAB */ },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(node.label) },
                containerColor = StudioGreen,
                contentColor = Color(0xFF003919)
            )
        }
        ComponentType.PROGRESS_BAR -> {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = StudioGreen,
                trackColor = StudioSurfaceVariant
            )
        }
        ComponentType.DIVIDER -> {
            HorizontalDivider(color = StudioBorder, modifier = Modifier.padding(vertical = 6.dp))
        }
        ComponentType.CHIP -> {
            AssistChip(
                onClick = { /* Chip */ },
                label = { Text(node.label) },
                leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(16.dp)) },
                colors = AssistChipDefaults.assistChipColors(containerColor = StudioSurfaceVariant)
            )
        }
    }
}
