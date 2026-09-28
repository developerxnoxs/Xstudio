package com.example.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.BuildDiagnostic
import com.example.ui.theme.*

@Composable
fun CodeEditorView(
    content: String,
    fileType: String,
    onContentChange: (String) -> Unit,
    diagnostics: List<BuildDiagnostic> = emptyList(),
    targetLineToHighlight: Int? = null,
    activeDiagnostic: BuildDiagnostic? = null,
    isRealtimeCheckingEnabled: Boolean = true,
    onToggleRealtimeChecking: () -> Unit = {},
    onRunManualCheck: () -> Unit = {},
    searchQuery: String = "",
    onAskAiFix: (BuildDiagnostic) -> Unit = {},
    onAskAiComplete: (prefix: String, suffix: String) -> Unit = { _, _ -> },
    onAskAiReview: () -> Unit = {},
    onDismissDiagnostic: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val editorState = remember { CodeEditorStateManager(content) }
    var selectedLineDiagnostic by remember { mutableStateOf<BuildDiagnostic?>(null) }
    var showIntelliSenseManual by remember { mutableStateOf(false) }

    // Synchronize if incoming content changed externally
    LaunchedEffect(content) {
        if (editorState.textFieldValue.text != content) {
            editorState.setText(content)
        }
    }

    // Word at cursor for IntelliSense
    val cursorWordInfo = remember(editorState.textFieldValue.text, editorState.textFieldValue.selection.start) {
        IntelliSenseEngine.extractWordAtCursor(
            editorState.textFieldValue.text,
            editorState.textFieldValue.selection.start
        )
    }

    val completions = remember(cursorWordInfo.first, showIntelliSenseManual, fileType) {
        if (showIntelliSenseManual || cursorWordInfo.first.length >= 2 || cursorWordInfo.first.startsWith("@") || cursorWordInfo.first.startsWith(".")) {
            IntelliSenseEngine.getCompletions(cursorWordInfo.first, fileType)
        } else {
            emptyList()
        }
    }

    val isIntelliSenseVisible = completions.isNotEmpty()

    val lines = remember(editorState.textFieldValue.text) {
        editorState.textFieldValue.text.lines()
    }
    val lineCount = lines.size.coerceAtLeast(1)

    val errorMap = remember(diagnostics) {
        diagnostics.groupBy { it.line }
    }
    val errorLines = remember(diagnostics) {
        diagnostics.map { it.line }.toSet()
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    // Smooth scroll and select target line if requested (e.g. from build errors jump)
    LaunchedEffect(targetLineToHighlight) {
        if (targetLineToHighlight != null && targetLineToHighlight in 1..lineCount) {
            editorState.setCursorToLine(targetLineToHighlight)
            val approximateLineHeightPx = 54 // ~20sp + padding in px
            val scrollTarget = ((targetLineToHighlight - 3).coerceAtLeast(0) * approximateLineHeightPx)
            verticalScrollState.animateScrollTo(scrollTarget)
            selectedLineDiagnostic = diagnostics.find { it.line == targetLineToHighlight } ?: activeDiagnostic
        }
    }

    LaunchedEffect(activeDiagnostic) {
        if (activeDiagnostic != null) {
            selectedLineDiagnostic = activeDiagnostic
            if (activeDiagnostic.line in 1..lineCount) {
                editorState.setCursorToLine(activeDiagnostic.line)
                val scrollTarget = ((activeDiagnostic.line - 3).coerceAtLeast(0) * 54)
                verticalScrollState.animateScrollTo(scrollTarget)
            }
        }
    }

    val currentDiagnosticToDisplay = selectedLineDiagnostic ?: activeDiagnostic ?: diagnostics.firstOrNull()

    val quickSnippets = remember(fileType) {
        when (fileType.uppercase()) {
            "KOTLIN" -> listOf(
                "@Composable", "fun", "val", "var", "Modifier", "Column", "Row", "Box",
                "Button", "Text", "Card", "remember { mutableStateOf() }", "LaunchedEffect",
                "{", "}", "(", ")", "\"", "=", "->", ":", ";"
            )
            "JAVA" -> listOf(
                "public", "private", "protected", "class", "void", "String", "int", "boolean",
                "@Override", "findViewById()", "setOnClickListener", "Toast.makeText()",
                "{", "}", "(", ")", ";", "\"", "=", "new"
            )
            "XML", "MANIFEST" -> listOf(
                "<LinearLayout", "<TextView", "<Button", "<ImageView", "<CardView",
                "android:layout_width=\"match_parent\"", "android:layout_height=\"wrap_content\"",
                "android:orientation=\"vertical\"", "android:text=\"\"", "android:id=\"@+id/\"",
                "/>", "</LinearLayout>", "\"", "<", ">"
            )
            "GRADLE" -> listOf(
                "implementation()", "plugins {", "dependencies {", "android {", "defaultConfig {",
                "alias(libs.", "compileSdk = 36", "minSdk = 24", "{", "}", "\"", "="
            )
            else -> listOf("{", "}", "(", ")", "[", "]", "\"", "=", ";", ":")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
    ) {
        // Quick Editor Toolbar
        Surface(
            color = StudioSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Undo / Redo controls
                IconButton(
                    onClick = {
                        val undid = editorState.undo()
                        if (undid != null) onContentChange(undid)
                    },
                    enabled = editorState.canUndo,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Undo,
                        contentDescription = "Undo",
                        tint = if (editorState.canUndo) Color.White else Color.DarkGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val redid = editorState.redo()
                        if (redid != null) onContentChange(redid)
                    },
                    enabled = editorState.canRedo,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Redo,
                        contentDescription = "Redo",
                        tint = if (editorState.canRedo) Color.White else Color.DarkGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(editorState.textFieldValue.text))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy All",
                        tint = StudioCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Format Code (Beautify / Indent)
                IconButton(
                    onClick = {
                        val formatted = IntelliSenseEngine.formatCode(editorState.textFieldValue.text, fileType)
                        editorState.setText(formatted)
                        onContentChange(formatted)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.AutoFixHigh,
                        contentDescription = "Format Code (Ctrl+Alt+L)",
                        tint = StudioGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // IntelliSense Auto-Complete Button
                IconButton(
                    onClick = { showIntelliSenseManual = !showIntelliSenseManual },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.FlashOn,
                        contentDescription = "IntelliSense Auto-Complete",
                        tint = if (isIntelliSenseVisible) StudioGreen else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Realtime Syntax Status & Diagnostics Badge
                if (isRealtimeCheckingEnabled) {
                    if (diagnostics.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StudioRed.copy(alpha = 0.2f),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clickable {
                                    val nextDiag = diagnostics.firstOrNull()
                                    selectedLineDiagnostic = nextDiag
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = StudioRed, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${diagnostics.size} Syntax Error${if (diagnostics.size > 1) "s" else ""}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioRed
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StudioGreen.copy(alpha = 0.15f),
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clickable { onRunManualCheck() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Live: Syntax OK",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioGreen
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Gray.copy(alpha = 0.2f),
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clickable { onToggleRealtimeChecking() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Live Lint Off",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Scrollable Quick Snippets Bar
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StudioGreen.copy(alpha = 0.22f),
                            border = BorderStroke(0.5.dp, StudioGreen.copy(alpha = 0.6f)),
                            onClick = {
                                val cursor = editorState.textFieldValue.selection.start
                                val fullText = editorState.textFieldValue.text
                                val prefix = fullText.take(cursor)
                                val suffix = fullText.drop(cursor)
                                onAskAiComplete(prefix, suffix)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "AI Complete",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioGreen
                                )
                            }
                        }
                    }
                    item {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StudioCyan.copy(alpha = 0.2f),
                            border = BorderStroke(0.5.dp, StudioCyan.copy(alpha = 0.5f)),
                            onClick = onAskAiReview
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = StudioCyan, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "AI Doctor",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StudioCyan
                                )
                            }
                        }
                    }
                    items(quickSnippets) { snippet ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StudioSurface,
                            onClick = {
                                val updated = editorState.insertSnippet(snippet)
                                onContentChange(updated)
                            }
                        ) {
                            Text(
                                text = snippet,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = if (snippet.startsWith("@") || snippet == "fun") StudioGreen else Color(0xFFDCDCDC),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // IntelliSense Auto-Completion Floating / Docked Strip
        AnimatedVisibility(
            visible = isIntelliSenseVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = Color(0xFF1B202D),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = StudioCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Auto-Complete:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StudioCyan
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        items(completions) { item ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StudioSurface,
                                border = BorderStroke(1.dp, Color(item.kind.colorHex).copy(alpha = 0.6f)),
                                onClick = {
                                    val (word, wordStart) = cursorWordInfo
                                    val updated = editorState.replaceWordWithCompletion(
                                        wordStart = wordStart,
                                        wordLength = word.length,
                                        completion = item
                                    )
                                    showIntelliSenseManual = false
                                    onContentChange(updated)
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = Color(item.kind.colorHex).copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = item.kind.badge,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(item.kind.colorHex),
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = item.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                    if (item.detail.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = item.detail,
                                            fontSize = 9.sp,
                                            color = Color.Gray,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = { showIntelliSenseManual = false },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }

        // Active Diagnostic Banner (if an error exists in the current file or is selected)
        AnimatedVisibility(
            visible = currentDiagnosticToDisplay != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (currentDiagnosticToDisplay != null) {
                Surface(
                    color = Color(0xFF2A1518),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(currentDiagnosticToDisplay.errorType.badgeColorHex).copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = currentDiagnosticToDisplay.errorType.label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(currentDiagnosticToDisplay.errorType.badgeColorHex),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Line ${currentDiagnosticToDisplay.line}:${currentDiagnosticToDisplay.column}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StudioRed
                            )
                            Spacer(modifier = Modifier.weight(1f))

                            // Ask Studio Bot to fix
                            TextButton(
                                onClick = { onAskAiFix(currentDiagnosticToDisplay) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = StudioCyan)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ask Studio Bot", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = {
                                    selectedLineDiagnostic = null
                                    onDismissDiagnostic()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }

                        Text(
                            text = currentDiagnosticToDisplay.message,
                            fontSize = 11.sp,
                            color = Color(0xFFFF8A80),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        if (currentDiagnosticToDisplay.suggestion != null) {
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = StudioGreen, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentDiagnosticToDisplay.suggestion,
                                    fontSize = 10.sp,
                                    color = Color(0xFFA5D6A7)
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = StudioBorder, thickness = 1.dp)

        // Main Editor Surface (Line Numbers + Code Canvas)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(verticalScrollState)
        ) {
            // Line Number Gutter
            Column(
                modifier = Modifier
                    .background(Color(0xFF141416))
                    .padding(vertical = 12.dp, horizontal = 6.dp)
                    .widthIn(min = 38.dp),
                horizontalAlignment = Alignment.End
            ) {
                for (i in 1..lineCount) {
                    val isError = errorLines.contains(i)
                    val lineDiags = errorMap[i]
                    val isSelectedLine = currentDiagnosticToDisplay?.line == i

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(20.dp)
                            .clickable(enabled = isError) {
                                selectedLineDiagnostic = lineDiags?.firstOrNull()
                            }
                    ) {
                        if (isError) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error at line $i",
                                tint = if (isSelectedLine) StudioRed else StudioRed.copy(alpha = 0.8f),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                        Text(
                            text = "$i",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelectedLine) StudioRed else if (isError) StudioRed.copy(alpha = 0.85f) else Color(0xFF555555),
                            fontWeight = if (isSelectedLine || isError) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            VerticalDivider(
                color = Color(0xFF2A2A2E),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
            )

            // Syntax-Highlighted Text Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScrollState)
                    .padding(vertical = 12.dp, horizontal = 10.dp)
            ) {
                BasicTextField(
                    value = editorState.textFieldValue,
                    onValueChange = { newValue ->
                        editorState.onValueChange(newValue)
                        onContentChange(newValue.text)
                    },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color(0xFFEEEEEE)
                    ),
                    cursorBrush = SolidColor(StudioGreen),
                    visualTransformation = VisualTransformation { text ->
                        val highlighted = SyntaxHighlighter.highlight(
                            code = text.text,
                            fileType = fileType,
                            searchQuery = searchQuery,
                            errorLines = errorLines
                        )
                        TransformedText(highlighted, OffsetMapping.Identity)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

