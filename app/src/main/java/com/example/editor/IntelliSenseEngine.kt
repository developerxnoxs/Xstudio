package com.example.editor

data class CompletionItem(
    val label: String,
    val insertText: String,
    val kind: CompletionKind,
    val detail: String,
    val documentation: String = "",
    val cursorOffsetFromEnd: Int = 0 // How many chars from the end of insertText to place the cursor
)

enum class CompletionKind(val badge: String, val colorHex: Long) {
    COMPOSABLE("UI", 0xFF4CAF50),
    MODIFIER("MOD", 0xFF00BCD4),
    KEYWORD("KEY", 0xFFFF9800),
    FUNCTION("FUN", 0xFFE91E63),
    STATE("STATE", 0xFF9C27B0),
    SNIPPET("SNIP", 0xFF3F51B5),
    PROPERTY("PROP", 0xFF607D8B)
}

object IntelliSenseEngine {

    private val KOTLIN_COMPOSABLES = listOf(
        CompletionItem(
            label = "Column",
            insertText = "Column(\n    modifier = Modifier.fillMaxWidth()\n) {\n    \n}",
            kind = CompletionKind.COMPOSABLE,
            detail = "(modifier, verticalArrangement, horizontalAlignment)",
            documentation = "A layout composable that places its children in a vertical sequence.",
            cursorOffsetFromEnd = 3
        ),
        CompletionItem(
            label = "Row",
            insertText = "Row(\n    modifier = Modifier.fillMaxWidth(),\n    verticalAlignment = Alignment.CenterVertically\n) {\n    \n}",
            kind = CompletionKind.COMPOSABLE,
            detail = "(modifier, horizontalArrangement, verticalAlignment)",
            documentation = "A layout composable that places its children in a horizontal sequence.",
            cursorOffsetFromEnd = 3
        ),
        CompletionItem(
            label = "Box",
            insertText = "Box(\n    modifier = Modifier.fillMaxSize(),\n    contentAlignment = Alignment.Center\n) {\n    \n}",
            kind = CompletionKind.COMPOSABLE,
            detail = "(modifier, contentAlignment)",
            documentation = "A layout composable that stacks elements on top of each other.",
            cursorOffsetFromEnd = 3
        ),
        CompletionItem(
            label = "Text",
            insertText = "Text(text = \"\", fontSize = 14.sp, color = Color.White)",
            kind = CompletionKind.COMPOSABLE,
            detail = "(text, color, fontSize, fontWeight, modifier)",
            documentation = "High level element that displays text.",
            cursorOffsetFromEnd = 38
        ),
        CompletionItem(
            label = "Button",
            insertText = "Button(onClick = { /* TODO */ }) {\n    Text(\"Click Me\")\n}",
            kind = CompletionKind.COMPOSABLE,
            detail = "(onClick, modifier, enabled, colors)",
            documentation = "Material 3 Button component.",
            cursorOffsetFromEnd = 3
        ),
        CompletionItem(
            label = "FilledTonalButton",
            insertText = "FilledTonalButton(onClick = { /* TODO */ }) {\n    Text(\"Button\")\n}",
            kind = CompletionKind.COMPOSABLE,
            detail = "(onClick, modifier, enabled)",
            documentation = "Material 3 Filled Tonal Button.",
            cursorOffsetFromEnd = 3
        ),
        CompletionItem(
            label = "OutlinedButton",
            insertText = "OutlinedButton(onClick = { /* TODO */ }) {\n    Text(\"Button\")\n}",
            kind = CompletionKind.COMPOSABLE,
            detail = "(onClick, modifier)",
            documentation = "Material 3 Outlined Button.",
            cursorOffsetFromEnd = 3
        ),
        CompletionItem(
            label = "IconButton",
            insertText = "IconButton(onClick = { /* TODO */ }) {\n    Icon(Icons.Default.Favorite, contentDescription = null)\n}",
            kind = CompletionKind.COMPOSABLE,
            detail = "(onClick, modifier, enabled)",
            documentation = "Interactive icon button with ripple effect.",
            cursorOffsetFromEnd = 3
        ),
        CompletionItem(
            label = "Card",
            insertText = "Card(\n    modifier = Modifier.fillMaxWidth(),\n    shape = RoundedCornerShape(12.dp)\n) {\n    Column(modifier = Modifier.padding(16.dp)) {\n        \n    }\n}",
            kind = CompletionKind.COMPOSABLE,
            detail = "(modifier, shape, colors, elevation)",
            documentation = "Material 3 Card container for containing related information.",
            cursorOffsetFromEnd = 9
        ),
        CompletionItem(
            label = "Scaffold",
            insertText = "Scaffold(\n    topBar = { /* TopAppBar */ },\n    floatingActionButton = { /* FAB */ }\n) { paddingValues ->\n    Box(modifier = Modifier.padding(paddingValues)) {\n        \n    }\n}",
            kind = CompletionKind.COMPOSABLE,
            detail = "(topBar, bottomBar, floatingActionButton)",
            documentation = "Implements the basic Material Design visual layout structure.",
            cursorOffsetFromEnd = 9
        ),
        CompletionItem(
            label = "LazyColumn",
            insertText = "LazyColumn(\n    verticalArrangement = Arrangement.spacedBy(8.dp),\n    modifier = Modifier.fillMaxSize()\n) {\n    items(itemsList) { item ->\n        Text(item.toString())\n    }\n}",
            kind = CompletionKind.COMPOSABLE,
            detail = "(modifier, state, contentPadding)",
            documentation = "A vertically scrolling list that only composes and lays out currently visible items.",
            cursorOffsetFromEnd = 3
        ),
        CompletionItem(
            label = "LazyRow",
            insertText = "LazyRow(\n    horizontalArrangement = Arrangement.spacedBy(8.dp)\n) {\n    items(itemsList) { item ->\n        \n    }\n}",
            kind = CompletionKind.COMPOSABLE,
            detail = "(modifier, horizontalArrangement)",
            documentation = "A horizontally scrolling list with lazy evaluation.",
            cursorOffsetFromEnd = 9
        ),
        CompletionItem(
            label = "Spacer",
            insertText = "Spacer(modifier = Modifier.height(16.dp))",
            kind = CompletionKind.COMPOSABLE,
            detail = "(modifier)",
            documentation = "Component that represents an empty space.",
            cursorOffsetFromEnd = 0
        ),
        CompletionItem(
            label = "Surface",
            insertText = "Surface(\n    color = MaterialTheme.colorScheme.surface,\n    shape = RoundedCornerShape(8.dp)\n) {\n    \n}",
            kind = CompletionKind.COMPOSABLE,
            detail = "(modifier, shape, color, tonalElevation)",
            documentation = "Material surface container.",
            cursorOffsetFromEnd = 3
        ),
        CompletionItem(
            label = "Icon",
            insertText = "Icon(Icons.Default.Android, contentDescription = null, tint = Color.White)",
            kind = CompletionKind.COMPOSABLE,
            detail = "(imageVector, contentDescription, tint)",
            documentation = "Renders an ImageVector or painter icon.",
            cursorOffsetFromEnd = 0
        ),
        CompletionItem(
            label = "OutlinedTextField",
            insertText = "OutlinedTextField(\n    value = textState,\n    onValueChange = { textState = it },\n    label = { Text(\"Label\") },\n    modifier = Modifier.fillMaxWidth()\n)",
            kind = CompletionKind.COMPOSABLE,
            detail = "(value, onValueChange, label, placeholder)",
            documentation = "Material 3 Outlined text input field.",
            cursorOffsetFromEnd = 0
        ),
        CompletionItem(
            label = "TopAppBar",
            insertText = "TopAppBar(\n    title = { Text(\"Title\") },\n    navigationIcon = {\n        IconButton(onClick = {}) {\n            Icon(Icons.Default.ArrowBack, contentDescription = \"Back\")\n        }\n    }\n)",
            kind = CompletionKind.COMPOSABLE,
            detail = "(title, navigationIcon, actions)",
            documentation = "Material 3 Top App Bar.",
            cursorOffsetFromEnd = 0
        )
    )

