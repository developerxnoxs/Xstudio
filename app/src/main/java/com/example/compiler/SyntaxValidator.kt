package com.example.compiler

import com.example.core.BuildDiagnostic
import com.example.core.DiagnosticType
import com.example.data.local.ProjectFileEntity

object SyntaxValidator {

    fun validateFile(file: ProjectFileEntity): List<BuildDiagnostic> {
        val diagnostics = mutableListOf<BuildDiagnostic>()
        val lines = file.content.lines()

        when (file.fileType.uppercase()) {
            "KOTLIN" -> {
                validateCode(file, lines, diagnostics, isKotlin = true)
                validateKotlinSpecific(file, lines, diagnostics)
            }
            "GRADLE" -> {
                validateCode(file, lines, diagnostics, isKotlin = true)
                validateGradleDsl(file, lines, diagnostics)
            }
            "JAVA" -> {
                validateCode(file, lines, diagnostics, isKotlin = false)
                validateJavaSpecific(file, lines, diagnostics)
            }
            "XML", "MANIFEST" -> validateXml(file, lines, diagnostics)
        }

        return diagnostics
    }

    private fun validateKotlinSpecific(
        file: ProjectFileEntity,
        lines: List<String>,
        diagnostics: MutableList<BuildDiagnostic>
    ) {
        for ((index, rawLine) in lines.withIndex()) {
            val lineNum = index + 1
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) continue

            // Incomplete val/var declaration (e.g. "val x =" or "var myVar =")
            if (trimmed.matches(Regex("^(val|var)\\s+[A-Za-z0-9_]+\\s*=\\s*$"))) {
                addDiagnostic(
                    diagnostics, file, lineNum,
                    "Incomplete variable initialization. Missing expression after '='",
                    DiagnosticType.KOTLIN_COMPILATION,
                    "Assign a value or expression after '='"
                )
            }

            // Incomplete function declaration e.g. "fun ()" or "fun myMethod" without ()
            if (trimmed.startsWith("fun ") && !trimmed.contains("(") && !trimmed.contains("`")) {
                addDiagnostic(
                    diagnostics, file, lineNum,
                    "Invalid function declaration. Missing parameter list '()'",
                    DiagnosticType.KOTLIN_COMPILATION,
                    "Add parameter list e.g. 'fun methodName()'"
                )
            }

            // Package or import missing identifier
            if (trimmed == "package" || trimmed == "import") {
                addDiagnostic(
                    diagnostics, file, lineNum,
                    "Incomplete '$trimmed' statement",
                    DiagnosticType.KOTLIN_COMPILATION,
                    "Specify package or import target"
                )
            }
        }
    }

    private fun validateGradleDsl(
        file: ProjectFileEntity,
        lines: List<String>,
        diagnostics: MutableList<BuildDiagnostic>
    ) {
        for ((index, rawLine) in lines.withIndex()) {
            val lineNum = index + 1
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("/*")) continue

            // Groovy single quote dependency check in Kotlin DSL: implementation 'androidx.core:core-ktx:1.12.0'
            if (trimmed.matches(Regex("^(implementation|api|ksp|testImplementation|androidTestImplementation)\\s+'[^']+'$"))) {
                val dep = trimmed.substringAfter("'").substringBefore("'")
                addDiagnostic(
                    diagnostics, file, lineNum,
                    "Invalid Groovy string syntax in Kotlin Gradle DSL (.gradle.kts)",
                    DiagnosticType.GRADLE_SCRIPT,
                    "Change to: ${trimmed.substringBefore(" ")} (\"$dep\")"
                )
            }

            // Groovy apply plugin: 'foo'
            if (trimmed.startsWith("apply plugin:")) {
                addDiagnostic(
                    diagnostics, file, lineNum,
                    "Groovy plugin syntax is invalid in Kotlin DSL",
                    DiagnosticType.GRADLE_SCRIPT,
                    "Use plugins { id(\"...\") } or alias(libs.plugins....)"
                )
            }
        }
    }

    private fun validateJavaSpecific(
        file: ProjectFileEntity,
        lines: List<String>,
        diagnostics: MutableList<BuildDiagnostic>
    ) {
        for ((index, rawLine) in lines.withIndex()) {
            val lineNum = index + 1
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*") || trimmed.startsWith("@")) continue

            // Check for missing semicolon on single-line return, package, import, throw, or variable assignment in Java
            if (trimmed.startsWith("package ") || trimmed.startsWith("import ") || trimmed.startsWith("return ") || trimmed.startsWith("throw ")) {
                if (!trimmed.endsWith(";") && !trimmed.endsWith("{") && !trimmed.endsWith("}")) {
                    addDiagnostic(
                        diagnostics, file, lineNum,
                        "Missing semicolon ';' at end of statement",
                        DiagnosticType.JAVA_COMPILATION,
                        "Add ';' to terminate the statement"
                    )
                }
            }
        }
    }

    private fun addDiagnostic(
        diagnostics: MutableList<BuildDiagnostic>,
        file: ProjectFileEntity,
        line: Int,
        message: String,
        errorType: DiagnosticType,
        suggestion: String? = null
    ) {
        val snippet = GradleConsoleParser.extractSnippet(file, line)
        val defaultSuggestion = suggestion ?: when {
            message.contains("brace") -> "Check matching opening and closing braces '{ ... }'"
            message.contains("parenthesis") -> "Check matching parentheses '( ... )'"
            message.contains("bracket") -> "Check matching brackets '[ ... ]'"
            message.contains("string literal") -> "Check unclosed quote '\"'"
            message.contains("XML") || message.contains("tag") -> "Ensure opening XML tags have matching closing tags </tag>"
            else -> "Review the code around line $line"
        }

        diagnostics.add(
            BuildDiagnostic(
                fileName = file.name,
                filePath = file.path,
                line = line,
                column = 1,
                message = message,
                rawLogLine = "e: ${file.path}:$line: $message",
                isWarning = false,
                errorType = errorType,
                codeSnippet = snippet,
                suggestion = defaultSuggestion
            )
        )
    }

    private fun validateCode(
        file: ProjectFileEntity,
        lines: List<String>,
        diagnostics: MutableList<BuildDiagnostic>,
        isKotlin: Boolean
    ) {
        var openBraces = 0
        var openParens = 0
        var openBrackets = 0

        var inBlockComment = false
        var inTripleQuoteString = false

        val diagType = if (isKotlin) DiagnosticType.KOTLIN_COMPILATION else DiagnosticType.JAVA_COMPILATION

        for ((index, rawLine) in lines.withIndex()) {
            val lineNum = index + 1
            var i = 0
            val len = rawLine.length

            var inSingleLineString = false
            var inCharLiteral = false

            while (i < len) {
                // If inside block comment
                if (inBlockComment) {
                    if (i + 1 < len && rawLine[i] == '*' && rawLine[i + 1] == '/') {
                        inBlockComment = false
                        i += 2
                        continue
                    }
                    i++
                    continue
                }

                // If inside triple-quoted string """..."""
                if (inTripleQuoteString) {
                    if (i + 2 < len && rawLine[i] == '"' && rawLine[i + 1] == '"' && rawLine[i + 2] == '"') {
                        inTripleQuoteString = false
                        i += 3
                        continue
                    }
                    i++
                    continue
                }

                // Check start of triple quote
                if (!inSingleLineString && !inCharLiteral && i + 2 < len && rawLine[i] == '"' && rawLine[i + 1] == '"' && rawLine[i + 2] == '"') {
                    inTripleQuoteString = true
                    i += 3
                    continue
                }

                // Check start of single line comment
                if (!inSingleLineString && !inCharLiteral && i + 1 < len && rawLine[i] == '/' && rawLine[i + 1] == '/') {
                    break // Rest of line is comment
                }

                // Check start of block comment
                if (!inSingleLineString && !inCharLiteral && i + 1 < len && rawLine[i] == '/' && rawLine[i + 1] == '*') {
                    inBlockComment = true
                    i += 2
                    continue
                }

                val c = rawLine[i]

                // Check escape character inside string/char
                if ((inSingleLineString || inCharLiteral) && c == '\\') {
                    i += 2 // Skip escaped character
                    continue
                }

                // Check char literal
                if (!inSingleLineString) {
                    if (c == '\'') {
                        inCharLiteral = !inCharLiteral
                        i++
                        continue
                    }
                }

                // Check string literal
                if (!inCharLiteral) {
                    if (c == '"') {
                        inSingleLineString = !inSingleLineString
                        i++
                        continue
                    }
                }

                // If inside string or char, skip code balance checks
                if (inSingleLineString || inCharLiteral) {
                    i++
                    continue
                }

                // Normal code characters
                when (c) {
                    '{' -> openBraces++
                    '}' -> {
                        openBraces--
                        if (openBraces < 0) {
                            addDiagnostic(
                                diagnostics, file, lineNum,
                                "Unexpected closing brace '}'",
                                diagType,
                                "Remove redundant '}' or add missing opening brace '{'"
                            )
                            openBraces = 0
                        }
                    }
                    '(' -> openParens++
                    ')' -> {
                        openParens--
                        if (openParens < 0) {
                            addDiagnostic(
                                diagnostics, file, lineNum,
                                "Unexpected closing parenthesis ')'",
                                diagType,
                                "Remove redundant ')' or add missing opening parenthesis '('"
                            )
                            openParens = 0
                        }
                    }
                    '[' -> openBrackets++
                    ']' -> {
                        openBrackets--
                        if (openBrackets < 0) {
                            addDiagnostic(
                                diagnostics, file, lineNum,
                                "Unexpected closing bracket ']'",
                                diagType,
                                "Remove redundant ']' or add missing opening bracket '['"
                            )
                            openBrackets = 0
                        }
                    }
                }

                i++
            }

            if (inSingleLineString) {
                addDiagnostic(
                    diagnostics, file, lineNum,
                    "Unterminated string literal at line $lineNum",
                    diagType,
                    "Add closing double quote '\"' at end of string"
                )
            }
        }

        if (inBlockComment) {
            addDiagnostic(
                diagnostics, file, lines.size.coerceAtLeast(1),
                "Unclosed block comment '/* ... */'",
                diagType,
                "Add '*/' to close the block comment"
            )
        }

        if (inTripleQuoteString) {
            addDiagnostic(
                diagnostics, file, lines.size.coerceAtLeast(1),
                "Unclosed multiline string literal '\"\"\"'",
                diagType,
                "Add triple quotes '\"\"\"' to close the multiline string"
            )
        }

        if (openBraces > 0) {
            addDiagnostic(
                diagnostics, file, lines.size.coerceAtLeast(1),
                "Missing closing brace '}' at end of file (Unclosed $openBraces block(s))",
                diagType,
                "Add '}' at the end of class/function block"
            )
        }

        if (openParens > 0) {
            addDiagnostic(
                diagnostics, file, lines.size.coerceAtLeast(1),
                "Missing closing parenthesis ')' at end of file (Unclosed $openParens group(s))",
                diagType,
                "Add ')' to close open parameter group"
            )
        }

        if (openBrackets > 0) {
            addDiagnostic(
                diagnostics, file, lines.size.coerceAtLeast(1),
                "Missing closing bracket ']' at end of file",
                diagType,
                "Add ']' to close open array/index expression"
            )
        }
    }

    private fun validateXml(
        file: ProjectFileEntity,
        lines: List<String>,
        diagnostics: MutableList<BuildDiagnostic>
    ) {
        val content = file.content
        if (content.isBlank()) return

        val diagType = if (file.fileType.equals("MANIFEST", ignoreCase = true) || file.name == "AndroidManifest.xml") {
            DiagnosticType.MANIFEST_MERGER
        } else {
            DiagnosticType.AAPT2_RESOURCE
        }

        // 1. Try standard Android XML pull parser for 100% accurate XML syntax validation
        try {
            val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(java.io.StringReader(content))

            var eventType = parser.eventType
            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                eventType = parser.next()
            }
            return // XML is completely valid!
        } catch (e: org.xmlpull.v1.XmlPullParserException) {
            val line = e.lineNumber.let { if (it > 0) it else 1 }
            val cleanMessage = e.message
                ?.substringBefore("(position:")
                ?.trim()
                ?.ifEmpty { "XML syntax error" }
                ?: "XML syntax error"

            addDiagnostic(
                diagnostics, file, line,
                cleanMessage,
                diagType,
                "Check XML tags syntax, attribute quotes, and proper closing tags"
            )
            return
        } catch (e: Exception) {
            // Fallback to token parser
        }

        // Fallback robust token parser on full content (handles multiline tags & comments)
        val tagStack = java.util.ArrayDeque<Pair<String, Int>>()
        var i = 0
        val len = content.length
        var currentLine = 1

        while (i < len) {
            val c = content[i]
            if (c == '\n') {
                currentLine++
                i++
                continue
            }

            // Check comments <!-- ... -->
            if (c == '<' && i + 3 < len && content.startsWith("<!--", i)) {
                val endComment = content.indexOf("-->", i + 4)
                if (endComment == -1) {
                    addDiagnostic(
                        diagnostics, file, currentLine,
                        "Unclosed XML comment '<!-- ... -->'",
                        diagType,
                        "Add '-->' to close XML comment"
                    )
                    break
                }
                for (k in i until endComment + 3) {
                    if (content[k] == '\n') currentLine++
                }
                i = endComment + 3
                continue
            }

            // Check processing instruction <?xml ... ?>
            if (c == '<' && i + 1 < len && content[i + 1] == '?') {
                val endPI = content.indexOf("?>", i + 2)
                if (endPI == -1) {
                    addDiagnostic(
                        diagnostics, file, currentLine,
                        "Unclosed XML processing instruction '<?...?>'",
                        diagType,
                        "Add '?>' to close XML declaration"
                    )
                    break
                }
                for (k in i until endPI + 2) {
                    if (content[k] == '\n') currentLine++
                }
                i = endPI + 2
                continue
            }

            // Check DOCTYPE <!DOCTYPE ... >
            if (c == '<' && i + 8 < len && content.startsWith("<!DOCTYPE", i, ignoreCase = true)) {
                val endDoc = content.indexOf('>', i + 9)
                if (endDoc != -1) {
                    for (k in i..endDoc) {
                        if (content[k] == '\n') currentLine++
                    }
                    i = endDoc + 1
                    continue
                }
            }

            // Check tag start <
            if (c == '<') {
                val tagStartLine = currentLine
                val isClosing = i + 1 < len && content[i + 1] == '/'
                val nameStartIndex = if (isClosing) i + 2 else i + 1

                // Find end of tag name
                var nameEndIndex = nameStartIndex
                while (nameEndIndex < len && !content[nameEndIndex].isWhitespace() && content[nameEndIndex] != '>' && content[nameEndIndex] != '/') {
                    nameEndIndex++
                }
                val tagName = content.substring(nameStartIndex, nameEndIndex).trim()

                // Find closing '>' taking into account quotes in attributes
                var tagEndIndex = nameEndIndex
                var inQuote = false
                var quoteChar = '"'
                var isSelfClosing = false

                while (tagEndIndex < len) {
                    val tc = content[tagEndIndex]
                    if (tc == '\n') currentLine++

                    if (!inQuote && (tc == '"' || tc == '\'')) {
                        inQuote = true
                        quoteChar = tc
                    } else if (inQuote && tc == quoteChar) {
                        inQuote = false
                    } else if (!inQuote && tc == '/') {
                        if (tagEndIndex + 1 < len && content[tagEndIndex + 1] == '>') {
                            isSelfClosing = true
                        }
                    } else if (!inQuote && tc == '>') {
                        break
                    }
                    tagEndIndex++
                }

                if (tagEndIndex >= len) {
                    addDiagnostic(
                        diagnostics, file, tagStartLine,
                        "Unclosed tag '<$tagName>'",
                        diagType,
                        "Add closing '>' to finish tag declaration"
                    )
                    break
                }

                if (tagName.isNotEmpty()) {
                    if (isClosing) {
                        if (tagStack.isNotEmpty() && tagStack.peek().first == tagName) {
                            tagStack.pop()
                        } else if (tagStack.isNotEmpty()) {
                            val expected = tagStack.peek().first
                            addDiagnostic(
                                diagnostics, file, tagStartLine,
                                "Mismatched closing tag '</$tagName>', expected '</$expected>'",
                                diagType,
                                "Replace '</$tagName>' with '</$expected>' or close nested tags properly"
                            )
                        } else {
                            addDiagnostic(
                                diagnostics, file, tagStartLine,
                                "Unexpected closing tag '</$tagName>'",
                                diagType,
                                "Remove redundant '</$tagName>' or add corresponding opening tag"
                            )
                        }
                    } else if (!isSelfClosing) {
                        tagStack.push(tagName to tagStartLine)
                    }
                }

                i = tagEndIndex + 1
                continue
            }

            i++
        }

        while (tagStack.isNotEmpty()) {
            val unclosed = tagStack.pop()
            addDiagnostic(
                diagnostics, file, unclosed.second,
                "Unclosed XML tag '<${unclosed.first}>'",
                diagType,
                "Add '</${unclosed.first}>' to close tag"
            )
        }
    }
}

