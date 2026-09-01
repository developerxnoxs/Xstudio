package com.example.engine

import java.util.UUID

enum class ComponentType(
    val displayName: String,
    val category: String,
    val xmlTag: String,
    val defaultIdPrefix: String
) {
    TEXT("TextView", "Typography", "TextView", "tv_"),
    BUTTON("MaterialButton", "Actions", "com.google.android.material.button.MaterialButton", "btn_"),
    OUTLINED_BUTTON("OutlinedButton", "Actions", "com.google.android.material.button.MaterialButton", "btn_outlined_"),
    CARD("CardView", "Containers", "com.google.android.material.card.MaterialCardView", "card_"),
    ELEVATED_CARD("ElevatedCard", "Containers", "com.google.android.material.card.MaterialCardView", "elevated_card_"),
    TEXT_FIELD("TextInputLayout & EditText", "Inputs", "com.google.android.material.textfield.TextInputLayout", "input_layout_"),
    SWITCH("SwitchCompat", "Inputs", "androidx.appcompat.widget.SwitchCompat", "switch_"),
    CHECKBOX("CheckBox", "Inputs", "CheckBox", "checkbox_"),
    SLIDER("SeekBar / Slider", "Inputs", "com.google.android.material.slider.Slider", "slider_"),
    FAB("FloatingActionButton", "Actions", "com.google.android.material.floatingactionbutton.FloatingActionButton", "fab_"),
    PROGRESS_BAR("ProgressBar", "Indicators", "ProgressBar", "progress_bar_"),
    DIVIDER("Divider Line", "Layout", "View", "divider_"),
    CHIP("MaterialChip", "Actions", "com.google.android.material.chip.Chip", "chip_"),
    IMAGE_VIEW("ImageView", "Media", "ImageView", "img_preview_")
}

enum class LayoutDimensionType(val xmlValue: String, val label: String) {
    MATCH_PARENT("match_parent", "Match Parent"),
    WRAP_CONTENT("wrap_content", "Wrap Content"),
    FIXED_DP("custom", "Fixed DP")
}

data class VisualUiNode(
    val id: String = UUID.randomUUID().toString(),
    val type: ComponentType,
    var idName: String = "",
    var label: String = "",
    var secondaryText: String = "",
    var hint: String = "",
    var colorHex: String = "#4CAF50", // Hex color or token
    var textColorHex: String = "#FFFFFF",
    var paddingDp: Int = 12,
    var marginDp: Int = 6,
    var cornerRadiusDp: Int = 12,
    var elevationDp: Int = 4,
    var textSizeSp: Int = 14,
    var isBold: Boolean = false,
    var isEnabled: Boolean = true,
    var isChecked: Boolean = false,
    var sliderValue: Float = 0.5f,
    var layoutWidthType: LayoutDimensionType = LayoutDimensionType.MATCH_PARENT,
    var layoutHeightType: LayoutDimensionType = LayoutDimensionType.WRAP_CONTENT,
    var customWidthDp: Int = 200,
    var customHeightDp: Int = 48,
    var gravity: String = "start", // "start", "center", "end", "center_horizontal"
    var visibility: String = "visible" // "visible", "invisible", "gone"
) {
    init {
        if (idName.isBlank()) {
            val shortId = id.take(4)
            idName = "${type.defaultIdPrefix}$shortId"
        }
    }
}

object VisualLayoutBridge {