    private val KOTLIN_MODIFIERS = listOf(
        CompletionItem("Modifier.fillMaxSize()", "Modifier.fillMaxSize()", CompletionKind.MODIFIER, "Modifier", "Fill max available size in parent."),
        CompletionItem("Modifier.fillMaxWidth()", "Modifier.fillMaxWidth()", CompletionKind.MODIFIER, "Modifier", "Fill max available width."),
        CompletionItem("Modifier.fillMaxHeight()", "Modifier.fillMaxHeight()", CompletionKind.MODIFIER, "Modifier", "Fill max available height."),
        CompletionItem("Modifier.padding()", "Modifier.padding(16.dp)", CompletionKind.MODIFIER, "Modifier (all: Dp)", "Apply inner padding to element."),
        CompletionItem("Modifier.padding(horizontal, vertical)", "Modifier.padding(horizontal = 16.dp, vertical = 8.dp)", CompletionKind.MODIFIER, "Modifier", "Apply asymmetric padding."),
        CompletionItem("Modifier.background()", "Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))", CompletionKind.MODIFIER, "Modifier", "Paint background color and shape."),
        CompletionItem("Modifier.clickable()", "Modifier.clickable { /* onClick */ }", CompletionKind.MODIFIER, "Modifier", "Make component clickable with ripple."),
        CompletionItem("Modifier.clip()", "Modifier.clip(RoundedCornerShape(12.dp))", CompletionKind.MODIFIER, "Modifier", "Clip element to a shape."),
        CompletionItem("Modifier.size()", "Modifier.size(48.dp)", CompletionKind.MODIFIER, "Modifier (size: Dp)", "Set width and height simultaneously."),
        CompletionItem("Modifier.width()", "Modifier.width(120.dp)", CompletionKind.MODIFIER, "Modifier (width: Dp)", "Set fixed width."),
        CompletionItem("Modifier.height()", "Modifier.height(48.dp)", CompletionKind.MODIFIER, "Modifier (height: Dp)", "Set fixed height."),
        CompletionItem("Modifier.weight()", "Modifier.weight(1f)", CompletionKind.MODIFIER, "Modifier (weight: Float)", "RowScope / ColumnScope weight distribution."),
        CompletionItem("Modifier.align()", "Modifier.align(Alignment.Center)", CompletionKind.MODIFIER, "Modifier", "Align within Box or Row/Column.")
    )

