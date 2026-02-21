package com.github.allureidea.gradle

import java.io.File

object SourceModifier {

    private const val ALLURE_ID_IMPORT = "import io.qameta.allure.AllureId"

    fun addAllureId(file: File, testAnnotationLine: Int, allureId: Long, indent: String) {
        val lines = file.readLines().toMutableList()

        // Insert @AllureId("N") right before @Test
        lines.add(testAnnotationLine, "${indent}@AllureId(\"$allureId\")")

        // Ensure import exists
        ensureImport(lines)

        file.writeText(lines.joinToString("\n") + "\n")
    }

    private fun ensureImport(lines: MutableList<String>) {
        if (lines.any { it.trim() == ALLURE_ID_IMPORT }) return

        // Find the last import line and insert after it
        val lastImportIndex = lines.indexOfLast { it.trimStart().startsWith("import ") }
        if (lastImportIndex >= 0) {
            lines.add(lastImportIndex + 1, ALLURE_ID_IMPORT)
        } else {
            // No imports found — insert after package declaration
            val packageIndex = lines.indexOfFirst { it.trimStart().startsWith("package ") }
            if (packageIndex >= 0) {
                lines.add(packageIndex + 1, "")
                lines.add(packageIndex + 2, ALLURE_ID_IMPORT)
            } else {
                lines.add(0, ALLURE_ID_IMPORT)
            }
        }
    }
}
