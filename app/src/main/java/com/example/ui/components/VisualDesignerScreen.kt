package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.ComponentType
import com.example.engine.LayoutDimensionType
import com.example.engine.VisualLayoutBridge
import com.example.engine.VisualUiNode
import com.example.ui.theme.*

enum class DeviceViewport(val label: String, val widthDp: Int, val heightDp: Int) {
    PHONE("Pixel 9 Pro (360dp)", 360, 680),
    FOLD("Pixel Fold (500dp)", 500, 600),
    TABLET("Pixel Tablet (720dp)", 720, 520)
}

enum class CanvasThemeMode(val label: String) {
    DARK("Dark Material 3"),
    LIGHT("Light Material 3"),
    BLUEPRINT("Blueprint Wireframe"),
    OLED_CONTRAST("High-Contrast OLED")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualDesignerScreen(
    nodes: List<VisualUiNode>,
    selectedNodeId: String?,
    onSelectNode: (String?) -> Unit,
    onAddNode: (ComponentType) -> Unit,
    onUpdateNode: (VisualUiNode) -> Unit,
    onDeleteNode: (String) -> Unit,
    onDuplicateNode: (String) -> Unit = {},
    onReorderNode: (Int, Int) -> Unit = { _, _ -> },
    designerMode: String = "XML",
    onToggleDesignerMode: (String) -> Unit = {},
    isRealtimeSyncEnabled: Boolean = true,
    onToggleRealtimeSync: (Boolean) -> Unit = {},
    onSaveXmlLayout: (String) -> Unit = {},
    onLoadFromActiveXml: () -> Unit = {},
    onSyncCode: () -> Unit,
    activeFileName: String? = null,
    modifier: Modifier = Modifier
) {
    val selectedNode = nodes.find { it.id == selectedNodeId }
    val clipboardManager = LocalClipboardManager.current

    var activeViewport by remember { mutableStateOf(DeviceViewport.PHONE) }
    var activeTheme by remember { mutableStateOf(CanvasThemeMode.DARK) }
    var isLandscape by remember { mutableStateOf(false) }
    var showHierarchyTree by remember { mutableStateOf(false) }
    var showCodePreviewModal by remember { mutableStateOf(false) }
    var showSaveLayoutDialog by remember { mutableStateOf(false) }
    var saveLayoutFileName by remember { mutableStateOf("activity_main.xml") }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    val canvasBg = when (activeTheme) {
        CanvasThemeMode.DARK -> Color(0xFF0F141C)
        CanvasThemeMode.LIGHT -> Color(0xFFF1F5F9)
        CanvasThemeMode.BLUEPRINT -> Color(0xFF0F2B48)
        CanvasThemeMode.OLED_CONTRAST -> Color.Black
    }

    val canvasCardBg = when (activeTheme) {
        CanvasThemeMode.DARK -> Color(0xFF1E293B)
        CanvasThemeMode.LIGHT -> Color(0xFFFFFFFF)
        CanvasThemeMode.BLUEPRINT -> Color(0xFF163E65)
        CanvasThemeMode.OLED_CONTRAST -> Color(0xFF121212)
    }

    val canvasTextColor = when (activeTheme) {
        CanvasThemeMode.DARK -> Color.White
        CanvasThemeMode.LIGHT -> Color(0xFF0F172A)
        CanvasThemeMode.BLUEPRINT -> Color(0xFF7DD3FC)
        CanvasThemeMode.OLED_CONTRAST -> Color.White
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        // TOP CONTROL & VIEWPORT TOOLBAR
        Surface(
            color = StudioSurface,
            tonalElevation = 4.dp,
            border = BorderStroke(0.5.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                // First Row: Mode selector, Viewports, Theme & Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mode Toggle: XML vs Compose
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StudioBackground,
                            border = BorderStroke(0.5.dp, StudioBorder)
                        ) {
                            Row(modifier = Modifier.padding(2.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (designerMode == "XML") StudioGreen.copy(alpha = 0.25f) else Color.Transparent,
                                    border = if (designerMode == "XML") BorderStroke(1.dp, StudioGreen) else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onToggleDesignerMode("XML") }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = if (designerMode == "XML") StudioGreen else Color.Gray,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Android XML",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (designerMode == "XML") StudioGreen else Color.LightGray
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (designerMode == "COMPOSE") StudioCyan.copy(alpha = 0.25f) else Color.Transparent,
                                    border = if (designerMode == "COMPOSE") BorderStroke(1.dp, StudioCyan) else null,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onToggleDesignerMode("COMPOSE") }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Code,
                                            contentDescription = null,
                                            tint = if (designerMode == "COMPOSE") StudioCyan else Color.Gray,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "Compose",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (designerMode == "COMPOSE") StudioCyan else Color.LightGray
                                        )
                                    }
                                }
                            }
                        }

                        // Realtime Sync Status Indicator
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isRealtimeSyncEnabled) StudioGreen.copy(alpha = 0.15f) else StudioSurfaceVariant,
                            border = BorderStroke(0.5.dp, if (isRealtimeSyncEnabled) StudioGreen else Color.Gray),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onToggleRealtimeSync(!isRealtimeSyncEnabled) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isRealtimeSyncEnabled) StudioGreen else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isRealtimeSyncEnabled) "Live Sync: ON" else "Sync: Paused",
                                    fontSize = 9.sp,
                                    color = if (isRealtimeSyncEnabled) StudioGreen else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Viewport & Tools Actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Hierarchy Tree Toggle
                        IconButton(
                            onClick = { showHierarchyTree = !showHierarchyTree },
                            modifier = Modifier
                                .size(28.dp)
                                .background(if (showHierarchyTree) StudioPurple.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(6.dp))
                        ) {
                            Icon(
                                Icons.Default.AccountTree,
                                contentDescription = "Component Tree",
                                tint = if (showHierarchyTree) StudioPurple else Color.LightGray,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        // Theme Mode Switcher
                        IconButton(
                            onClick = {
                                activeTheme = when (activeTheme) {
                                    CanvasThemeMode.DARK -> CanvasThemeMode.LIGHT
                                    CanvasThemeMode.LIGHT -> CanvasThemeMode.BLUEPRINT
                                    CanvasThemeMode.BLUEPRINT -> CanvasThemeMode.OLED_CONTRAST
                                    CanvasThemeMode.OLED_CONTRAST -> CanvasThemeMode.DARK
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                when (activeTheme) {
                                    CanvasThemeMode.DARK -> Icons.Default.DarkMode
                                    CanvasThemeMode.LIGHT -> Icons.Default.LightMode
                                    CanvasThemeMode.BLUEPRINT -> Icons.Default.Architecture
                                    CanvasThemeMode.OLED_CONTRAST -> Icons.Default.Contrast
                                },
                                contentDescription = activeTheme.label,
                                tint = if (activeTheme == CanvasThemeMode.BLUEPRINT) StudioCyan else StudioGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Rotate Orientation
                        IconButton(
                            onClick = { isLandscape = !isLandscape },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (isLandscape) Icons.Default.ScreenRotation else Icons.Default.StayCurrentPortrait,
                                contentDescription = "Rotate",
                                tint = if (isLandscape) StudioOrange else Color.LightGray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Code Preview Modal Button
                        IconButton(
                            onClick = { showCodePreviewModal = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.IntegrationInstructions,
                                contentDescription = "Inspect XML Code",
                                tint = StudioCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Save As XML Button
                        FilledTonalButton(
                            onClick = { showSaveLayoutDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = StudioGreen,
                                contentColor = Color(0xFF003919)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save XML", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Viewport selector tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DeviceViewport.values().forEach { vp ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (activeViewport == vp) StudioGreen.copy(alpha = 0.2f) else StudioSurfaceVariant,
                                border = if (activeViewport == vp) BorderStroke(1.dp, StudioGreen) else null,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { activeViewport = vp }
                            ) {
                                Text(
                                    text = vp.label,
                                    fontSize = 9.sp,
                                    fontWeight = if (activeViewport == vp) FontWeight.Bold else FontWeight.Normal,
                                    color = if (activeViewport == vp) StudioGreen else Color.LightGray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    // Load from active file action
                    if (activeFileName?.endsWith(".xml", ignoreCase = true) == true) {
                        TextButton(
                            onClick = onLoadFromActiveXml,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            modifier = Modifier.height(22.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Load from $activeFileName", fontSize = 9.sp, color = StudioCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Component Palette Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Palette:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(ComponentType.values()) { type ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StudioSurfaceVariant,
                                border = BorderStroke(0.5.dp, Color(0xFF2E384D)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onAddNode(type) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        when (type) {
                                            ComponentType.TEXT -> Icons.Default.TextFields
                                            ComponentType.BUTTON -> Icons.Default.SmartButton
                                            ComponentType.OUTLINED_BUTTON -> Icons.Default.CropFree
                                            ComponentType.CARD, ComponentType.ELEVATED_CARD -> Icons.Default.Layers
                                            ComponentType.TEXT_FIELD -> Icons.Default.Edit
                                            ComponentType.SWITCH -> Icons.Default.ToggleOn
                                            ComponentType.CHECKBOX -> Icons.Default.CheckBox
                                            ComponentType.SLIDER -> Icons.Default.Tune
                                            ComponentType.FAB -> Icons.Default.AddCircle
                                            ComponentType.PROGRESS_BAR -> Icons.Default.HourglassEmpty
                                            ComponentType.DIVIDER -> Icons.Default.HorizontalRule
                                            ComponentType.CHIP -> Icons.Default.Label
                                            ComponentType.IMAGE_VIEW -> Icons.Default.Image
                                        },
                                        contentDescription = null,
                                        tint = StudioGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(type.displayName, fontSize = 9.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // MAIN WORKSPACE (CANVAS + OPTIONAL HIERARCHY TREE)
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Collapsible Component Tree
            AnimatedVisibility(
                visible = showHierarchyTree,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = StudioSurfaceVariant,
                    border = BorderStroke(0.5.dp, StudioBorder),
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Component Tree",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioPurple
                            )
                            IconButton(
                                onClick = { showHierarchyTree = false },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Tree", tint = Color.Gray, modifier = Modifier.size(12.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Root container node
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StudioBackground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("LinearLayout (vertical)", fontSize = 10.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(nodes) { index, node ->
                                val isSelected = node.id == selectedNodeId
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelected) StudioGreen.copy(alpha = 0.2f) else Color.Transparent,
                                    border = if (isSelected) BorderStroke(1.dp, StudioGreen) else BorderStroke(0.5.dp, Color(0xFF2A3245)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable { onSelectNode(node.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("${index + 1}.", fontSize = 8.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                node.idName.ifBlank { node.label },
                                                fontSize = 10.sp,
                                                color = if (isSelected) StudioGreen else Color.White,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1
                                            )
                                        }

                                        Row {
                                            IconButton(
                                                onClick = { onDuplicateNode(node.id) },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.Gray, modifier = Modifier.size(11.dp))
                                            }
                                            IconButton(
                                                onClick = { onDeleteNode(node.id) },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StudioRed, modifier = Modifier.size(11.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CANVAS AREA
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = canvasBg,
                    border = BorderStroke(
                        width = if (activeTheme == CanvasThemeMode.BLUEPRINT) 2.dp else 1.5.dp,
                        color = if (activeTheme == CanvasThemeMode.BLUEPRINT) StudioCyan.copy(alpha = 0.6f) else StudioBorder
                    ),
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = if (isLandscape) 720.dp else activeViewport.widthDp.dp)
                        .fillMaxWidth(if (activeViewport == DeviceViewport.TABLET || isLandscape) 1f else 0.94f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    ) {
                        // Device Header simulation
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "9:41",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = canvasTextColor
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.SignalCellular4Bar, contentDescription = null, tint = canvasTextColor, modifier = Modifier.size(12.dp))
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = canvasTextColor, modifier = Modifier.size(12.dp))
                                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = canvasTextColor, modifier = Modifier.size(12.dp))
                            }
                        }

                        if (nodes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Widgets,
                                        contentDescription = null,
                                        tint = if (activeTheme == CanvasThemeMode.BLUEPRINT) StudioCyan else Color.Gray,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Drag & Drop XML Layout Canvas is Empty",
                                        color = if (activeTheme == CanvasThemeMode.BLUEPRINT) StudioCyan else Color.LightGray,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Tap or drag any component from the palette above to build layout",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(nodes, key = { _, node -> node.id }) { index, node ->
                                    val isSelected = node.id == selectedNodeId

                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // Selected Node Ribbon / Drag Handle & Dimension Tag
                                        if (isSelected) {
                                            Surface(
                                                color = StudioGreen,
                                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.DragHandle, contentDescription = "Drag Grip", tint = Color(0xFF003919), modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text(
                                                            text = "@+id/${node.idName} (${node.type.displayName})",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF003919),
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }

                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        // Reorder Up
                                                        if (index > 0) {
                                                            IconButton(
                                                                onClick = { onReorderNode(index, index - 1) },
                                                                modifier = Modifier.size(20.dp)
                                                            ) {
                                                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = Color(0xFF003919), modifier = Modifier.size(12.dp))
                                                            }
                                                        }
                                                        // Reorder Down
                                                        if (index < nodes.size - 1) {
                                                            IconButton(
                                                                onClick = { onReorderNode(index, index + 1) },
                                                                modifier = Modifier.size(20.dp)
                                                            ) {
                                                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = Color(0xFF003919), modifier = Modifier.size(12.dp))
                                                            }
                                                        }
                                                        // Duplicate
                                                        IconButton(
                                                            onClick = { onDuplicateNode(node.id) },
                                                            modifier = Modifier.size(20.dp)
                                                        ) {
                                                            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color(0xFF003919), modifier = Modifier.size(12.dp))
                                                        }
                                                        // Delete
                                                        IconButton(
                                                            onClick = { onDeleteNode(node.id) },
                                                            modifier = Modifier.size(20.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFF8B0000), modifier = Modifier.size(12.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Item Canvas Box
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(if (isSelected) RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp) else RoundedCornerShape(8.dp))
                                                .border(
                                                    width = if (isSelected) 2.dp else if (activeTheme == CanvasThemeMode.BLUEPRINT) 1.dp else 0.5.dp,
                                                    color = if (isSelected) StudioGreen else if (activeTheme == CanvasThemeMode.BLUEPRINT) StudioCyan.copy(alpha = 0.4f) else Color.Transparent,
                                                    shape = if (isSelected) RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp) else RoundedCornerShape(8.dp)
                                                )
                                                .clickable { onSelectNode(if (isSelected) null else node.id) }
                                                .padding(if (isSelected) 4.dp else 2.dp)
                                        ) {
                                            RenderVisualXmlNode(
                                                node = node,
                                                cardBg = canvasCardBg,
                                                textClr = canvasTextColor,
                                                isBlueprint = activeTheme == CanvasThemeMode.BLUEPRINT,
                                                onUpdateNode = onUpdateNode
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // PROPERTY INSPECTOR BOTTOM DRAWER (WHEN A NODE IS SELECTED)
        AnimatedVisibility(
            visible = selectedNode != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            if (selectedNode != null) {
                Surface(
                    color = StudioSurface,
                    tonalElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Inspector Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Attributes Inspector: ${selectedNode.type.displayName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "@+id/${selectedNode.idName}",
                                    fontSize = 10.sp,
                                    color = StudioCyan,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = { onDuplicateNode(selectedNode.id) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = StudioCyan, modifier = Modifier.size(15.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onDeleteNode(selectedNode.id) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StudioRed, modifier = Modifier.size(15.dp))
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onSelectNode(null) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Inspector", tint = Color.Gray, modifier = Modifier.size(15.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // SECTION 1: IDs & Text Attributes
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedNode.idName,
                                onValueChange = { onUpdateNode(selectedNode.copy(idName = it)) },
                                label = { Text("android:id", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = selectedNode.label,
                                onValueChange = { onUpdateNode(selectedNode.copy(label = it)) },
                                label = { Text("android:text / Label", fontSize = 10.sp) },
                                modifier = Modifier.weight(1.5f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Optional Secondary / Hint fields
                        if (selectedNode.type == ComponentType.CARD || selectedNode.type == ComponentType.ELEVATED_CARD || selectedNode.type == ComponentType.TEXT_FIELD) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = selectedNode.secondaryText,
                                    onValueChange = { onUpdateNode(selectedNode.copy(secondaryText = it)) },
                                    label = { Text("Subtitle / Default Value", fontSize = 10.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                if (selectedNode.type == ComponentType.TEXT_FIELD) {
                                    OutlinedTextField(
                                        value = selectedNode.hint,
                                        onValueChange = { onUpdateNode(selectedNode.copy(hint = it)) },
                                        label = { Text("android:hint", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // SECTION 2: Dimensions & Layout Width/Height
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Layout Width Selector
                            Column(modifier = Modifier.weight(1f)) {
                                Text("layout_width", fontSize = 9.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    LayoutDimensionType.values().forEach { dim ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (selectedNode.layoutWidthType == dim) StudioGreen.copy(alpha = 0.2f) else StudioSurfaceVariant,
                                            border = if (selectedNode.layoutWidthType == dim) BorderStroke(1.dp, StudioGreen) else null,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { onUpdateNode(selectedNode.copy(layoutWidthType = dim)) }
                                        ) {
                                            Text(
                                                dim.label.take(5),
                                                fontSize = 8.sp,
                                                color = if (selectedNode.layoutWidthType == dim) StudioGreen else Color.LightGray,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Layout Height Selector
                            Column(modifier = Modifier.weight(1f)) {
                                Text("layout_height", fontSize = 9.sp, color = Color.Gray)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    LayoutDimensionType.values().forEach { dim ->
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (selectedNode.layoutHeightType == dim) StudioCyan.copy(alpha = 0.2f) else StudioSurfaceVariant,
                                            border = if (selectedNode.layoutHeightType == dim) BorderStroke(1.dp, StudioCyan) else null,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { onUpdateNode(selectedNode.copy(layoutHeightType = dim)) }
                                        ) {
                                            Text(
                                                dim.label.take(5),
                                                fontSize = 8.sp,
                                                color = if (selectedNode.layoutHeightType == dim) StudioCyan else Color.LightGray,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // SECTION 3: Sliders (Padding, Margin, CornerRadius, Elevation)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Padding", fontSize = 9.sp, color = Color.LightGray)
                                    Text("${selectedNode.paddingDp}dp", fontSize = 9.sp, color = StudioGreen, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = selectedNode.paddingDp.toFloat(),
                                    onValueChange = { onUpdateNode(selectedNode.copy(paddingDp = it.toInt())) },
                                    valueRange = 0f..32f,
                                    modifier = Modifier.height(18.dp),
                                    colors = SliderDefaults.colors(thumbColor = StudioGreen, activeTrackColor = StudioGreen)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Margin", fontSize = 9.sp, color = Color.LightGray)
                                    Text("${selectedNode.marginDp}dp", fontSize = 9.sp, color = StudioCyan, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = selectedNode.marginDp.toFloat(),
                                    onValueChange = { onUpdateNode(selectedNode.copy(marginDp = it.toInt())) },
                                    valueRange = 0f..24f,
                                    modifier = Modifier.height(18.dp),
                                    colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioCyan)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Radius", fontSize = 9.sp, color = Color.LightGray)
                                    Text("${selectedNode.cornerRadiusDp}dp", fontSize = 9.sp, color = StudioPurple, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = selectedNode.cornerRadiusDp.toFloat(),
                                    onValueChange = { onUpdateNode(selectedNode.copy(cornerRadiusDp = it.toInt())) },
                                    valueRange = 0f..28f,
                                    modifier = Modifier.height(18.dp),
                                    colors = SliderDefaults.colors(thumbColor = StudioPurple, activeTrackColor = StudioPurple)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // SECTION 4: Quick Color Palette & Toggles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Quick Color Swatches
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Tint:", fontSize = 9.sp, color = Color.Gray)
                                val colors = listOf("#4CAF50", "#38BDF8", "#A855F7", "#F59E0B", "#EF4444", "#1E293B", "#FFFFFF")
                                colors.forEach { hex ->
                                    val isColorSelected = selectedNode.colorHex.equals(hex, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (hex) {
                                                    "#4CAF50" -> StudioGreen
                                                    "#38BDF8" -> StudioCyan
                                                    "#A855F7" -> StudioPurple
                                                    "#F59E0B" -> StudioOrange
                                                    "#EF4444" -> StudioRed
                                                    "#1E293B" -> Color(0xFF1E293B)
                                                    else -> Color.White
                                                }
                                            )
                                            .border(
                                                width = if (isColorSelected) 2.dp else 0.5.dp,
                                                color = if (isColorSelected) Color.White else Color.Gray,
                                                shape = CircleShape
                                            )
                                            .clickable { onUpdateNode(selectedNode.copy(colorHex = hex)) }
                                    )
                                }
                            }

                            // Enabled / Bold Switches
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Bold", fontSize = 9.sp, color = Color.LightGray)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Switch(
                                        checked = selectedNode.isBold,
                                        onCheckedChange = { onUpdateNode(selectedNode.copy(isBold = it)) },
                                        modifier = Modifier.height(20.dp),
                                        colors = SwitchDefaults.colors(checkedThumbColor = StudioGreen)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Enabled", fontSize = 9.sp, color = Color.LightGray)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Switch(
                                        checked = selectedNode.isEnabled,
                                        onCheckedChange = { onUpdateNode(selectedNode.copy(isEnabled = it)) },
                                        modifier = Modifier.height(20.dp),
                                        colors = SwitchDefaults.colors(checkedThumbColor = StudioCyan)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // LIVE XML / KOTLIN CODE PREVIEW MODAL
    if (showCodePreviewModal) {
        val generatedXml = remember(nodes) {
            VisualLayoutBridge.generateAndroidXmlLayout(nodes, "LinearLayout", "MyApp")
        }
        val generatedCompose = remember(nodes) {
            VisualLayoutBridge.generateComposeCode(nodes, "MyApp", "com.example")
        }
        val previewCode = if (designerMode == "XML") generatedXml else generatedCompose

        AlertDialog(
            onDismissRequest = { showCodePreviewModal = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (designerMode == "XML") "Live XML Source" else "Generated Compose Code",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(previewCode))
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = StudioGreen, modifier = Modifier.size(16.dp))
                    }
                }
            },
            text = {
                Surface(
                    color = Color(0xFF0D1117),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Text(
                        text = previewCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFFE6EDF3),
                        modifier = Modifier
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSyncCode()
                        showCodePreviewModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioGreen)
                ) {
                    Text("Apply to Editor", color = Color(0xFF003919), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCodePreviewModal = false }) {
                    Text("Close")
                }
            }
        )
    }

    // SAVE XML LAYOUT DIALOG
    if (showSaveLayoutDialog) {
        AlertDialog(
            onDismissRequest = { showSaveLayoutDialog = false },
            title = { Text("Save XML Layout to Project", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter layout file name in res/layout/:", fontSize = 11.sp, color = Color.LightGray)
                    OutlinedTextField(
                        value = saveLayoutFileName,
                        onValueChange = { saveLayoutFileName = it },
                        label = { Text("File name (e.g. activity_main.xml)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveXmlLayout(saveLayoutFileName)
                        showSaveLayoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioGreen)
                ) {
                    Text("Save to res/layout", color = Color(0xFF003919), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveLayoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RenderVisualXmlNode(
    node: VisualUiNode,
    cardBg: Color,
    textClr: Color,
    isBlueprint: Boolean,
    onUpdateNode: (VisualUiNode) -> Unit
) {
    val nodeTint = when (node.colorHex) {
        "#4CAF50" -> StudioGreen
        "#38BDF8" -> StudioCyan
        "#A855F7" -> StudioPurple
        "#F59E0B" -> StudioOrange
        "#EF4444" -> StudioRed
        "#1E293B" -> Color(0xFF1E293B)
        else -> StudioGreen
    }

    when (node.type) {
        ComponentType.TEXT -> {
            Text(
                text = node.label,
                fontSize = node.textSizeSp.sp,
                fontWeight = if (node.isBold) FontWeight.Bold else FontWeight.Normal,
                color = if (isBlueprint) Color(0xFF38BDF8) else textClr,
                modifier = Modifier.padding(node.paddingDp.dp)
            )
        }

        ComponentType.BUTTON -> {
            Button(
                onClick = { /* Tactile interactive click */ },
                shape = RoundedCornerShape(node.cornerRadiusDp.dp),
                enabled = node.isEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBlueprint) Color(0xFF164E63) else nodeTint,
                    contentColor = if (isBlueprint) Color(0xFF38BDF8) else Color(0xFF003919)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(node.marginDp.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(node.label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        ComponentType.OUTLINED_BUTTON -> {
            OutlinedButton(
                onClick = { /* Tactile interactive click */ },
                shape = RoundedCornerShape(node.cornerRadiusDp.dp),
                enabled = node.isEnabled,
                border = BorderStroke(1.5.dp, if (isBlueprint) Color(0xFF38BDF8) else nodeTint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(node.marginDp.dp)
            ) {
                Text(node.label, color = if (isBlueprint) Color(0xFF38BDF8) else textClr, fontSize = 12.sp)
            }
        }

        ComponentType.CARD, ComponentType.ELEVATED_CARD -> {
            ElevatedCard(
                shape = RoundedCornerShape(node.cornerRadiusDp.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = node.elevationDp.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isBlueprint) Color(0xFF163E65) else cardBg
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(node.marginDp.dp)
            ) {
                Column(modifier = Modifier.padding(node.paddingDp.dp)) {
                    Text(
                        node.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isBlueprint) Color(0xFF7DD3FC) else textClr
                    )
                    if (node.secondaryText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            node.secondaryText,
                            fontSize = 11.sp,
                            color = if (isBlueprint) Color(0xFF38BDF8) else textClr.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        ComponentType.TEXT_FIELD -> {
            var inputVal by remember { mutableStateOf(node.secondaryText) }
            OutlinedTextField(
                value = inputVal,
                onValueChange = {
                    inputVal = it
                    onUpdateNode(node.copy(secondaryText = it))
                },
                label = { Text(node.label, fontSize = 11.sp) },
                placeholder = if (node.hint.isNotBlank()) { { Text(node.hint, fontSize = 11.sp) } } else null,
                shape = RoundedCornerShape(node.cornerRadiusDp.dp),
                enabled = node.isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(node.marginDp.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isBlueprint) Color(0xFF38BDF8) else StudioGreen,
                    unfocusedBorderColor = if (isBlueprint) Color(0xFF1E3A8A) else StudioBorder,
                    focusedTextColor = if (isBlueprint) Color(0xFF7DD3FC) else textClr,
                    unfocusedTextColor = if (isBlueprint) Color(0xFF7DD3FC) else textClr
                )
            )
        }

        ComponentType.SWITCH -> {
            var checkedState by remember { mutableStateOf(node.isChecked) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = node.paddingDp.dp, vertical = node.marginDp.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(node.label, color = if (isBlueprint) Color(0xFF7DD3FC) else textClr, fontSize = 12.sp)
                Switch(
                    checked = checkedState,
                    onCheckedChange = {
                        checkedState = it
                        onUpdateNode(node.copy(isChecked = it))
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = StudioGreen)
                )
            }
        }

        ComponentType.CHECKBOX -> {
            var checkedState by remember { mutableStateOf(node.isChecked) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = node.paddingDp.dp, vertical = node.marginDp.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checkedState,
                    onCheckedChange = {
                        checkedState = it
                        onUpdateNode(node.copy(isChecked = it))
                    },
                    colors = CheckboxDefaults.colors(checkedColor = StudioGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(node.label, color = if (isBlueprint) Color(0xFF7DD3FC) else textClr, fontSize = 12.sp)
            }
        }

        ComponentType.SLIDER -> {
            var sliderVal by remember { mutableFloatStateOf(node.sliderValue) }
            Column(modifier = Modifier.padding(horizontal = node.paddingDp.dp, vertical = node.marginDp.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(node.label, color = if (isBlueprint) Color(0xFF7DD3FC) else textClr, fontSize = 11.sp)
                    Text("${(sliderVal * 100).toInt()}%", color = StudioGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = sliderVal,
                    onValueChange = {
                        sliderVal = it
                        onUpdateNode(node.copy(sliderValue = it))
                    },
                    colors = SliderDefaults.colors(thumbColor = StudioGreen, activeTrackColor = StudioGreen)
                )
            }
        }

        ComponentType.FAB -> {
            ExtendedFloatingActionButton(
                onClick = { /* Tactile interactive click */ },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(node.label, fontWeight = FontWeight.Bold) },
                containerColor = if (isBlueprint) Color(0xFF164E63) else StudioGreen,
                contentColor = if (isBlueprint) Color(0xFF38BDF8) else Color(0xFF003919),
                modifier = Modifier.padding(node.marginDp.dp)
            )
        }

        ComponentType.PROGRESS_BAR -> {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = node.paddingDp.dp, vertical = node.marginDp.dp),
                color = if (isBlueprint) Color(0xFF38BDF8) else StudioGreen,
                trackColor = StudioSurfaceVariant
            )
        }

        ComponentType.DIVIDER -> {
            HorizontalDivider(
                color = if (isBlueprint) Color(0xFF38BDF8).copy(alpha = 0.5f) else StudioBorder,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        ComponentType.CHIP -> {
            AssistChip(
                onClick = { /* Tactile interactive click */ },
                label = { Text(node.label, fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(15.dp)) },
                colors = AssistChipDefaults.assistChipColors(containerColor = StudioSurfaceVariant),
                modifier = Modifier.padding(node.marginDp.dp)
            )
        }

        ComponentType.IMAGE_VIEW -> {
            Surface(
                shape = RoundedCornerShape(node.cornerRadiusDp.dp),
                color = if (isBlueprint) Color(0xFF1E3A8A) else StudioSurfaceVariant,
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(node.marginDp.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(node.label, fontSize = 10.sp, color = textClr)
                    }
                }
            }
        }
    }
}