    fun getDefaultNodesForTemplate(templateType: String, appName: String): List<VisualUiNode> {
        return listOf(
            VisualUiNode(
                type = ComponentType.ELEVATED_CARD,
                idName = "card_hero",
                label = "$appName Studio Canvas",
                secondaryText = "Drag & drop components or edit attributes visually in real time.",
                colorHex = "#1E293B",
                textColorHex = "#FFFFFF",
                paddingDp = 16,
                marginDp = 8,
                cornerRadiusDp = 16,
                elevationDp = 6
            ),
            VisualUiNode(
                type = ComponentType.TEXT,
                idName = "tv_section_title",
                label = "Live XML Components",
                textSizeSp = 18,
                isBold = true,
                textColorHex = "#4CAF50",
                paddingDp = 6,
                marginDp = 4
            ),
            VisualUiNode(
                type = ComponentType.BUTTON,
                idName = "btn_primary_action",
                label = "Execute Debug Build",
                colorHex = "#4CAF50",
                textColorHex = "#003919",
                marginDp = 6,
                cornerRadiusDp = 10,
                elevationDp = 2
            ),
            VisualUiNode(
                type = ComponentType.TEXT_FIELD,
                idName = "input_project_title",
                label = "Project Title",
                hint = "Enter module or activity name",
                secondaryText = appName,
                marginDp = 6
            ),
            VisualUiNode(
                type = ComponentType.SWITCH,
                idName = "switch_hot_reload",
                label = "Real-time Code Reflection",
                isChecked = true,
                marginDp = 6
            ),
            VisualUiNode(
                type = ComponentType.SLIDER,
                idName = "slider_scaling",
                label = "Layout Padding Multiplier",
                sliderValue = 0.7f,
                marginDp = 6
            ),
            VisualUiNode(
                type = ComponentType.FAB,
                idName = "fab_add_element",
                label = "Add Element",
                colorHex = "#4CAF50",
                textColorHex = "#003919",
                marginDp = 8,
                cornerRadiusDp = 16,
                elevationDp = 6
            )
        )
    }

