package com.clawstack.shellguard.totp.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class ShellResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val code: String? = null,
    val details: List<ValidationIssue>? = null,
    val suggestion: String? = null
)

@Serializable
data class ValidationIssue(
    val path: String? = null,
    val message: String
)

@Serializable
data class TokenRequest(
    val type: String = "human",
    val keyHash: String
)

@Serializable
data class UserProfileDto(
    val uuid: String,
    val username: String,
    val displayName: String? = null
)

@Serializable
data class SessionData(
    val token: String,
    val type: String,
    val createdAt: String,
    val expiresAt: String,
    val user: UserProfileDto
)

@Serializable
data class TokenResponse(
    val success: Boolean,
    val data: SessionData? = null,
    val error: String? = null
)

@Serializable
data class PearlDto(
    val id: String,
    val owner_uuid: String,
    val title: String,
    val username: String? = null,
    val url: String? = null,
    val category: String? = null,
    val notes: String? = null,
    val secret: String = "", // ShellCryption envelope for password
    val totp_secret: String? = null, // ShellCryption envelope for TOTP seed
    val attachments: String = "[]",
    val custom_fields: String? = null,
    val created_at: String = "",
    val updated_at: String? = null
)

@Serializable
data class CreateVaultItemRequest(
    val title: String,
    val username: String? = null,
    val url: String? = null,
    val category: String? = null,
    val notes: String? = null,
    val secret: String = "", // ShellCryption envelope for password
    val totp_secret: String? = null, // ShellCryption envelope for TOTP seed
    val type: String = "password",
    val custom_fields: String? = null
)

@Serializable
data class VaultItemResponse(
    val success: Boolean,
    val data: PearlDto? = null,
    val error: String? = null
)

@Serializable
data class VaultResponse(
    val success: Boolean,
    val data: List<PearlDto> = emptyList(),
    val error: String? = null
)
