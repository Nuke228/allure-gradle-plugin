package com.github.allureidea.gradle

import org.gradle.api.provider.Property

interface AllureTestOpsExtension {
    val url: Property<String>
    val token: Property<String>
    val projectId: Property<Long>
}
