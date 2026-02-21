package com.github.allureidea.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class AllureTestOpsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "allureTestOps",
            AllureTestOpsExtension::class.java
        )

        project.tasks.register("syncAllureTestCases", SyncAllureTestCasesTask::class.java) {
            it.url.set(extension.url)
            it.token.set(extension.token)
            it.projectId.set(extension.projectId)
        }
    }
}
