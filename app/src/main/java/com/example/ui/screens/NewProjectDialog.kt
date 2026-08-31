package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.window.Dialog
import com.example.core.LanguageType
import com.example.core.SdkConfiguration
import com.example.data.repository.ProjectTemplate
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onCreateProject: (template: ProjectTemplate, name: String, pkg: String, desc: String, language: LanguageType, sdkConfig: SdkConfiguration) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var selectedTemplate by remember { mutableStateOf(ProjectTemplate.COMPOSE_M3_APP) }
    var selectedLanguage by remember { mutableStateOf(LanguageType.KOTLIN) }
    var minSdk by remember { mutableIntStateOf(24) }
    var targetSdk by remember { mutableIntStateOf(36) }
    var projectName by remember { mutableStateOf("MyAwesomeApp") }
    var packageName by remember { mutableStateOf("com.example.myawesomeapp") }
    var description by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = StudioSurface,
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddBox, contentDescription = null, tint = StudioGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Android Project", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Language Picker
                    item {
                        Text("1. Programming Language", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StudioGreen)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LanguageType.values().forEach { lang ->
                                val isSelected = selectedLanguage == lang
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedLanguage = lang },
                                    label = {
                                        Text(
                                            "${lang.displayName} (${lang.extension})",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (lang == LanguageType.KOTLIN) Icons.Default.Code else Icons.Default.Terminal,
                                            contentDescription = null,
                                            tint = if (isSelected) StudioGreen else Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 2. Template Picker
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("2. Project Template", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StudioGreen)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    items(ProjectTemplate.values()) { template ->
                        val isSelected = selectedTemplate == template
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) StudioSurfaceVariant else StudioBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) StudioGreen else StudioBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedTemplate = template
                                    if (projectName.isBlank() || projectName == "MyAwesomeApp") {
                                        projectName = template.title.replace(" ", "")
                                        packageName = "com.example.${projectName.lowercase()}"
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) StudioGreen else StudioSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        when (template) {
                                            ProjectTemplate.COMPOSE_M3_APP -> Icons.Default.Dashboard
                                            ProjectTemplate.AI_GEMINI_CHAT -> Icons.Default.SmartToy
                                            ProjectTemplate.CANVAS_GAME -> Icons.Default.SportsEsports
                                            ProjectTemplate.FINANCE_CONVERTER -> Icons.Default.CurrencyExchange
                                            ProjectTemplate.STOREFRONT_M3 -> Icons.Default.ShoppingBag
                                            ProjectTemplate.EMPTY_COMPOSE -> Icons.Default.Code
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF003919) else StudioGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(template.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text(template.description, fontSize = 11.sp, color = Color.Gray, maxLines = 2)
                                }
                            }
                        }
                    }

                    // 3. Configure Details
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("3. Android SDK & Package", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = StudioGreen)
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = projectName,
                            onValueChange = {
                                projectName = it
                                packageName = "com.example.${it.lowercase().replace(" ", "")}"
                            },
                            label = { Text("Application Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = packageName,
                            onValueChange = { packageName = it },
                            label = { Text("Package Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Min SDK & Target SDK selectors
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = "API $minSdk (Android ${if (minSdk >= 34) "14+" else if (minSdk >= 26) "8.0+" else "7.0+"})",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Minimum SDK") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = "API $targetSdk (Android 16)",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Target SDK") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (projectName.isNotBlank() && packageName.isNotBlank()) {
                                onCreateProject(
                                    selectedTemplate,
                                    projectName,
                                    packageName,
                                    description,
                                    selectedLanguage,
                                    SdkConfiguration(minSdk = minSdk, targetSdk = targetSdk)
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioGreen, contentColor = Color(0xFF003919))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Project", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
