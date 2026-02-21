package com.github.allureidea.gradle.api

import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI

class AllureApiClient(private val baseUrl: String) {

    private val json = Json { ignoreUnknownKeys = true }

    fun createTestCase(jwt: String, request: TestCaseRequest): TestCaseResponse {
        val requestBody = json.encodeToString(TestCaseRequest.serializer(), request)

        val conn = URI("$baseUrl/api/rs/testcase").toURL()
            .openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Authorization", "Bearer $jwt")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")

        try {
            conn.outputStream.use { os ->
                os.write(requestBody.toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val error = runCatching { conn.errorStream?.bufferedReader()?.readText() }
                    .getOrNull() ?: "HTTP $code"
                throw RuntimeException("Failed to create test case (HTTP $code): $error")
            }

            val responseBody = conn.inputStream.bufferedReader().readText()
            return json.decodeFromString<TestCaseResponse>(responseBody)
        } finally {
            conn.disconnect()
        }
    }
}
