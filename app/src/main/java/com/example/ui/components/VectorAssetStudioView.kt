package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class VectorPresetItem(
    val name: String,
    val resName: String,
    val icon: ImageVector,
    val category: String,
    val pathData: String
)

@Composable
fun VectorAssetStudioView(
    onSaveToDrawable: (fileName: String, xmlContent: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    val presetIcons = remember {
        listOf(
            VectorPresetItem("Home", "ic_home.xml", Icons.Default.Home, "Navigation", "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"),
            VectorPresetItem("Settings", "ic_settings.xml", Icons.Default.Settings, "Navigation", "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"),
            VectorPresetItem("Search", "ic_search.xml", Icons.Default.Search, "Actions", "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z"),
            VectorPresetItem("Add", "ic_add.xml", Icons.Default.Add, "Actions", "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"),
            VectorPresetItem("Play", "ic_play.xml", Icons.Default.PlayArrow, "Media", "M8 5v14l11-7z"),
            VectorPresetItem("Pause", "ic_pause.xml", Icons.Default.Pause, "Media", "M6 19h4V5H6v14zm8-14v14h4V5h-4z"),
            VectorPresetItem("Volume Up", "ic_volume_up.xml", Icons.Default.VolumeUp, "Media", "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"),
            VectorPresetItem("Favorite", "ic_favorite.xml", Icons.Default.Favorite, "Social", "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"),
            VectorPresetItem("Share", "ic_share.xml", Icons.Default.Share, "Social", "M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92s2.92-1.31 2.92-2.92c0-1.61-1.31-2.92-2.92-2.92z"),
            VectorPresetItem("Notifications", "ic_notifications.xml", Icons.Default.Notifications, "Communication", "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.64 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2zm-2 1H8v-6c0-2.48 1.51-4.5 4-4.5s4 2.02 4 4.5v6z"),
            VectorPresetItem("Person / User", "ic_person.xml", Icons.Default.Person, "Social", "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"),
            VectorPresetItem("Cloud Sync", "ic_cloud_sync.xml", Icons.Default.CloudSync, "System", "M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96z")
        )
    }

    var selectedPreset by remember { mutableStateOf(presetIcons.first()) }
    var iconTintHex by remember { mutableStateOf("#4CAF50") }
    var iconSizeDp by remember { mutableFloatStateOf(24f) }
    var customPathData by remember { mutableStateOf(presetIcons.first().pathData) }
    var assetFileName by remember { mutableStateOf("ic_custom_vector.xml") }
    var saveNotice by remember { mutableStateOf<String?>(null) }

    fun generateVectorXml(path: String, colorHex: String, size: Float): String {
        return """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="${size.toInt()}dp"
    android:height="${size.toInt()}dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="$colorHex">
    <path
        android:fillColor="@android:color/white"
        android:pathData="$path" />
</vector>""".trimIndent()
    }

    val currentGeneratedXml = remember(customPathData, iconTintHex, iconSizeDp) {
        generateVectorXml(customPathData, iconTintHex, iconSizeDp)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .padding(10.dp)
    ) {
        // Top Toolbar
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = StudioSurface,
            border = BorderStroke(0.5.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Vector Asset Studio & SVG Converter", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Generate Android Vector Drawables for res/drawable/", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                Button(
                    onClick = {
                        val finalFileName = if (assetFileName.endsWith(".xml")) assetFileName else "$assetFileName.xml"
                        onSaveToDrawable(finalFileName, currentGeneratedXml)
                        saveNotice = "Saved '$finalFileName' to res/drawable/ directory!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioGreen),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF003919), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Drawable", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF003919))
                }
            }
        }

        if (saveNotice != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = StudioGreen.copy(alpha = 0.15f),
                border = BorderStroke(0.5.dp, StudioGreen.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(saveNotice!!, fontSize = 10.sp, color = StudioGreen)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Split Main Body: Left Presets Grid, Right Preview & XML Editor
        Row(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preset Icons Grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(StudioSurface, RoundedCornerShape(8.dp))
                    .border(0.5.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text("Icon Preset Gallery", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(6.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(presetIcons) { item ->
                        val isSelected = item.resName == selectedPreset.resName
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) StudioSurfaceVariant else Color(0xFF161922),
                            border = BorderStroke(1.dp, if (isSelected) StudioGreen else Color(0xFF242938)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedPreset = item
                                    customPathData = item.pathData
                                    assetFileName = item.resName
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.name,
                                    tint = if (isSelected) StudioGreen else Color.LightGray,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    item.name,
                                    fontSize = 9.sp,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Preview & Live XML Output
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .background(StudioSurface, RoundedCornerShape(8.dp))
                    .border(0.5.dp, StudioBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Live Vector Preview", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(currentGeneratedXml)) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy XML", tint = StudioGreen, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Preview Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color(0xFF0F1219), RoundedCornerShape(6.dp))
                        .border(0.5.dp, Color(0xFF222838), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = selectedPreset.icon,
                        contentDescription = null,
                        tint = when (iconTintHex) {
                            "#4CAF50" -> StudioGreen
                            "#00BCD4" -> StudioCyan
                            "#FF9800" -> StudioOrange
                            "#E91E63" -> Color(0xFFE91E63)
                            "#9C27B0" -> StudioPurple
                            else -> Color.White
                        },
                        modifier = Modifier.size(iconSizeDp.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Controls: Color Swatches & Size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Color swatches
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("#4CAF50", "#00BCD4", "#FF9800", "#E91E63", "#9C27B0", "#FFFFFF").forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (hex) {
                                            "#4CAF50" -> StudioGreen
                                            "#00BCD4" -> StudioCyan
                                            "#FF9800" -> StudioOrange
                                            "#E91E63" -> Color(0xFFE91E63)
                                            "#9C27B0" -> StudioPurple
                                            else -> Color.White
                                        }
                                    )
                                    .clickable { iconTintHex = hex }
                            )
                        }
                    }

                    // Size Picker
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${iconSizeDp.toInt()}dp", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Slider(
                            value = iconSizeDp,
                            onValueChange = { iconSizeDp = it },
                            valueRange = 16f..64f,
                            modifier = Modifier.width(90.dp).height(20.dp),
                            colors = SliderDefaults.colors(thumbColor = StudioGreen, activeTrackColor = StudioGreen)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Generated XML Output:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StudioCyan)
                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0E1117),
                    border = BorderStroke(0.5.dp, Color(0xFF232838)),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    Text(
                        text = currentGeneratedXml,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF9CDCFE),
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}
