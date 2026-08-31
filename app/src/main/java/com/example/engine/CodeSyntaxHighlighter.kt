package com.example.engine

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.*

object CodeSyntaxHighlighter {

    private val KOTLIN_KEYWORDS = setOf(
        "package", "import", "class", "interface", "object", "enum", "fun", "val", "var",
        "override", "private", "public", "protected", "internal", "if", "else", "when",
        "for", "while", "return", "try", "catch", "finally", "throw", "null", "true", "false",
        "this", "super", "is", "as", "in", "by", "companion", "data", "sealed", "suspend"
    )

    private val KOTLIN_TYPES = setOf(
        "String", "Int", "Long", "Float", "Double", "Boolean", "List", "Map", "Set",
        "Modifier", "Color", "Dp", "Text", "Button", "Column", "Row", "Box", "Card",
        "Scaffold", "Unit", "Any", "ViewModel", "StateFlow", "Flow", "Composable"
    )

    fun highlight(code: String, fileType: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)

            when (fileType.uppercase()) {
                "KOTLIN" -> highlightKotlin(code)
                "XML", "MANIFEST" -> highlightXml(code)
                "GRADLE" -> highlightGradle(code)
                "JSON" -> highlightJson(code)
                else -> highlightKotlin(code)
            }
        }
    }

    private fun AnnotatedString.Builder.highlightKotlin(code: String) {
        // 1. Comments
        val commentRegex = Regex("//.*|/\\*[\\s\\S]*?\\*/")
        commentRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxComment), match.range.first, match.range.last + 1)
        }

        // 2. Strings
        val stringRegex = Regex("\"(\\\\.|[^\"])*\"")
        stringRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxString), match.range.first, match.range.last + 1)
        }

        // 3. Annotations (@Composable, @Preview, etc.)
        val annotationRegex = Regex("@[A-Za-z0-9_]+")
        annotationRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxAnnotation, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
        }

        // 4. Numbers
        val numberRegex = Regex("\\b\\d+(\\.\\d+)?(f|L)?\\b")
        numberRegex.findAll(code).forEach { match ->
            addStyle(SpanStyle(color = SyntaxNumber), match.range.first, match.range.last + 1)
        }

        // 5. Words (Keywords, Types, Functions)
        val wordRegex = Regex("\\b[A-Za-z_][A-Za-z0-9_]*\\b")
        wordRegex.findAll(code).forEach { match ->
            val word = match.value
            val start = match.range.first
            val end = match.range.last + 1

            if (KOTLIN_KEYWORDS.contains(word)) {
                addStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.Bold), start, end)
            } else if (KOTLIN_TYPES.contains(word) || (word.first().isUpperCase() && word != "TODO")) {
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
