package com.clawstack.shellguard.totp.data.remote

object ApiClient {
    private var currentClient: ShellGuardTotpClient? = null
    var authToken: String? = null
        private set
    var currentServerUrl: String? = null
        private set

    var onUnauthorizedCallback: (() -> Unit)? = null

    fun updateAuthToken(token: String?) {
        authToken = token
    }

    fun getCurrentClient(): ShellGuardTotpClient {
        return currentClient ?: throw IllegalStateException("API Client not initialized with base URL.")
    }

    @Synchronized
    fun getClient(baseUrl: String): ShellGuardTotpClient {
        if (currentServerUrl == baseUrl && currentClient != null) {
            return currentClient!!
        }

        currentClient?.close()
        currentServerUrl = baseUrl
        currentClient = ShellGuardTotpClient(
            baseUrl = baseUrl,
            onUnauthorized = { onUnauthorizedCallback?.invoke() }
        )
        return currentClient!!
    }

    fun reset() {
        currentClient?.close()
        currentClient = null
        authToken = null
        currentServerUrl = null
    }
}
