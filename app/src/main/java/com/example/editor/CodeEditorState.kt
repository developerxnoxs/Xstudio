package com.example.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

class CodeEditorStateManager(initialText: String = "") {

    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    private val maxHistory = 50

    var textFieldValue by mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length)))
        private set

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun setText(newText: String) {
        textFieldValue = TextFieldValue(newText, TextRange(newText.length))
        undoStack.clear()
        redoStack.clear()
    }

    fun onValueChange(newValue: TextFieldValue) {
        val old = textFieldValue
        if (old.text != newValue.text) {
            // Push to undo stack
            if (undoStack.size >= maxHistory) {
                undoStack.removeAt(0)
            }
            undoStack.add(old)
            redoStack.clear()
        }
        textFieldValue = newValue
    }

    fun undo(): String? {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(textFieldValue)
            textFieldValue = previous
            return previous.text
        }
        return null
    }

    fun redo(): String? {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(textFieldValue)
            textFieldValue = next
            return next.text
        }
        return null
    }

    fun insertSnippet(snippet: String): String {
        val currentText = textFieldValue.text
        val selection = textFieldValue.selection
        val newText = if (selection.start != selection.end) {
            currentText.replaceRange(selection.start, selection.end, snippet)
        } else {
            val idx = selection.start.coerceIn(0, currentText.length)
            currentText.substring(0, idx) + snippet + currentText.substring(idx)
        }
        val newCursor = (selection.start + snippet.length).coerceIn(0, newText.length)
        val updated = TextFieldValue(newText, TextRange(newCursor))
        onValueChange(updated)
        return newText
    }

    fun handleBracketAutoClose(char: Char): Boolean {
        val pair = when (char) {
            '{' -> "{}"
            '(' -> "()"
            '[' -> "[]"
            '"' -> "\"\""
            '\'' -> "''"
            else -> return false
        }
        val currentText = textFieldValue.text
        val selection = textFieldValue.selection
        val start = selection.start
        val newText = currentText.substring(0, start) + pair + currentText.substring(start)
        val updated = TextFieldValue(newText, TextRange(start + 1))
        onValueChange(updated)
        return true
    }

    fun handleAutoIndent(): String {
        val currentText = textFieldValue.text
        val selection = textFieldValue.selection
        val start = selection.start
        // Find current line indent
        val lastNewline = currentText.lastIndexOf('\n', (start - 1).coerceAtLeast(0))
        val currentLineStart = if (lastNewline == -1) 0 else lastNewline + 1
        val currentLine = currentText.substring(currentLineStart, start)
        val indent = currentLine.takeWhile { it == ' ' || it == '\t' }
        val extraIndent = if (currentLine.trimEnd().endsWith("{")) "    " else ""

        val inserted = "\n" + indent + extraIndent
        val newText = currentText.substring(0, start) + inserted + currentText.substring(start)
        val updated = TextFieldValue(newText, TextRange(start + inserted.length))
        onValueChange(updated)
        return newText
    }

    fun setCursorToLine(lineNumber: Int) {
        val text = textFieldValue.text
        val lines = text.lines()
        if (lines.isEmpty()) return
        val targetLineIdx = (lineNumber - 1).coerceIn(0, lines.lastIndex)
        var charOffset = 0
        for (i in 0 until targetLineIdx) {
            charOffset += lines[i].length + 1
        }
        val lineLength = lines[targetLineIdx].length
        val endOffset = (charOffset + lineLength).coerceIn(charOffset, text.length)
        textFieldValue = TextFieldValue(text, TextRange(charOffset, endOffset))
    }

    fun replaceWordWithCompletion(wordStart: Int, wordLength: Int, completion: CompletionItem): String {
        val currentText = textFieldValue.text
        val safeStart = wordStart.coerceIn(0, currentText.length)
        val safeEnd = (safeStart + wordLength).coerceIn(safeStart, currentText.length)
        
        val newText = currentText.substring(0, safeStart) + completion.insertText + currentText.substring(safeEnd)
        val finalCursor = (safeStart + completion.insertText.length - completion.cursorOffsetFromEnd).coerceIn(0, newText.length)
        
        val updated = TextFieldValue(newText, TextRange(finalCursor))
        onValueChange(updated)
        return newText
    }
}