    /**
     * Generates a complete, standard Android XML Layout.
     */
    fun generateAndroidXmlLayout(
        nodes: List<VisualUiNode>,
        rootLayoutType: String = "LinearLayout",
        appName: String = "App"
    ): String {
        return buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            if (rootLayoutType.contains("ScrollView", ignoreCase = true)) {
                appendLine("<androidx.core.widget.NestedScrollView")
                appendLine("    xmlns:android=\"http://schemas.android.com/apk/res/android\"")
                appendLine("    xmlns:app=\"http://schemas.android.com/apk/res-auto\"")
                appendLine("    xmlns:tools=\"http://schemas.android.com/tools\"")
                appendLine("    android:layout_width=\"match_parent\"")
                appendLine("    android:layout_height=\"match_parent\"")
                appendLine("    android:fillViewport=\"true\"")
                appendLine("    android:background=\"#0F141C\">")
                appendLine()
                appendLine("    <LinearLayout")
                appendLine("        android:id=\"@+id/layout_container\"")
                appendLine("        android:layout_width=\"match_parent\"")
                appendLine("        android:layout_height=\"wrap_content\"")
                appendLine("        android:orientation=\"vertical\"")
                appendLine("        android:padding=\"16dp\">")
            } else {
                appendLine("<LinearLayout")
                appendLine("    xmlns:android=\"http://schemas.android.com/apk/res/android\"")
                appendLine("    xmlns:app=\"http://schemas.android.com/apk/res-auto\"")
                appendLine("    xmlns:tools=\"http://schemas.android.com/tools\"")
                appendLine("    android:id=\"@+id/main_layout\"")
                appendLine("    android:layout_width=\"match_parent\"")
                appendLine("    android:layout_height=\"match_parent\"")
                appendLine("    android:orientation=\"vertical\"")
                appendLine("    android:padding=\"16dp\"")
                appendLine("    android:background=\"#0F141C\">")
            }

            appendLine()
            for (node in nodes) {
                append(formatXmlNode(node, "        "))
                appendLine()
            }

            if (rootLayoutType.contains("ScrollView", ignoreCase = true)) {
                appendLine("    </LinearLayout>")
                appendLine("</androidx.core.widget.NestedScrollView>")
            } else {
                appendLine("</LinearLayout>")
            }
        }
    }

    private fun formatXmlNode(node: VisualUiNode, indent: String): String {
        val widthStr = when (node.layoutWidthType) {
            LayoutDimensionType.MATCH_PARENT -> "match_parent"
            LayoutDimensionType.WRAP_CONTENT -> "wrap_content"
            LayoutDimensionType.FIXED_DP -> "${node.customWidthDp}dp"
        }
        val heightStr = when (node.layoutHeightType) {
            LayoutDimensionType.MATCH_PARENT -> "match_parent"
            LayoutDimensionType.WRAP_CONTENT -> "wrap_content"
            LayoutDimensionType.FIXED_DP -> "${node.customHeightDp}dp"
        }

        return buildString {
            when (node.type) {
                ComponentType.TEXT -> {
                    appendLine("${indent}<TextView")
                    appendLine("${indent}    android:id=\"@+id/${node.idName}\"")
                    appendLine("${indent}    android:layout_width=\"$widthStr\"")
                    appendLine("${indent}    android:layout_height=\"$heightStr\"")
                    appendLine("${indent}    android:text=\"${escapeXml(node.label)}\"")
                    appendLine("${indent}    android:textSize=\"${node.textSizeSp}sp\"")
                    appendLine("${indent}    android:textColor=\"${node.textColorHex}\"")
                    if (node.isBold) appendLine("${indent}    android:textStyle=\"bold\"")
                    if (node.paddingDp > 0) appendLine("${indent}    android:padding=\"${node.paddingDp}dp\"")
                    if (node.marginDp > 0) appendLine("${indent}    android:layout_margin=\"${node.marginDp}dp\"")
                    appendLine("${indent}    android:gravity=\"${node.gravity}\" />")
                }

                ComponentType.BUTTON, ComponentType.OUTLINED_BUTTON -> {
                    val isOutlined = node.type == ComponentType.OUTLINED_BUTTON
                    appendLine("${indent}<com.google.android.material.button.MaterialButton")
                    appendLine("${indent}    android:id=\"@+id/${node.idName}\"")
                    appendLine("${indent}    android:layout_width=\"$widthStr\"")
                    appendLine("${indent}    android:layout_height=\"$heightStr\"")
                    appendLine("${indent}    android:text=\"${escapeXml(node.label)}\"")
                    if (isOutlined) {
                        appendLine("${indent}    style=\"@style/Widget.Material3.Button.OutlinedButton\"")
                        appendLine("${indent}    app:strokeColor=\"${node.colorHex}\"")
                    } else {
                        appendLine("${indent}    app:backgroundTint=\"${node.colorHex}\"")
                        appendLine("${indent}    android:textColor=\"${node.textColorHex}\"")
                    }
                    if (node.cornerRadiusDp > 0) appendLine("${indent}    app:cornerRadius=\"${node.cornerRadiusDp}dp\"")
                    if (node.elevationDp > 0) appendLine("${indent}    android:elevation=\"${node.elevationDp}dp\"")
                    if (node.marginDp > 0) appendLine("${indent}    android:layout_margin=\"${node.marginDp}dp\"")
                    appendLine("${indent}    android:enabled=\"${node.isEnabled}\" />")
                }

                ComponentType.CARD, ComponentType.ELEVATED_CARD -> {
                    appendLine("${indent}<com.google.android.material.card.MaterialCardView")
                    appendLine("${indent}    android:id=\"@+id/${node.idName}\"")
                    appendLine("${indent}    android:layout_width=\"$widthStr\"")
                    appendLine("${indent}    android:layout_height=\"$heightStr\"")
                    appendLine("${indent}    android:layout_margin=\"${node.marginDp}dp\"")
                    appendLine("${indent}    app:cardCornerRadius=\"${node.cornerRadiusDp}dp\"")
                    appendLine("${indent}    app:cardElevation=\"${node.elevationDp}dp\"")
                    appendLine("${indent}    app:cardBackgroundColor=\"${node.colorHex}\">")
                    appendLine()
                    appendLine("${indent}    <LinearLayout")
                    appendLine("${indent}        android:layout_width=\"match_parent\"")
                    appendLine("${indent}        android:layout_height=\"wrap_content\"")
                    appendLine("${indent}        android:orientation=\"vertical\"")
                    appendLine("${indent}        android:padding=\"${node.paddingDp}dp\">")
                    appendLine()
                    appendLine("${indent}        <TextView")
                    appendLine("${indent}            android:id=\"@+id/tv_${node.idName}_title\"")
                    appendLine("${indent}            android:layout_width=\"match_parent\"")
                    appendLine("${indent}            android:layout_height=\"wrap_content\"")
                    appendLine("${indent}            android:text=\"${escapeXml(node.label)}\"")
                    appendLine("${indent}            android:textSize=\"16sp\"")
                    appendLine("${indent}            android:textStyle=\"bold\"")
                    appendLine("${indent}            android:textColor=\"${node.textColorHex}\" />")
                    if (node.secondaryText.isNotBlank()) {
                        appendLine()
                        appendLine("${indent}        <TextView")
                        appendLine("${indent}            android:id=\"@+id/tv_${node.idName}_sub\"")
                        appendLine("${indent}            android:layout_width=\"match_parent\"")
                        appendLine("${indent}            android:layout_height=\"wrap_content\"")
                        appendLine("${indent}            android:layout_marginTop=\"4dp\"")
                        appendLine("${indent}            android:text=\"${escapeXml(node.secondaryText)}\"")
                        appendLine("${indent}            android:textSize=\"13sp\"")
                        appendLine("${indent}            android:textColor=\"#A0AEC0\" />")
                    }
                    appendLine("${indent}    </LinearLayout>")
                    appendLine("${indent}</com.google.android.material.card.MaterialCardView>")
                }

                ComponentType.TEXT_FIELD -> {
                    appendLine("${indent}<com.google.android.material.textfield.TextInputLayout")
                    appendLine("${indent}    android:id=\"@+id/${node.idName}\"")
                    appendLine("${indent}    android:layout_width=\"$widthStr\"")
                    appendLine("${indent}    android:layout_height=\"$heightStr\"")
                    appendLine("${indent}    android:layout_margin=\"${node.marginDp}dp\"")
                    appendLine("${indent}    android:hint=\"${escapeXml(if (node.hint.isNotBlank()) node.hint else node.label)}\"")
                    appendLine("${indent}    app:boxCornerRadiusTopStart=\"${node.cornerRadiusDp}dp\"")
                    appendLine("${indent}    app:boxCornerRadiusTopEnd=\"${node.cornerRadiusDp}dp\"")
                    appendLine("${indent}    app:boxCornerRadiusBottomStart=\"${node.cornerRadiusDp}dp\"")
                    appendLine("${indent}    app:boxCornerRadiusBottomEnd=\"${node.cornerRadiusDp}dp\"")
                    appendLine("${indent}    style=\"@style/Widget.Material3.TextInputLayout.OutlinedBox\">")
                    appendLine()
                    appendLine("${indent}    <com.google.android.material.textfield.TextInputEditText")
                    appendLine("${indent}        android:id=\"@+id/et_${node.idName}\"")
                    appendLine("${indent}        android:layout_width=\"match_parent\"")
                    appendLine("${indent}        android:layout_height=\"wrap_content\"")
                    appendLine("${indent}        android:text=\"${escapeXml(node.secondaryText)}\"")
                    appendLine("${indent}        android:textColor=\"${node.textColorHex}\" />")
                    appendLine("${indent}</com.google.android.material.textfield.TextInputLayout>")
                }

                ComponentType.SWITCH -> {
                    appendLine("${indent}<androidx.appcompat.widget.SwitchCompat")
                    appendLine("${indent}    android:id=\"@+id/${node.idName}\"")
                    appendLine("${indent}    android:layout_width=\"$widthStr\"")
                    appendLine("${indent}    android:layout_height=\"$heightStr\"")
                    appendLine("${indent}    android:text=\"${escapeXml(node.label)}\"")
                    appendLine("${indent}    android:checked=\"${node.isChecked}\"")
                    appendLine("${indent}    android:textColor=\"${node.textColorHex}\"")
                    if (node.marginDp > 0) appendLine("${indent}    android:layout_margin=\"${node.marginDp}dp\"")
                    if (node.paddingDp > 0) appendLine("${indent}    android:padding=\"${node.paddingDp}dp\" />")
                    else appendLine("${indent}    />")
                }

                ComponentType.CHECKBOX -> {
                    appendLine("${indent}<CheckBox")
                    appendLine("${indent}    android:id=\"@+id/${node.idName}\"")
                    appendLine("${indent}    android:layout_width=\"$widthStr\"")
                    appendLine("${indent}    android:layout_height=\"$heightStr\"")
                    appendLine("${indent}    android:text=\"${escapeXml(node.label)}\"")
                    appendLine("${indent}    android:checked=\"${node.isChecked}\"")
                    appendLine("${indent}    android:textColor=\"${node.textColorHex}\"")
                    if (node.marginDp > 0) appendLine("${indent}    android:layout_margin=\"${node.marginDp}dp\" />")
                    else appendLine("${indent}    />")
                }

                ComponentType.SLIDER -> {
                    appendLine("${indent}<com.google.android.material.slider.Slider")
                    appendLine("${indent}    android:id=\"@+id/${node.idName}\"")
                    appendLine("${indent}    android:layout_width=\"$widthStr\"")
                    appendLine("${indent}    android:layout_height=\"$heightStr\"")
                    appendLine("${indent}    android:valueFrom=\"0.0\"")
                    appendLine("${indent}    android:valueTo=\"100.0\"")
                    appendLine("${indent}    android:value=\"${(node.sliderValue * 100).toInt()}.0\"")
                    appendLine("${indent}    app:thumbColor=\"${node.colorHex}\"")
                    appendLine("${indent}    app:trackColorActive=\"${node.colorHex}\"")
                    if (node.marginDp > 0) appendLine("${indent}    android:layout_margin=\"${node.marginDp}dp\" />")
                    else appendLine("${indent}    />")
                }

                ComponentType.FAB -> {
                    appendLine("${indent}<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton")
                    appendLine("${indent}    android:id=\"@+id/${node.idName}\"")
                    appendLine("${indent}    android:layout_width=\"wrap_content\"")
                    appendLine("${indent}    android:layout_height=\"wrap_content\"")
                    appendLine("${indent}    android:text=\"${escapeXml(node.label)}\"")
                    appendLine("${indent}    app:backgroundTint=\"${node.colorHex}\"")
                    appendLine("${indent}    android:textColor=\"${node.textColorHex}\"")
                    if (node.elevationDp > 0) appendLine("${indent}    app:elevation=\"${node.elevationDp}dp\"")
                    if (node.marginDp > 0) appendLine("${indent}    android:layout_margin=\"${node.marginDp}dp\" />")
                    else appendLine("${indent}    />")
                }

                ComponentType.PROGRESS_BAR -> {
                    appendLine("${indent}<ProgressBar")
                    appendLine("${indent}    android:id=\"@+id/${node.idName}\"")
                    appendLine("${indent}    style=\"?android:attr/progressBarStyleHorizontal\"")
                    appendLine("${indent}    android:layout_width=\"$widthStr\"")
                    appendLine("${indent}    android:layout_height=\"$heightStr\"")
                    appendLine("${indent}    android:indeterminate=\"true\"")
                    if (node.marginDp > 0) appendLine("${indent}    android:layout_margin=\"${node.marginDp}dp\" />")
                    else appendLine("${indent}    />")
                }

                ComponentType.DIVIDER -> {
                    appendLine("${indent}<View")
                    appendLine("${indent}    android:id=\"@+id/${node.idName}\"")
                    appendLine("${indent}    android:layout_width=\"match_parent\"")
                    appendLine("${indent}    android:layout_height=\"1dp\"")
                    appendLine("${indent}    android:background=\"#334155\"")
                    appendLine("${indent}    android:layout_marginTop=\"8dp\"")
                    appendLine("${indent}    android:layout_marginBottom=\"8dp\" />")
                }

                ComponentType.CHIP -> {
                    appendLine("${indent}<com.google.android.material.chip.Chip")
                    appendLine("${indent}    android:id=\"@+id/${node.idName}\"")
                    appendLine("${indent}    android:layout_width=\"wrap_content\"")
                    appendLine("${indent}    android:layout_height=\"wrap_content\"")
                    appendLine("${indent}    android:text=\"${escapeXml(node.label)}\"")
                    if (node.marginDp > 0) appendLine("${indent}    android:layout_margin=\"${node.marginDp}dp\" />")
                    else appendLine("${indent}    />")
                }

                ComponentType.IMAGE_VIEW -> {
                    appendLine("${indent}<ImageView")
                    appendLine("${indent}    android:id=\"@+id/${node.idName}\"")
                    appendLine("${indent}    android:layout_width=\"$widthStr\"")
                    appendLine("${indent}    android:layout_height=\"$heightStr\"")
                    appendLine("${indent}    android:src=\"@drawable/ic_launcher_foreground\"")
                    appendLine("${indent}    android:contentDescription=\"${escapeXml(node.label)}\"")
                    if (node.marginDp > 0) appendLine("${indent}    android:layout_margin=\"${node.marginDp}dp\" />")
                    else appendLine("${indent}    />")
                }
            }
        }.trimEnd()
    }

    private fun escapeXml(str: String): String {
        return str
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Parses standard Android layout XML back into VisualUiNodes.
     */
    fun parseXmlToVisualNodes(xmlContent: String): List<VisualUiNode> {
        val result = mutableListOf<VisualUiNode>()
        if (xmlContent.isBlank()) return result

        try {
            val lines = xmlContent.lines()
            var currentTag = ""
            var currentAttrs = mutableMapOf<String, String>()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("<") && !trimmed.startsWith("<?") && !trimmed.startsWith("<!--") && !trimmed.startsWith("</")) {
                    val tagMatch = Regex("<([a-zA-Z0-9_\\.]+)").find(trimmed)
                    if (tagMatch != null) {
                        currentTag = tagMatch.groupValues[1]
                        currentAttrs = mutableMapOf()
                    }
                }

                // Collect attributes
                val attrMatches = Regex("([a-zA-Z0-9_:]+)=\"([^\"]*)\"").findAll(trimmed)
                for (match in attrMatches) {
                    val attrKey = match.groupValues[1]
                    val attrVal = match.groupValues[2]
                    currentAttrs[attrKey] = attrVal
                }

                if (trimmed.endsWith("/>") || trimmed.endsWith("</$currentTag>")) {
                    val node = createNodeFromParsedTag(currentTag, currentAttrs)
                    if (node != null) {
                        result.add(node)
                    }
                    currentTag = ""
                    currentAttrs = mutableMapOf()
                }
            }
        } catch (_: Exception) {
            // Fallback gracefully
        }

        return result
    }

    private fun createNodeFromParsedTag(tag: String, attrs: Map<String, String>): VisualUiNode? {
        val rawId = attrs["android:id"] ?: ""
        val cleanId = rawId.removePrefix("@+id/").removePrefix("@id/")
        val text = attrs["android:text"] ?: ""
        val hint = attrs["android:hint"] ?: ""

        val type = when {
            tag.endsWith("TextView") -> ComponentType.TEXT
            tag.endsWith("MaterialButton") || tag.endsWith("Button") -> {
                if (attrs["style"]?.contains("OutlinedButton") == true) ComponentType.OUTLINED_BUTTON else ComponentType.BUTTON
            }
            tag.endsWith("MaterialCardView") || tag.endsWith("CardView") -> ComponentType.CARD
            tag.endsWith("TextInputLayout") || tag.endsWith("EditText") -> ComponentType.TEXT_FIELD
            tag.endsWith("SwitchCompat") || tag.endsWith("Switch") -> ComponentType.SWITCH
            tag.endsWith("CheckBox") -> ComponentType.CHECKBOX
            tag.endsWith("Slider") || tag.endsWith("SeekBar") -> ComponentType.SLIDER
            tag.endsWith("FloatingActionButton") -> ComponentType.FAB
            tag.endsWith("ProgressBar") -> ComponentType.PROGRESS_BAR
            tag.endsWith("Chip") -> ComponentType.CHIP
            tag.endsWith("ImageView") -> ComponentType.IMAGE_VIEW
            tag == "View" && (attrs["android:layout_height"] == "1dp" || attrs["android:layout_height"] == "2dp") -> ComponentType.DIVIDER
            else -> return null
        }

        val margin = attrs["android:layout_margin"]?.removeSuffix("dp")?.toIntOrNull() ?: 6
        val padding = attrs["android:padding"]?.removeSuffix("dp")?.toIntOrNull() ?: 12
        val cornerRadius = attrs["app:cardCornerRadius"]?.removeSuffix("dp")?.toIntOrNull()
            ?: attrs["app:cornerRadius"]?.removeSuffix("dp")?.toIntOrNull() ?: 12

        return VisualUiNode(
            type = type,
            idName = if (cleanId.isNotBlank()) cleanId else "${type.defaultIdPrefix}${UUID.randomUUID().toString().take(4)}",
            label = if (text.isNotBlank()) text else type.displayName,
            hint = hint,
            marginDp = margin,
            paddingDp = padding,
            cornerRadiusDp = cornerRadius,
            textColorHex = attrs["android:textColor"] ?: "#FFFFFF",
            colorHex = attrs["app:backgroundTint"] ?: attrs["app:cardBackgroundColor"] ?: "#4CAF50"
        )
    }

    fun generateComposeCode(nodes: List<VisualUiNode>, appName: String, packageName: String): String {
        return buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import androidx.compose.foundation.layout.*")
            appendLine("import androidx.compose.foundation.lazy.LazyColumn")
            appendLine("import androidx.compose.foundation.lazy.items")
            appendLine("import androidx.compose.foundation.shape.RoundedCornerShape")
            appendLine("import androidx.compose.material.icons.Icons")
            appendLine("import androidx.compose.material.icons.filled.*")
            appendLine("import androidx.compose.material3.*")
            appendLine("import androidx.compose.runtime.*")
            appendLine("import androidx.compose.ui.Alignment")
            appendLine("import androidx.compose.ui.Modifier")
            appendLine("import androidx.compose.ui.graphics.Color")
            appendLine("import androidx.compose.ui.text.font.FontWeight")
            appendLine("import androidx.compose.ui.unit.dp")
            appendLine("import androidx.compose.ui.unit.sp")
            appendLine()
            appendLine("@OptIn(ExperimentalMaterial3Api::class)")
            appendLine("@Composable")
            appendLine("fun ActivityMainLayoutView(modifier: Modifier = Modifier) {")
            appendLine("    var sliderValue by remember { mutableFloatStateOf(0.5f) }")
            appendLine("    var isChecked by remember { mutableStateOf(true) }")
            appendLine("    var inputText by remember { mutableStateOf(\"$appName\") }")
            appendLine()
            appendLine("    LazyColumn(")
            appendLine("        modifier = modifier")
            appendLine("            .fillMaxSize()")
            appendLine("            .padding(16.dp),")
            appendLine("        verticalArrangement = Arrangement.spacedBy(10.dp)")
            appendLine("    ) {")

            for (node in nodes) {
                appendLine("        item {")
                when (node.type) {
                    ComponentType.TEXT -> {
                        appendLine("            Text(")
                        appendLine("                text = \"${node.label}\",")
                        appendLine("                fontSize = ${node.textSizeSp}.sp,")
                        if (node.isBold) appendLine("                fontWeight = FontWeight.Bold,")
                        appendLine("                modifier = Modifier.padding(${node.paddingDp}.dp)")
                        appendLine("            )")
                    }
                    ComponentType.BUTTON -> {
                        appendLine("            Button(")
                        appendLine("                onClick = { /* Action: ${node.idName} */ },")
                        appendLine("                shape = RoundedCornerShape(${node.cornerRadiusDp}.dp),")
                        appendLine("                modifier = Modifier.fillMaxWidth()")
                        appendLine("            ) {")
                        appendLine("                Icon(Icons.Default.PlayArrow, contentDescription = null)")
                        appendLine("                Spacer(modifier = Modifier.width(8.dp))")
                        appendLine("                Text(\"${node.label}\")")
                        appendLine("            }")
                    }
                    ComponentType.OUTLINED_BUTTON -> {
                        appendLine("            OutlinedButton(")
                        appendLine("                onClick = { /* Action */ },")
                        appendLine("                shape = RoundedCornerShape(${node.cornerRadiusDp}.dp),")
                        appendLine("                modifier = Modifier.fillMaxWidth()")
                        appendLine("            ) {")
                        appendLine("                Text(\"${node.label}\")")
                        appendLine("            }")
                    }
                    ComponentType.ELEVATED_CARD, ComponentType.CARD -> {
                        appendLine("            ElevatedCard(")
                        appendLine("                shape = RoundedCornerShape(${node.cornerRadiusDp}.dp),")
                        appendLine("                modifier = Modifier.fillMaxWidth()")
                        appendLine("            ) {")
                        appendLine("                Column(modifier = Modifier.padding(${node.paddingDp}.dp)) {")
                        appendLine("                    Text(\"${node.label}\", fontWeight = FontWeight.Bold, fontSize = 16.sp)")
                        if (node.secondaryText.isNotBlank()) {
                            appendLine("                    Spacer(modifier = Modifier.height(4.dp))")
                            appendLine("                    Text(\"${node.secondaryText}\", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)")
                        }
                        appendLine("                }")
                        appendLine("            }")
                    }
                    ComponentType.TEXT_FIELD -> {
                        appendLine("            OutlinedTextField(")
                        appendLine("                value = inputText,")
                        appendLine("                onValueChange = { inputText = it },")
                        appendLine("                label = { Text(\"${node.label}\") },")
                        if (node.hint.isNotBlank()) appendLine("                placeholder = { Text(\"${node.hint}\") },")
                        appendLine("                shape = RoundedCornerShape(${node.cornerRadiusDp}.dp),")
                        appendLine("                modifier = Modifier.fillMaxWidth()")
                        appendLine("            )")
                    }
                    ComponentType.SWITCH -> {
                        appendLine("            Row(")
                        appendLine("                modifier = Modifier.fillMaxWidth(),")
                        appendLine("                horizontalArrangement = Arrangement.SpaceBetween,")
                        appendLine("                verticalAlignment = Alignment.CenterVertically")
                        appendLine("            ) {")
                        appendLine("                Text(\"${node.label}\")")
                        appendLine("                Switch(checked = isChecked, onCheckedChange = { isChecked = it })")
                        appendLine("            }")
                    }
                    ComponentType.CHECKBOX -> {
                        appendLine("            Row(")
                        appendLine("                modifier = Modifier.fillMaxWidth(),")
                        appendLine("                verticalAlignment = Alignment.CenterVertically")
                        appendLine("            ) {")
                        appendLine("                Checkbox(checked = isChecked, onCheckedChange = { isChecked = it })")
                        appendLine("                Spacer(modifier = Modifier.width(8.dp))")
                        appendLine("                Text(\"${node.label}\")")
                        appendLine("            }")
                    }
                    ComponentType.SLIDER -> {
                        appendLine("            Column {")
                        appendLine("                Text(\"${node.label}: \${(sliderValue * 100).toInt()}%\")")
                        appendLine("                Slider(value = sliderValue, onValueChange = { sliderValue = it })")
                        appendLine("            }")
                    }
                    ComponentType.FAB -> {
                        appendLine("            ExtendedFloatingActionButton(")
                        appendLine("                onClick = { /* FAB */ },")
                        appendLine("                icon = { Icon(Icons.Default.Add, contentDescription = null) },")
                        appendLine("                text = { Text(\"${node.label}\") }")
                        appendLine("            )")
                    }
                    ComponentType.PROGRESS_BAR -> {
                        appendLine("            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())")
                    }
                    ComponentType.DIVIDER -> {
                        appendLine("            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))")
                    }
                    ComponentType.CHIP -> {
                        appendLine("            AssistChip(")
                        appendLine("                onClick = { /* Chip */ },")
                        appendLine("                label = { Text(\"${node.label}\") },")
                        appendLine("                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }")
                        appendLine("            )")
                    }
                    ComponentType.IMAGE_VIEW -> {
                        appendLine("            Icon(")
                        appendLine("                Icons.Default.Image,")
                        appendLine("                contentDescription = \"${node.label}\",")
                        appendLine("                modifier = Modifier.size(64.dp)")
                        appendLine("            )")
                    }
                }
                appendLine("        }")
            }

            appendLine("    }")
            appendLine("}")
        }
    }
}