    private val KOTLIN_STATE_SNIPPETS = listOf(
        CompletionItem(
            label = "remember { mutableStateOf() }",
            insertText = "var value by remember { mutableStateOf(\"\") }",
            kind = CompletionKind.STATE,
            detail = "State holder",
            documentation = "Creates a local mutable state remembered across recompositions."
        ),
        CompletionItem(
            label = "LaunchedEffect",
            insertText = "LaunchedEffect(Unit) {\n    \n}",
            kind = CompletionKind.STATE,
            detail = "(key1, block)",
            documentation = "Runs suspend functions in the scope of a Composable when keys change.",
            cursorOffsetFromEnd = 3
        ),
        CompletionItem(
            label = "rememberCoroutineScope",
            insertText = "val coroutineScope = rememberCoroutineScope()",
            kind = CompletionKind.STATE,
            detail = "CoroutineScope",
            documentation = "Returns a CoroutineScope bound to this point in composition."
        ),
        CompletionItem(
            label = "derivedStateOf",
            insertText = "val calculatedState by remember { derivedStateOf { /* calculation */ } }",
            kind = CompletionKind.STATE,
            detail = "Derived State",
            documentation = "Creates a State whose value is derived from other States."
        )
    )

    private val KOTLIN_KEYWORDS_SNIPPETS = listOf(
        CompletionItem(
            label = "@Composable fun",
            insertText = "@Composable\nfun MyComponent(\n    modifier: Modifier = Modifier\n) {\n    \n}",
            kind = CompletionKind.SNIPPET,
            detail = "New Composable function",
            documentation = "Declares a new Jetpack Compose UI component.",
            cursorOffsetFromEnd = 3
        ),
        CompletionItem(
            label = "fun",
            insertText = "fun myFunctionName() {\n    \n}",
            kind = CompletionKind.KEYWORD,
            detail = "Function declaration",
            documentation = "Defines a Kotlin function.",
            cursorOffsetFromEnd = 3
        ),
        CompletionItem(
            label = "data class",
            insertText = "data class MyModel(\n    val id: String,\n    val name: String\n)",
            kind = CompletionKind.SNIPPET,
            detail = "Data class declaration",
            documentation = "Creates a Kotlin data class with auto-generated equals, hashCode, and copy.",
            cursorOffsetFromEnd = 0
        ),
        CompletionItem(
            label = "when",
            insertText = "when (target) {\n    is String -> { /* TODO */ }\n    else -> { /* TODO */ }\n}",
            kind = CompletionKind.KEYWORD,
            detail = "when statement / expression",
            documentation = "Kotlin pattern matching conditional expression.",
            cursorOffsetFromEnd = 3
        ),
        CompletionItem(
            label = "Log.d",
            insertText = "Log.d(\"TAG\", \"Debug message: \")",
            kind = CompletionKind.FUNCTION,
            detail = "Android Log",
            documentation = "Sends a DEBUG log output to Logcat.",
            cursorOffsetFromEnd = 2
        ),
        CompletionItem(
            label = "Toast.makeText",
            insertText = "Toast.makeText(context, \"Hello!\", Toast.LENGTH_SHORT).show()",
            kind = CompletionKind.FUNCTION,
            detail = "Android Toast",
            documentation = "Shows a brief popup notification on the screen.",
            cursorOffsetFromEnd = 24
        )
    )

