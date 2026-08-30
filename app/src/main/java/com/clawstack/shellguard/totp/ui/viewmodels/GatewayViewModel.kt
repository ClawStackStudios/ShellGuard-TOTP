package com.clawstack.shellguard.totp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clawstack.shellguard.totp.data.repository.AuthRepository
import com.clawstack.shellguard.totp.data.repository.TotpRepository
import com.clawstack.shellguard.totp.data.repository.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class GatewayUiState {
    object Idle : GatewayUiState()
    object Loading : GatewayUiState()
    data class Success(val session: UserSession) : GatewayUiState()
    data class Error(val message: String) : GatewayUiState()
}

class GatewayViewModel(
    private val authRepository: AuthRepository,
    private val totpRepository: TotpRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GatewayUiState>(GatewayUiState.Idle)
    val uiState: StateFlow<GatewayUiState> = _uiState.asStateFlow()

    val savedServerUrl = MutableStateFlow(authRepository.getSavedServerUrl())
    val savedKey = MutableStateFlow(authRepository.getSavedKey())

    fun login(serverUrl: String, rawHuKey: String) {
        viewModelScope.launch {
            _uiState.value = GatewayUiState.Loading
            authRepository.login(serverUrl, rawHuKey)
                .onSuccess { session ->
                    // Trigger initial delta sync
                    totpRepository.syncRemoteVault(session.serverUrl, session.rawHuKey, session.userUuid)
                    _uiState.value = GatewayUiState.Success(session)
                }
                .onFailure { error ->
                    _uiState.value = GatewayUiState.Error(error.message ?: "Authentication failed. Check your identity key and server URL.")
                }
        }
    }

    fun clearSavedKey() {
        authRepository.clearSavedKey()
        savedKey.value = null
    }

    fun resetState() {
        _uiState.value = GatewayUiState.Idle
    }
}
