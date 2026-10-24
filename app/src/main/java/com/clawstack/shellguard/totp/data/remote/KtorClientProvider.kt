package com.clawstack.shellguard.totp.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Security helper to configure OkHttp and Ktor for cleartext HTTP traffic,
 * local network IPs (192.168.x.x, 10.x.x.x, 127.0.0.1, localhost), Tailscale VPN IPs (100.x.y.z),
 * and self-signed certificate environments.
 */
object NetworkSecurityHelper {

    fun configureOkHttpClient(builder: OkHttpClient.Builder) {
        builder.connectionSpecs(
            listOf(
                ConnectionSpec.MODERN_TLS,
                ConnectionSpec.COMPATIBLE_TLS,
                ConnectionSpec.CLEARTEXT
            )
        )
        builder.connectTimeout(15, TimeUnit.SECONDS)
        builder.readTimeout(20, TimeUnit.SECONDS)
        builder.writeTimeout(20, TimeUnit.SECONDS)
        builder.retryOnConnectionFailure(true)
    }
}

object KtorClientProvider {

    private val jsonConfig = Json {
        prettyPrint = false
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun createClient(
        baseUrl: String? = null,
        onUnauthorized: (() -> Unit)? = null
    ): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
                    NetworkSecurityHelper.configureOkHttpClient(this)
                }
            }

            install(ContentNegotiation) {
                json(jsonConfig)
            }

            if (baseUrl != null) {
                defaultRequest {
                    url {
                        val base = baseUrl.removeSuffix("/")
                        takeFrom("$base/")
                    }
                    header("X-Client-Version", "1.0.0")
                    header("Accept-Version", "1.0.0")
                    header("Accept", "application/json")
                }
            }

            HttpResponseValidator {
                validateResponse { response ->
                    if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                        onUnauthorized?.invoke()
                    }
                }
            }
        }
    }

    val client: HttpClient by lazy {
        createClient()
    }
}
