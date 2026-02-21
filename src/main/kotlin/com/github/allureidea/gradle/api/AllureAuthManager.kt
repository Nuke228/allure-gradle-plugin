package com.github.allureidea.gradle.api

import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URI

class AllureAuthManager(private val baseUrl: String) {

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cachedJwt: String? = null

    @Volatile
    private var expiresAtMillis: Long = 0

    fun getValidJwt(apiToken: String): String {
        val now = System.currentTimeMillis()
        val jwt = cachedJwt
        if (jwt != null && now < expiresAtMillis - 60_000) {
            return jwt
        }
        return exchangeToken(apiToken)
    }

    private fun exchangeToken(apiToken: String): String {
        val conn = URI("$baseUrl/api/uaa/oauth/token").toURL()
            .openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 30_000
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.setRequestProperty("Accept", "application/json")

        try {
            conn.outputStream.use { os ->
                os.write("grant_type=apitoken&scope=openid&token=$apiToken".toByteArray(Charsets.UTF_8))
            }
            val code = conn.responseCode
            if (code != 200) {
                val error = runCatching { conn.errorStream?.bufferedReader()?.readText() }
                    .getOrNull() ?: "HTTP $code"
                throw RuntimeException("Auth failed (HTTP $code): $error")
            }

            val responseBody = conn.inputStream.bufferedReader().readText()
            val authResponse = json.decodeFromString<AuthResponse>(responseBody)

            cachedJwt = authResponse.accessToken
            expiresAtMillis = System.currentTimeMillis() + authResponse.expiresIn * 1000

            return authResponse.accessToken
        } finally {
            conn.disconnect()
        }
    }
}
