package com.clawstack.shellguard.totp.data.remote

object ApiClient {
    private var currentClient: ShellGuardTotpClient? = null
    private var currentAllowSelfSigned: Boolean = false

    @Volatile
    var allowSelfSignedCerts: Boolean = false
        private set

    @Volatile
    var pinnedFingerprint: String? = null
        private set

    var onPinCertificateCallback: ((String) -> Unit)? = null

    var authToken: String? = null
        private set
    var currentServerUrl: String? = null
        private set

    var onUnauthorizedCallback: (() -> Unit)? = null

    fun updateAuthToken(token: String?) {
        authToken = token
    }

    @Synchronized
    fun configureTls(
        allowSelfSigned: Boolean,
        pinnedSha256: String? = null,
        onPinCertificate: ((String) -> Unit)? = null
    ) {
        this.allowSelfSignedCerts = allowSelfSigned
        this.pinnedFingerprint = pinnedSha256
        this.onPinCertificateCallback = onPinCertificate
    }

    fun getCurrentClient(): ShellGuardTotpClient {
        return currentClient ?: throw IllegalStateException("API Client not initialized with base URL.")
    }

    @Synchronized
    fun getClient(
        baseUrl: String,
        allowSelfSigned: Boolean = this.allowSelfSignedCerts
    ): ShellGuardTotpClient {
        if (currentServerUrl == baseUrl && currentAllowSelfSigned == allowSelfSigned && currentClient != null) {
            return currentClient!!
        }

        currentClient?.close()
        currentServerUrl = baseUrl
        currentAllowSelfSigned = allowSelfSigned
        currentClient = ShellGuardTotpClient(
            baseUrl = baseUrl,
            allowSelfSignedCerts = allowSelfSigned,
            getPinnedFingerprint = { pinnedFingerprint },
            onPinCertificate = { fingerprint ->
                pinnedFingerprint = fingerprint
                onPinCertificateCallback?.invoke(fingerprint)
            },
            onUnauthorized = { onUnauthorizedCallback?.invoke() }
        )
        return currentClient!!
    }

    @Synchronized
    fun reset() {
        currentClient?.close()
        currentClient = null
        authToken = null
        currentServerUrl = null
        currentAllowSelfSigned = false
        allowSelfSignedCerts = false
        pinnedFingerprint = null
        onPinCertificateCallback = null
    }
}
