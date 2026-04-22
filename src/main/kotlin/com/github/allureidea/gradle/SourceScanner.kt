package com.github.allureidea.gradle

import java.io.File

data class UnlinkedTest(
    val file: File,
    val methodName: String,
    val displayName: String?,
    val testAnnotationLine: Int,
    val indent: String
) {
    val testCaseName: String get() = displayName ?: methodName
}

object SourceScanner {

    private val testAnnotationRegex = Regex("""^(\s*)@(?:Test|ParameterizedTest)\b""")
    private val allureIdRegex = Regex("""@AllureId\b""")
    private val displayNameRegex = Regex("""@DisplayName\s*\(\s*"([^"]+)"\s*\)""")
    private val methodRegex = Regex("""^\s*(?:(?:public|private|protected|internal|open|abstract|override|suspend)\s+)*fun\s+(?:`([^`]+)`|(\w+))\s*\(""")
    private val javaMethodRegex = Regex("""^\s*(?:(?:public|private|protected|static|final|abstract|synchronized)\s+)*void\s+(\w+)\s*\(""")

    fun scan(sourceDirs: List<File>): List<UnlinkedTest> {
        val results = mutableListOf<UnlinkedTest>()

        for (dir in sourceDirs) {
            if (!dir.exists()) continue
            dir.walk()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .forEach { file ->
                    results.addAll(scanFile(file))
                }
        }

        return results
    }

    private fun scanFile(file: File): List<UnlinkedTest> {
        val lines = file.readLines()
        val results = mutableListOf<UnlinkedTest>()

        var i = 0
        while (i < lines.size) {
            val match = testAnnotationRegex.find(lines[i])
            if (match != null) {
                val indent = match.groupValues[1]
                val testAnnotationLine = i

                val blockText = annotationBlockText(lines, testAnnotationLine)
                val hasAllureId = allureIdRegex.containsMatchIn(blockText)

                if (!hasAllureId) {
                    val methodName = findMethodName(lines, testAnnotationLine + 1)
                    if (methodName != null) {
                        val displayName = displayNameRegex.find(blockText)?.groupValues?.get(1)
                        results.add(UnlinkedTest(file, methodName, displayName, testAnnotationLine, indent))
                    }
                }
            }
            i++
        }

        return results
    }

    /**
     * Returns the full annotation block surrounding the test-annotation line, joined with newlines.
     *
     * The block spans every annotation before and after the test-annotation line until the
     * enclosing function declaration (or a non-annotation, non-comment code line going backward).
     * Multi-line annotations (where parentheses open on one line and close on another) are kept
     * intact — the scan uses parenthesis balance so continuation lines don't terminate the block.
     */
    internal fun annotationBlockText(lines: List<String>, testLine: Int): String {
        // Scan forward: include lines until the function declaration (outside annotation parens).
        var end = testLine
        var fwdBalance = 0
        fwdBalance += parenDelta(lines[testLine])
        var k = testLine + 1
        while (k < lines.size) {
            val line = lines[k]
            if (fwdBalance == 0 && (methodRegex.containsMatchIn(line) || javaMethodRegex.containsMatchIn(line))) break
            val trimmed = line.trim()
            if (fwdBalance == 0 && trimmed.isNotEmpty() && !trimmed.startsWith("@") && !trimmed.startsWith("//")) break
            end = k
            fwdBalance += parenDelta(line)
            k++
        }

        // Scan backward: include lines that are annotations or belong to multi-line annotations.
        var start = testLine
        var bwdBalance = 0 // unclosed ')' when reading bottom-up
        var j = testLine - 1
        while (j >= 0) {
            val line = lines[j]
            bwdBalance += reverseParenDelta(line)
            val trimmed = line.trim()
            if (bwdBalance == 0 && trimmed.isNotEmpty() && !trimmed.startsWith("@") && !trimmed.startsWith("//")) {
                break
            }
            start = j
            j--
        }

        return lines.subList(start, end + 1).joinToString("\n")
    }

    private fun parenDelta(line: String): Int {
        var delta = 0
        for (c in line) {
            when (c) {
                '(' -> delta++
                ')' -> delta--
            }
        }
        return delta
    }

    private fun reverseParenDelta(line: String): Int {
        var delta = 0
        for (c in line) {
            when (c) {
                ')' -> delta++
                '(' -> delta--
            }
        }
        return delta
    }

    private fun findMethodName(lines: List<String>, startLine: Int): String? {
        var i = startLine
        var parenBalance = 0
        while (i < lines.size && i < startLine + 20) {
            val line = lines[i]
            val wasInsideAnnotation = parenBalance > 0
            if (parenBalance == 0) {
                methodRegex.find(line)?.let { m ->
                    return m.groupValues[1].ifEmpty { m.groupValues[2] }
                }
                javaMethodRegex.find(line)?.let { return it.groupValues[1] }
            }
            parenBalance += parenDelta(line)
            val trimmed = line.trim()
            // A closing line of a multi-line annotation ends with parenBalance == 0 but isn't real code,
            // so require that we started this line outside any annotation before treating it as a terminator.
            if (parenBalance == 0 && !wasInsideAnnotation &&
                trimmed.isNotEmpty() && !trimmed.startsWith("@") && !trimmed.startsWith("//")) break
            i++
        }
        return null
    }
}
