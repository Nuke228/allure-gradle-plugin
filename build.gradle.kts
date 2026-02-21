plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.3.1"
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
}

kotlin {
    jvmToolchain(25)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
}

gradlePlugin {
    website.set("https://github.com/Nuke228/allure-gradle-plugin")
    vcsUrl.set("https://github.com/Nuke228/allure-gradle-plugin")
    plugins {
        create("syncTestCases") {
            id = "io.github.nuke228.allure-sync-testcases"
            implementationClass = "com.github.allureidea.gradle.AllureTestOpsPlugin"
            displayName = "Allure TestOps Test Case Sync"
            description = "Scans source code for @Test methods without @AllureId, creates test cases in Allure TestOps and annotates the source back."
            tags.set(listOf("allure", "testops", "testing", "test-management"))
        }
    }
}
