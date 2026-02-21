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

    private val testAnnotationRegex = Regex("""^(\s*)@Test\b""")
    private val allureIdRegex = Regex("""@AllureId\b""")
    private val displayNameRegex = Regex("""@DisplayName\(\s*"([^"]+)"\s*\)""")
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

                val hasAllureId = hasAllureIdInAnnotationBlock(lines, testAnnotationLine)

                if (!hasAllureId) {
                    val methodName = findMethodName(lines, testAnnotationLine + 1)
                    if (methodName != null) {
                        val displayName = findDisplayName(lines, testAnnotationLine)
                        results.add(UnlinkedTest(file, methodName, displayName, testAnnotationLine, indent))
                    }
                }
            }
            i++
        }

        return results
    }

    private fun findDisplayName(lines: List<String>, testLine: Int): String? {
        // Search backwards through annotation block
        var j = testLine
        while (j >= 0) {
            val trimmed = lines[j].trim()
            displayNameRegex.find(trimmed)?.let { return it.groupValues[1] }
            if (j < testLine && !trimmed.startsWith("@") && trimmed.isNotEmpty()) break
            j--
        }
        // Search forwards through annotation block (between @Test and fun)
        var k = testLine + 1
        while (k < lines.size) {
            val trimmed = lines[k].trim()
            displayNameRegex.find(trimmed)?.let { return it.groupValues[1] }
            if (methodRegex.containsMatchIn(lines[k]) || javaMethodRegex.containsMatchIn(lines[k])) break
            if (trimmed.isNotEmpty() && !trimmed.startsWith("@")) break
            k++
        }
        return null
    }

    private fun hasAllureIdInAnnotationBlock(lines: List<String>, testLine: Int): Boolean {
        var j = testLine
        while (j >= 0) {
            val trimmed = lines[j].trim()
            if (allureIdRegex.containsMatchIn(trimmed)) return true
            if (j < testLine && !trimmed.startsWith("@") && trimmed.isNotEmpty()) break
            j--
        }
        var k = testLine + 1
        while (k < lines.size) {
            val trimmed = lines[k].trim()
            if (allureIdRegex.containsMatchIn(trimmed)) return true
            if (methodRegex.containsMatchIn(lines[k]) || javaMethodRegex.containsMatchIn(lines[k])) break
            if (trimmed.isNotEmpty() && !trimmed.startsWith("@")) break
            k++
        }
        return false
    }

    private fun findMethodName(lines: List<String>, startLine: Int): String? {
        var i = startLine
        while (i < lines.size && i < startLine + 10) {
            methodRegex.find(lines[i])?.let { m ->
                return m.groupValues[1].ifEmpty { m.groupValues[2] }
            }
            javaMethodRegex.find(lines[i])?.let { return it.groupValues[1] }

            val trimmed = lines[i].trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("@")) break
            i++
        }
        return null
    }
}
