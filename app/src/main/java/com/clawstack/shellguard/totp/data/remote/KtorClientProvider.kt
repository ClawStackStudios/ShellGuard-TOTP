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
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Security helper to configure OkHttp and Ktor for cleartext HTTP traffic,
 * local network IPs (192.168.x.x, 10.x.x.x, 127.0.0.1, localhost), Tailscale VPN IPs (100.x.y.z),
 * and explicit opt-in self-signed HTTPS certificates with TOFU SHA-256 pinning.
 */
object NetworkSecurityHelper {

    fun computeSha256Fingerprint(cert: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
        return digest.joinToString(":") { "%02X".format(it) }
    }

    internal fun createTofuTrustManager(
        getPinnedFingerprint: (() -> String?)? = null,
        onPinCertificate: ((String) -> Unit)? = null
    ): X509TrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            val leaf = chain?.firstOrNull()
                ?: throw CertificateException("Empty server certificate chain")
            val fingerprint = computeSha256Fingerprint(leaf)
            val pinned = getPinnedFingerprint?.invoke()
            if (pinned.isNullOrBlank()) {
                onPinCertificate?.invoke(fingerprint)
            } else if (!fingerprint.equals(pinned, ignoreCase = true)) {
                throw CertificateException(
                    "Server certificate fingerprint mismatch (expected $pinned, got $fingerprint). " +
                        "Re-connect in Auth Gateway if the server certificate was rotated."
                )
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    fun configureOkHttpClient(
        builder: OkHttpClient.Builder,
        baseUrl: String? = null,
        allowSelfSignedCerts: Boolean = false,
        getPinnedFingerprint: (() -> String?)? = null,
        onPinCertificate: ((String) -> Unit)? = null
    ) {
        builder.connectionSpecs(
            listOf(
                ConnectionSpec.MODERN_TLS,
                ConnectionSpec.COMPATIBLE_TLS,
                ConnectionSpec.CLEARTEXT
            )
        )
        if (allowSelfSignedCerts) {
            val trustManager = createTofuTrustManager(getPinnedFingerprint, onPinCertificate)
            val sslContext = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
            }
            builder.sslSocketFactory(sslContext.socketFactory, trustManager)

            val expectedHost = baseUrl?.let {
                runCatching { URI(it).host }.getOrNull()
            }
            builder.hostnameVerifier { hostname, _ ->
                !expectedHost.isNullOrBlank() && hostname.equals(expectedHost, ignoreCase = true)
            }
        }
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
        allowSelfSignedCerts: Boolean = false,
        getPinnedFingerprint: (() -> String?)? = null,
        onPinCertificate: ((String) -> Unit)? = null,
        onUnauthorized: (() -> Unit)? = null
    ): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
                    NetworkSecurityHelper.configureOkHttpClient(
                        builder = this,
                        baseUrl = baseUrl,
                        allowSelfSignedCerts = allowSelfSignedCerts,
                        getPinnedFingerprint = getPinnedFingerprint,
                        onPinCertificate = onPinCertificate
                    )
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
