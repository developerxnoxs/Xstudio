package com.example.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.*

object SyntaxHighlighter {

    private val KOTLIN_KEYWORDS = setOf(
        "package", "import", "class", "interface", "object", "enum", "fun", "val", "var",
        "override", "private", "public", "protected", "internal", "if", "else", "when",
        "for", "while", "return", "try", "catch", "finally", "throw", "null", "true", "false",
        "this", "super", "is", "as", "in", "by", "companion", "data", "sealed", "suspend",
        "abstract", "actual", "annotation", "expect", "external", "final", "infix", "inline",
        "inner", "lateinit", "noinline", "open", "operator", "out", "reified", "tailrec",
        "vararg", "const", "crossinline"
    )

    private val JAVA_KEYWORDS = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
        "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null"
    )

    private val COMMON_TYPES = setOf(
        "String", "Int", "Long", "Float", "Double", "Boolean", "List", "Map", "Set",
        "Modifier", "Color", "Dp", "Text", "Button", "Column", "Row", "Box", "Card",
        "Scaffold", "Unit", "Any", "ViewModel", "StateFlow", "Flow", "Composable",
        "Activity", "AppCompatActivity", "ComponentActivity", "Bundle", "Context",
        "Toast", "Intent", "TextView", "ImageView", "View", "ViewGroup", "LinearLayout",
        "RelativeLayout", "FrameLayout", "RecyclerView", "Adapter", "ViewHolder",
        "ArrayList", "HashMap", "HashSet", "CharSequence", "Object", "Integer"
    )

    fun highlight(
        code: String,
        fileType: String,
        searchQuery: String = "",
        errorLines: Set<Int> = emptySet()
    ): AnnotatedString {
        return buildAnnotatedString {
            append(code)

            when (fileType.uppercase()) {
                "KOTLIN" -> highlightKotlin(code)
                "JAVA" -> highlightJava(code)
                "XML", "MANIFEST" -> highlightXml(code)
                "GRADLE" -> highlightGradle(code)
                "JSON" -> highlightJson(code)
                else -> highlightKotlin(code)
            }

            // Search query highlighting
            if (searchQuery.isNotBlank() && searchQuery.length >= 2) {
                var index = 0
                while (index < code.length) {
                    val found = code.indexOf(searchQuery, index, ignoreCase = true)
                    if (found == -1) break
                    addStyle(
                        SpanStyle(
                            background = Color(0xFFFFD54F),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        ),
                        found,
                        found + searchQuery.length
                    )
                    index = found + searchQuery.length
                }
            }

            // Realtime syntax error line highlight with background tint and underline
            if (errorLines.isNotEmpty()) {
                val lines = code.lines()
                var currentOffset = 0
                for ((lineIdx, lineStr) in lines.withIndex()) {
                    val lineNum = lineIdx + 1
                    val lineLen = lineStr.length
                    if (errorLines.contains(lineNum) && lineLen > 0) {
                        addStyle(
                            SpanStyle(
                                background = Color(0x33FF5252),
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                            ),
                            currentOffset,
                            (currentOffset + lineLen).coerceAtMost(code.length)
                        )
                    }
                    currentOffset += lineLen + 1 // +1 for \n
                }
            }
        }
    }

    private fun AnnotatedString.Builder.highlightKotlin(code: String) {
        // Comments
        val commentRegex = Regex("//.*|/\\*[\\s\\S]*?\\*/")
        commentRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxComment), match.range.first, match.range.last + 1)
        }

        // Strings
        val stringRegex = Regex("\"\"\"[\\s\\S]*?\"\"\"|\"(\\\\.|[^\"])*\"")
        stringRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxString), match.range.first, match.range.last + 1)
        }

        // Annotations (@Composable, @OptIn, etc.)
        val annotationRegex = Regex("@[A-Za-z0-9_.]+")
        annotationRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxAnnotation, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
        }

        // Numbers
        val numberRegex = Regex("\\b\\d+(\\.\\d+)?(f|L)?\\b")
        numberRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxNumber), match.range.first, match.range.last + 1)
        }

        // Identifiers
        val wordRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
        wordRegex.findAll(code).forEach { match ->
            val word = match.value
            val start = match.range.first
            val end = match.range.last + 1

            if (KOTLIN_KEYWORDS.contains(word)) {
                addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold), start, end)
            } else if (COMMON_TYPES.contains(word) || (word.first().isUpperCase() && word != "TODO")) {
                addStyle(SpanStyle(color = SyntaxType), start, end)
            }
        }
    }

    private fun AnnotatedString.Builder.highlightJava(code: String) {
        // Comments
        val commentRegex = Regex("//.*|/\\*[\\s\\S]*?\\*/")
        commentRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxComment), match.range.first, match.range.last + 1)
        }

        // Strings & chars
        val stringRegex = Regex("\"(\\\\.|[^\"])*\"|'(\\\\.|[^'])*'")
        stringRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxString), match.range.first, match.range.last + 1)
        }

        // Annotations (@Override, @NonNull)
        val annotationRegex = Regex("@[A-Za-z0-9_.]+")
        annotationRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxAnnotation, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
        }

        // Numbers
        val numberRegex = Regex("\\b\\d+(\\.\\d+)?(f|L|d)?\\b")
        numberRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxNumber), match.range.first, match.range.last + 1)
        }

        // Identifiers
        val wordRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
        wordRegex.findAll(code).forEach { match ->
            val word = match.value
            val start = match.range.first
            val end = match.range.last + 1

            if (JAVA_KEYWORDS.contains(word)) {
                addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold), start, end)
            } else if (COMMON_TYPES.contains(word) || (word.first().isUpperCase() && word != "TODO")) {
                addStyle(SpanStyle(color = SyntaxType), start, end)
            }
        }
    }

    private fun AnnotatedString.Builder.highlightXml(code: String) {
        // XML comments
        val commentRegex = Regex("<!--[\\s\\S]*?-->")
        commentRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxComment), match.range.first, match.range.last + 1)
        }

        // XML strings
        val stringRegex = Regex("\"[^\"]*\"")
        stringRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxString), match.range.first, match.range.last + 1)
        }

        // XML tags
        val tagRegex = Regex("</?[A-Za-z0-9_\\-\\.:]+")
        tagRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxTag, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
        }

        // XML attributes
        val attrRegex = Regex("\\b[A-Za-z0-9_\\-]+:")
        attrRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxAttribute), match.range.first, match.range.last + 1)
        }
    }

    private fun AnnotatedString.Builder.highlightGradle(code: String) {
        highlightKotlin(code)
    }

    private fun AnnotatedString.Builder.highlightJson(code: String) {
        val stringRegex = Regex("\"[^\"]*\"")
        stringRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxString), match.range.first, match.range.last + 1)
        }
        val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
        numberRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxNumber), match.range.first, match.range.last + 1)
        }
    }
}
