package com.example.engine

import java.util.UUID

enum class ComponentType(val displayName: String, val category: String, val iconName: String) {
    TEXT("Text Label", "Typography", "title"),
    BUTTON("Filled Button", "Actions", "smart_button"),
    OUTLINED_BUTTON("Outlined Button", "Actions", "crop_free"),
    CARD("Card Container", "Containers", "credit_card"),
    ELEVATED_CARD("Elevated Card", "Containers", "layers"),
    TEXT_FIELD("Outlined TextField", "Inputs", "edit"),
    SWITCH("Switch Toggle", "Inputs", "toggle_on"),
    CHECKBOX("Checkbox Item", "Inputs", "check_box"),
    SLIDER("Range Slider", "Inputs", "tune"),
    FAB("Floating Action Button", "Actions", "add_circle"),
    PROGRESS_BAR("Progress Bar", "Indicators", "hourglass_empty"),
    DIVIDER("Divider Line", "Layout", "horizontal_rule"),
    CHIP("Assist Chip", "Actions", "label")
}

data class VisualUiNode(
    val id: String = UUID.randomUUID().toString(),
    val type: ComponentType,
    var label: String,
    var secondaryText: String = "",
    var colorHex: String = "Primary", // "Primary", "Secondary", "SurfaceVariant", "Error"
    var paddingDp: Int = 12,
    var cornerRadiusDp: Int = 12,
    var isEnabled: Boolean = true,
    var isChecked: Boolean = false,
    var sliderValue: Float = 0.5f
)

object VisualLayoutBridge {

    fun getDefaultNodesForTemplate(templateType: String, appName: String): List<VisualUiNode> {
        return listOf(
            VisualUiNode(
                type = ComponentType.ELEVATED_CARD,
                label = "$appName Hero Banner",
                secondaryText = "Active Development in Mobile IDE",
                colorHex = "Primary",
                paddingDp = 16,
                cornerRadiusDp = 16
            ),
            VisualUiNode(
                type = ComponentType.TEXT,
                label = "Quick Actions & Components",
                secondaryText = "",
                colorHex = "Primary",
                paddingDp = 8
            ),
            VisualUiNode(
                type = ComponentType.BUTTON,
                label = "Run Debug Build",
                secondaryText = "",
                colorHex = "Primary"
            ),
            VisualUiNode(
                type = ComponentType.TEXT_FIELD,
                label = "Project Name",
                secondaryText = appName
            ),
            VisualUiNode(
                type = ComponentType.SWITCH,
                label = "Enable Live Hot Reload",
                isChecked = true
            ),
            VisualUiNode(
                type = ComponentType.SLIDER,
                label = "Screen Scaling",
                sliderValue = 0.75f
            ),
            VisualUiNode(
                type = ComponentType.FAB,
                label = "Add New Composable",
                colorHex = "Primary"
            )
        )
    }

    fun generateComposeCode(nodes: List<VisualUiNode>, appName: String, packageName: String): String {
        return buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import androidx.compose.foundation.layout.*")
            appendLine("import androidx.compose.foundation.lazy.LazyColumn")
            appendLine("import androidx.compose.foundation.shape.RoundedCornerShape")
            appendLine("import androidx.compose.material.icons.Icons")
            appendLine("import androidx.compose.material.icons.filled.*")
            appendLine("import androidx.compose.material3.*")
            appendLine("import androidx.compose.runtime.*")
            appendLine("import androidx.compose.ui.Alignment")
            appendLine("import androidx.compose.ui.Modifier")
            appendLine("import androidx.compose.ui.text.font.FontWeight")
            appendLine("import androidx.compose.ui.unit.dp")
            appendLine("import androidx.compose.ui.unit.sp")
            appendLine()
            appendLine("@OptIn(ExperimentalMaterial3Api::class)")
            appendLine("@Composable")
            appendLine("fun VisualDesignerView(modifier: Modifier = Modifier) {")
            appendLine("    var sliderState by remember { mutableFloatStateOf(0.5f) }")
            appendLine("    var switchState by remember { mutableStateOf(true) }")
            appendLine("    var textInput by remember { mutableStateOf(\"$appName\") }")
            appendLine()
            appendLine("    LazyColumn(")
            appendLine("        modifier = modifier")
            appendLine("            .fillMaxSize()")
            appendLine("            .padding(16.dp),")
            appendLine("        verticalArrangement = Arrangement.spacedBy(12.dp)")
            appendLine("    ) {")

            for (node in nodes) {
                appendLine("        item {")
                when (node.type) {
                    ComponentType.TEXT -> {
                        appendLine("            Text(")
                        appendLine("                text = \"${node.label}\",")
                        appendLine("                style = MaterialTheme.typography.titleMedium,")
                        appendLine("                fontWeight = FontWeight.Bold")
                        appendLine("            )")
                    }
                    ComponentType.BUTTON -> {
                        appendLine("            Button(")
                        appendLine("                onClick = { /* Action */ },")
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
                        appendLine("                modifier = Modifier.fillMaxWidth()")
                        appendLine("            ) {")
                        appendLine("                Text(\"${node.label}\")")
                        appendLine("            }")
                    }
                    ComponentType.ELEVATED_CARD, ComponentType.CARD -> {
                        appendLine("            ElevatedCard(")
                        appendLine("                modifier = Modifier.fillMaxWidth(),")
                        appendLine("                shape = RoundedCornerShape(${node.cornerRadiusDp}.dp)")
                        appendLine("            ) {")
                        appendLine("                Column(modifier = Modifier.padding(${node.paddingDp}.dp)) {")
                        appendLine("                    Text(\"${node.label}\", fontWeight = FontWeight.Bold)")
                        if (node.secondaryText.isNotBlank()) {
                            appendLine("                    Spacer(modifier = Modifier.height(4.dp))")
                            appendLine("                    Text(\"${node.secondaryText}\", style = MaterialTheme.typography.bodyMedium)")
                        }
                        appendLine("                }")
                        appendLine("            }")
                    }
                    ComponentType.TEXT_FIELD -> {
                        appendLine("            OutlinedTextField(")
                        appendLine("                value = textInput,")
                        appendLine("                onValueChange = { textInput = it },")
                        appendLine("                label = { Text(\"${node.label}\") },")
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
                        appendLine("                Switch(checked = switchState, onCheckedChange = { switchState = it })")
                        appendLine("            }")
                    }
                    ComponentType.CHECKBOX -> {
                        appendLine("            Row(")
                        appendLine("                modifier = Modifier.fillMaxWidth(),")
                        appendLine("                verticalAlignment = Alignment.CenterVertically")
                        appendLine("            ) {")
                        appendLine("                Checkbox(checked = switchState, onCheckedChange = { switchState = it })")
                        appendLine("                Spacer(modifier = Modifier.width(8.dp))")
                        appendLine("                Text(\"${node.label}\")")
                        appendLine("            }")
                    }
                    ComponentType.SLIDER -> {
                        val sliderExpr = "$" + "{(sliderState * 100).toInt()}%"
                        appendLine("            Column {")
                        appendLine("                Text(\"${node.label}: $sliderExpr\")")
                        appendLine("                Slider(value = sliderState, onValueChange = { sliderState = it })")
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
                }
                appendLine("        }")
            }

            appendLine("    }")
            appendLine("}")
        }
    }
}
