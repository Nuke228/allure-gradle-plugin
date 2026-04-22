package com.github.allureidea.gradle

import java.io.File
import java.nio.file.Files
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SourceScannerTest {

    private fun scan(source: String): List<UnlinkedTest> {
        val tmpDir = Files.createTempDirectory("scanner-test").toFile()
        try {
            File(tmpDir, "Sample.kt").writeText(source)
            return SourceScanner.scan(listOf(tmpDir))
        } finally {
            tmpDir.deleteRecursively()
        }
    }

    @Test
    fun `detects @Test without @AllureId`() {
        val source =
            """
            package x
            class Foo {
                @Test
                @DisplayName("My test")
                fun `my test`() {}
            }
            """.trimIndent()

        val tests = scan(source)
        assertEquals(1, tests.size)
        assertEquals("my test", tests[0].methodName)
        assertEquals("My test", tests[0].displayName)
    }

    @Test
    fun `detects @ParameterizedTest without @AllureId`() {
        val source =
            """
            package x
            class Foo {
                @ParameterizedTest
                @DisplayName("My parametrized test")
                @MethodSource("args")
                fun `my parametrized test`(arg: String) {}
            }
            """.trimIndent()

        val tests = scan(source)
        assertEquals(1, tests.size)
        assertEquals("my parametrized test", tests[0].methodName)
        assertEquals("My parametrized test", tests[0].displayName)
    }

    @Test
    fun `skips when @AllureId present on @ParameterizedTest`() {
        val source =
            """
            package x
            class Foo {
                @ParameterizedTest
                @AllureId("123")
                @DisplayName("Already linked")
                fun `already linked`(arg: String) {}
            }
            """.trimIndent()

        val tests = scan(source)
        assertTrue(tests.isEmpty())
    }

    @Test
    fun `finds multi-line @DisplayName`() {
        val source =
            """
            package x
            class Foo {
                @Test
                @DisplayName(
                    "Multi line display name"
                )
                fun `my test`() {}
            }
            """.trimIndent()

        val tests = scan(source)
        assertEquals(1, tests.size)
        assertEquals("Multi line display name", tests[0].displayName)
    }

    @Test
    fun `detects @AllureId after multi-line @DisplayName`() {
        val source =
            """
            package x
            class Foo {
                @Test
                @DisplayName(
                    "Multi line name"
                )
                @AllureId("42")
                fun `my test`() {}
            }
            """.trimIndent()

        val tests = scan(source)
        assertTrue(tests.isEmpty(), "Should skip test that already has @AllureId, even after multi-line @DisplayName")
    }

    @Test
    fun `detects @AllureId before @Test`() {
        val source =
            """
            package x
            class Foo {
                @AllureId("99")
                @Test
                @DisplayName("Pre-linked")
                fun `pre linked`() {}
            }
            """.trimIndent()

        val tests = scan(source)
        assertTrue(tests.isEmpty())
    }

    @Test
    fun `handles multi-line @DisplayName on @ParameterizedTest`() {
        val source =
            """
            package x
            class Foo {
                @ParameterizedTest
                @DisplayName(
                    "Long parametrized name"
                )
                @MethodSource("args")
                fun `parametrized`(arg: String) {}
            }
            """.trimIndent()

        val tests = scan(source)
        assertEquals(1, tests.size)
        assertEquals("Long parametrized name", tests[0].displayName)
    }

    @Test
    fun `falls back to method name when no @DisplayName`() {
        val source =
            """
            package x
            class Foo {
                @Test
                fun `plain test`() {}
            }
            """.trimIndent()

        val tests = scan(source)
        assertEquals(1, tests.size)
        assertNull(tests[0].displayName)
        assertEquals("plain test", tests[0].testCaseName)
    }

    @Test
    fun `detects multiple unlinked tests in one file`() {
        val source =
            """
            package x
            class Foo {
                @Test
                @DisplayName("First")
                fun `first`() {}

                @ParameterizedTest
                @DisplayName(
                    "Second"
                )
                @MethodSource("args")
                fun `second`(arg: String) {}

                @Test
                @AllureId("7")
                @DisplayName("Third linked")
                fun `third`() {}
            }
            """.trimIndent()

        val tests = scan(source)
        assertEquals(2, tests.size)
        val names = tests.map { it.displayName }.toSet()
        assertEquals(setOf("First", "Second"), names)
    }
}
