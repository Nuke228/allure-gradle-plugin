package com.github.allureidea.gradle

import com.github.allureidea.gradle.api.AllureApiClient
import com.github.allureidea.gradle.api.AllureAuthManager
import com.github.allureidea.gradle.api.TestCaseRequest
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class SyncAllureTestCasesTask : DefaultTask() {

    @get:Input
    abstract val url: Property<String>

    @get:Input
    abstract val token: Property<String>

    @get:Input
    abstract val projectId: Property<Long>

    @TaskAction
    fun sync() {
        val baseUrl = url.getOrElse("").trimEnd('/')
        val apiToken = token.getOrElse("")
        val projId = projectId.getOrElse(0L)

        if (baseUrl.isBlank()) throw GradleException("allureTestOps.url is not set")
        if (apiToken.isBlank()) throw GradleException("allureTestOps.token is not set")
        if (projId <= 0) throw GradleException("allureTestOps.projectId is not set")

        // Authenticate
        val authManager = AllureAuthManager(baseUrl)
        val jwt = authManager.getValidJwt(apiToken)
        logger.lifecycle("Authenticated with Allure TestOps at $baseUrl")

        // Collect source directories from all subprojects and root
        val sourceDirs = collectTestSourceDirs(project)

        logger.lifecycle("Scanning source directories: ${sourceDirs.map { it.path }}")

        // Scan for @Test methods without @AllureId
        val unlinkedTests = SourceScanner.scan(sourceDirs)

        if (unlinkedTests.isEmpty()) {
            logger.lifecycle("No unlinked @Test methods found. Everything is synced!")
            return
        }

        logger.lifecycle("Found ${unlinkedTests.size} @Test method(s) without @AllureId")

        val apiClient = AllureApiClient(baseUrl)
        var synced = 0

        // Process files in reverse order of line numbers so insertions don't shift subsequent lines
        val groupedByFile = unlinkedTests.groupBy { it.file }

        for ((file, tests) in groupedByFile) {
            // Process in reverse line order to maintain correct line numbers
            val sortedTests = tests.sortedByDescending { it.testAnnotationLine }
            for (test in sortedTests) {
                try {
                    val request = TestCaseRequest(projectId = projId, name = test.testCaseName)
                    val response = apiClient.createTestCase(jwt, request)
                    SourceModifier.addAllureId(file, test.testAnnotationLine, response.id, test.indent)
                    logger.lifecycle("Created #${response.id} for '${test.testCaseName}' in ${file.name}")
                    synced++
                } catch (e: Exception) {
                    logger.error("Failed to sync '${test.methodName}' in ${file.name}: ${e.message}")
                }
            }
        }

        logger.lifecycle("Synced $synced test case(s)")
    }

    private fun collectTestSourceDirs(project: org.gradle.api.Project): List<File> {
        val dirs = mutableListOf<File>()
        val allProjects = listOf(project) + project.subprojects
        for (p in allProjects) {
            // Standard Kotlin/Java test source sets
            dirs.add(p.file("src/test/kotlin"))
            dirs.add(p.file("src/test/java"))
        }
        return dirs.filter { it.exists() }
    }
}