    private val ALL_KOTLIN_COMPLETIONS = KOTLIN_COMPOSABLES + KOTLIN_MODIFIERS + KOTLIN_STATE_SNIPPETS + KOTLIN_KEYWORDS_SNIPPETS

    fun getCompletions(
        prefix: String,
        fileType: String = "KOTLIN",
        maxResults: Int = 8
    ): List<CompletionItem> {
        val query = prefix.trim().lowercase()
        if (query.isEmpty()) {
            return ALL_KOTLIN_COMPLETIONS.take(maxResults)
        }

        return ALL_KOTLIN_COMPLETIONS
            .filter { item ->
                item.label.lowercase().contains(query) ||
                        item.insertText.lowercase().contains(query) ||
                        item.detail.lowercase().contains(query)
            }
            .sortedBy { item ->
                when {
                    item.label.lowercase().startsWith(query) -> 0
                    item.label.lowercase().contains(query) -> 1
                    else -> 2
                }
            }
            .take(maxResults)
    }

    /**
     * Extracts the current partial word right before the cursor position.
     */
    fun extractWordAtCursor(text: String, cursorIndex: Int): Pair<String, Int> {
        if (cursorIndex <= 0 || text.isEmpty()) return Pair("", 0)

        val safeCursor = cursorIndex.coerceIn(0, text.length)
        var start = safeCursor - 1

        while (start >= 0) {
            val ch = text[start]
            if (ch.isLetterOrDigit() || ch == '_' || ch == '.' || ch == '@') {
                start--
            } else {
                break
            }
        }

        val wordStart = start + 1
        val word = text.substring(wordStart, safeCursor)
        return Pair(word, wordStart)
    }

    /**
     * Formats code with standard indentations and clean spacing.
     */
    fun formatCode(source: String, fileType: String): String {
        val lines = source.lines()
        val result = StringBuilder()
        var indentLevel = 0
        val indentUnit = "    " // 4 spaces standard

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) {
                result.append("\n")
                continue
            }

            // Adjust indent down before line if it closes blocks
            val closingCount = trimmed.count { it == '}' || it == ')' || it == ']' }
            val openingCount = trimmed.count { it == '{' || it == '(' || it == '[' }

            var effectiveIndent = indentLevel
            if (trimmed.startsWith("}") || trimmed.startsWith(")") || trimmed.startsWith("]")) {
                effectiveIndent = (indentLevel - 1).coerceAtLeast(0)
            }

            for (i in 0 until effectiveIndent) {
                result.append(indentUnit)
            }

            result.append(trimmed)
            result.append("\n")

            indentLevel = (indentLevel + openingCount - closingCount).coerceAtLeast(0)
        }

        return result.toString().trimEnd() + "\n"
    }
}
