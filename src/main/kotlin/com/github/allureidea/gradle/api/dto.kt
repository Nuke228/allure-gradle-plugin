package com.github.allureidea.gradle.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Long
)

@Serializable
data class TestCaseRequest(
    val projectId: Long,
    val name: String
)

@Serializable
data class TestCaseResponse(
    val id: Long,
    val name: String,
    val projectId: Long
)
