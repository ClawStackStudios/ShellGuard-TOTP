package com.clawstack.shellguard.totp.data.remote

import android.util.Log
import com.clawstack.shellguard.totp.data.remote.models.CreateVaultItemRequest
import com.clawstack.shellguard.totp.data.remote.models.PearlDto
import com.clawstack.shellguard.totp.data.remote.models.ShellResponse
import com.clawstack.shellguard.totp.data.remote.models.TokenRequest
import com.clawstack.shellguard.totp.data.remote.models.TokenResponse
import com.clawstack.shellguard.totp.data.remote.models.VaultItemResponse
import com.clawstack.shellguard.totp.data.remote.models.VaultResponse
import com.clawstack.shellguard.totp.data.remote.models.SessionData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

suspend fun handleNetworkDiagnostics(response: HttpResponse) {
    if (response.status.value == 400) {
        val responseBody = response.bodyAsText()
        try {
            val errorDetails = Json { ignoreUnknownKeys = true }.decodeFromString<ShellResponse<Unit>>(responseBody)
            Log.e("ShellGuardClient", "🚨 [VALIDATION ERROR 400]: ${errorDetails.error}")
            errorDetails.details?.forEach { issue ->
                Log.e("ShellGuardClient", "  → Field [${issue.path}]: ${issue.message}")
            }
        } catch (e: Exception) {
            Log.e("ShellGuardClient", "🚨 [HTTP 400 RAW]: $responseBody", e)
        }
    }
}

class ShellGuardTotpClient(
    private val baseUrl: String, // e.g. http://192.168.1.50:6464, http://unraid.local:6464, or https://vault.example.com
    private val onUnauthorized: (() -> Unit)? = null
) {
    private val client: HttpClient = KtorClientProvider.createClient(
        baseUrl = baseUrl,
        onUnauthorized = onUnauthorized
    )

    /**
     * 1. Public Health Check
     */
    suspend fun getHealth(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.get("api/health")
            response.status == HttpStatusCode.OK
        }
    }

    /**
     * 2. Authentication Handshake (POST /api/auth/token)
     */
    suspend fun authenticate(keyHash: String): Result<SessionData> = withContext(Dispatchers.IO) {
        runCatching {
            val requestBody = TokenRequest(type = "human", keyHash = keyHash)
            val response: HttpResponse = client.post("api/auth/token") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            handleNetworkDiagnostics(response)

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                val res = response.body<TokenResponse>()
                if (res.success && res.data != null) {
                    res.data
                } else {
                    throw Exception(res.error ?: "Authentication failed")
                }
            } else {
                throw Exception("Auth request failed: ${response.status.value}")
            }
        }
    }

    /**
     * 3. Fetch Vault Items (GET /api/vault)
     */
    suspend fun fetchVault(sessionToken: String): Result<List<PearlDto>> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.get("api/vault") {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
            }
            handleNetworkDiagnostics(response)

            if (response.status == HttpStatusCode.OK) {
                val res = response.body<VaultResponse>()
                res.data
            } else {
                throw Exception("Fetch vault failed with status: ${response.status.value}")
            }
        }
    }

    /**
     * 4. Create Vault Item (POST /api/vault) - Upstream Push Endpoint
     */
    suspend fun createVaultItem(sessionToken: String, request: CreateVaultItemRequest): Result<PearlDto> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.post("api/vault") {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            handleNetworkDiagnostics(response)

            if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                val res = response.body<VaultItemResponse>()
                if (res.success && res.data != null) {
                    res.data
                } else {
                    throw Exception(res.error ?: "Create vault item failed")
                }
            } else {
                throw Exception("Create vault item failed with status: ${response.status.value}")
            }
        }
    }

    /**
     * 5. Update Vault Item (PUT /api/vault/:id)
     */
    suspend fun updateVaultItem(sessionToken: String, id: String, request: CreateVaultItemRequest): Result<PearlDto> = withContext(Dispatchers.IO) {
        runCatching {
            val response: HttpResponse = client.put("api/vault/$id") {
                header(HttpHeaders.Authorization, "Bearer $sessionToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            handleNetworkDiagnostics(response)

            if (response.status == HttpStatusCode.OK) {
                val res = response.body<VaultItemResponse>()
                if (res.success && res.data != null) {
                    res.data
                } else {
                    throw Exception(res.error ?: "Update vault item failed")
                }
            } else {
                throw Exception("Update vault item failed with status: ${response.status.value}")
            }
        }
    }

    fun close() {
        client.close()
    }
}
