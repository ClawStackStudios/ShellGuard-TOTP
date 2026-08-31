package com.clawstack.shellguard.totp.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clawstack.shellguard.totp.ShellGuardTotpApp
import com.clawstack.shellguard.totp.data.local.entities.SyncMetadataEntity
import com.clawstack.shellguard.totp.data.local.entities.TotpItemEntity
import com.clawstack.shellguard.totp.engine.TotpEngine
import com.clawstack.shellguard.totp.engine.TotpTicker
import com.clawstack.shellguard.totp.engine.TotpTickerState
import com.clawstack.shellguard.totp.engine.TotpUriParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class ClipboardFeedbackState(
    val isVisible: Boolean = false,
    val message: String = "",
    val copiedCode: String = ""
)

private data class FilterState(
    val owner: String,
    val query: String,
    val category: String?,
    val isConnected: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
class TotpViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ShellGuardTotpApp
    private val database = app.database
    private val totpItemDao = database.totpItemDao()
    private val syncMetadataDao = database.syncMetadataDao()
    private val authRepository = app.authRepository

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)
    val clipboardFeedback = MutableStateFlow(ClipboardFeedbackState())

    private var clipboardClearJob: Job? = null
    private var toastDismissJob: Job? = null

    val isServerConnected: StateFlow<Boolean> = authRepository.currentSession
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentOwnerUuid: StateFlow<String> = authRepository.currentSession
        .map { it?.userUuid ?: "local" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "local")

    init {
        // Automatically reset 'Synced' filter selection if user disconnects from server
        viewModelScope.launch {
            isServerConnected.collect { isConnected ->
                if (!isConnected && (selectedCategory.value == "☁️ Synced" || selectedCategory.value == "📱 Local Only")) {
                    selectedCategory.value = null
                }
            }
        }
    }

    // 1-second interval live epoch ticker
    val tickerState: StateFlow<TotpTickerState> = TotpTicker.observeTicker()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            TotpTickerState(
                System.currentTimeMillis(),
                TotpEngine.getRemainingSeconds(),
                TotpEngine.getProgressRatio()
            )
        )

    // Reactive categories list for current owner
    val categories: StateFlow<List<String>> = currentOwnerUuid.flatMapLatest { owner ->
        totpItemDao.observeDistinctCategories(owner)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reactive sync metadata state
    val syncMetadata: StateFlow<SyncMetadataEntity?> = syncMetadataDao.observeMetadata()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Live count of offline-only (local) codes
    val offlineCodesCount: StateFlow<Int> = currentOwnerUuid.flatMapLatest { owner ->
        totpItemDao.observeAllTotpItems(owner).map { list -> list.count { it.isLocalOnly || it.ownerUuid == "local" } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Live count of remote synced codes
    val syncedCodesCount: StateFlow<Int> = currentOwnerUuid.flatMapLatest { owner ->
        totpItemDao.observeAllTotpItems(owner).map { list -> list.count { !it.isLocalOnly && it.ownerUuid != "local" } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Filtered items flow combining search query & category selection & ownerUuid
    val items: StateFlow<List<TotpItemEntity>> = combine(
        currentOwnerUuid,
        searchQuery,
        selectedCategory,
        isServerConnected
    ) { owner, query, category, isConnected ->
        FilterState(owner, query, category, isConnected)
    }.flatMapLatest { filterState ->
        val baseFlow = if (filterState.query.isNotBlank()) {
            totpItemDao.searchTotpItems(filterState.owner, filterState.query.trim())
        } else {
            totpItemDao.observeAllTotpItems(filterState.owner)
        }

        baseFlow.map { list ->
            when (filterState.category) {
                null, "All Accounts", "All Tokens" -> list
                "☁️ Synced" -> if (filterState.isConnected) list.filter { !it.isLocalOnly && it.ownerUuid != "local" } else list
                "📱 Local Only" -> list.filter { it.isLocalOnly || it.ownerUuid == "local" }
                else -> list.filter { it.category.equals(filterState.category, ignoreCase = true) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onCategorySelected(category: String?) {
        selectedCategory.value = category
    }

    fun filterOfflineCodesOnly() {
        if (isServerConnected.value) {
            selectedCategory.value = "📱 Local Only"
        } else {
            selectedCategory.value = null
        }
    }

    /**
     * Copies code to Android clipboard, presents auto-clearing feedback, and schedules clipboard scrub.
     */
    fun copyToClipboard(title: String, code: String) {
        val context = getApplication<Application>().applicationContext
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return

        val clip = ClipData.newPlainText("ShellGuard 2FA Code", code)
        clipboard.setPrimaryClip(clip)

        val isAutoClear = authRepository.isAutoClearClipboard.value
        val formattedCode = if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}" else code
        clipboardFeedback.value = ClipboardFeedbackState(
            isVisible = true,
            message = if (isAutoClear) "Copied $formattedCode (auto-clears in 30s)" else "Copied $formattedCode",
            copiedCode = code
        )

        // Dismiss visual toast after 4s
        toastDismissJob?.cancel()
        toastDismissJob = viewModelScope.launch {
            delay(4000)
            clipboardFeedback.value = clipboardFeedback.value.copy(isVisible = false)
        }

        // Scrub actual clipboard after 30s for security if enabled
        clipboardClearJob?.cancel()
        if (isAutoClear) {
            clipboardClearJob = viewModelScope.launch {
                delay(30_000)
                try {
                    // If clipboard still holds this specific code, clear it
                    if (clipboard.primaryClip?.getItemAt(0)?.text?.toString() == code) {
                        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                } catch (ignored: Exception) {}
            }
        }
    }

    fun addManualSecret(
        title: String,
        username: String?,
        category: String?,
        secret: String,
        algorithm: String = "SHA1",
        digits: Int = 6,
        period: Int = 30
    ) {
        viewModelScope.launch {
            val session = authRepository.currentSession.value
            val isConnected = session != null
            val owner = session?.userUuid ?: "local"
            val cleanSecret = secret.replace(" ", "").replace("-", "").uppercase()
            val newItem = TotpItemEntity(
                id = UUID.randomUUID().toString(),
                ownerUuid = owner,
                title = title.trim(),
                username = username?.trim()?.ifBlank { null },
                category = category?.trim()?.ifBlank { null },
                secret = cleanSecret,
                algorithm = algorithm,
                digits = digits,
                period = period,
                isLocalOnly = !isConnected,
                syncState = if (isConnected) "PENDING_SYNC" else "SYNCED"
            )
            totpItemDao.upsertItem(newItem)
        }
    }

    fun updateItem(item: TotpItemEntity) {
        viewModelScope.launch {
            totpItemDao.updateItem(item.copy(localUpdatedAt = System.currentTimeMillis()))
        }
    }

    fun updateItemDetails(id: String, title: String, username: String?, category: String?) {
        viewModelScope.launch {
            val existing = totpItemDao.getItemById(id) ?: return@launch
            val updated = existing.copy(
                title = title.trim(),
                username = username?.trim()?.ifBlank { null },
                category = category?.trim()?.ifBlank { null },
                localUpdatedAt = System.currentTimeMillis()
            )
            totpItemDao.updateItem(updated)
        }
    }

    fun importScannedUri(rawUri: String): Boolean {
        val parsed = TotpUriParser.parse(rawUri) ?: return false
        viewModelScope.launch {
            val session = authRepository.currentSession.value
            val isConnected = session != null
            val owner = session?.userUuid ?: "local"
            val newItem = TotpItemEntity(
                id = UUID.randomUUID().toString(),
                ownerUuid = owner,
                title = parsed.title,
                username = parsed.username,
                category = parsed.issuer,
                secret = parsed.secret,
                algorithm = parsed.algorithm,
                digits = parsed.digits,
                period = parsed.period,
                isLocalOnly = !isConnected,
                syncState = if (isConnected) "PENDING_SYNC" else "SYNCED"
            )
            totpItemDao.upsertItem(newItem)
        }
        return true
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            totpItemDao.deleteById(id)
        }
    }

    fun deleteItem(item: TotpItemEntity) {
        viewModelScope.launch {
            totpItemDao.deleteById(item.id)
        }
    }
}
