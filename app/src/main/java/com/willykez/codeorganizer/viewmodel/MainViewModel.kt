package com.willykez.codeorganizer.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.willykez.codeorganizer.data.AiClientFactory
import com.willykez.codeorganizer.data.AiResult
import com.willykez.codeorganizer.data.FileOrganizer
import com.willykez.codeorganizer.data.SettingsStore
import com.willykez.codeorganizer.model.AiProvider
import com.willykez.codeorganizer.model.LogLine
import com.willykez.codeorganizer.model.ParsedFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Rough guard, not a hard limit — most providers' context windows start straining well
 * before this many characters, so we warn instead of silently sending a paste that's
 * likely to get truncated by the model. */
private const val LARGE_PASTE_WARNING_CHARS = 60_000

data class MainUiState(
    val provider: AiProvider = AiProvider.ANTHROPIC,
    val apiKey: String = "",
    val model: String = AiProvider.ANTHROPIC.defaultModel,
    val baseUrl: String = AiProvider.ANTHROPIC.defaultBaseUrl,
    val treeUriString: String = "",
    val pastedSource: String = "",
    val isWorking: Boolean = false,
    val log: List<LogLine> = emptyList(),
    /** Set together: what the AI parsed, awaiting the user's confirm/cancel before anything is written. */
    val pendingProjectName: String? = null,
    val pendingFiles: List<ParsedFile>? = null
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsStore(app)
    private val fileOrganizer = FileOrganizer(app)

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { loadAll() }
    }

    /** Re-reads the currently selected provider's settings plus the shared folder URI. */
    fun refreshSettings() {
        viewModelScope.launch { loadAll() }
    }

    private suspend fun loadAll() {
        val provider = settings.getSelectedProvider()
        val ps = settings.getProviderSettings(provider)
        val tree = settings.treeUri.first()
        _state.value = _state.value.copy(
            provider = provider,
            apiKey = ps.apiKey,
            model = ps.model,
            baseUrl = ps.baseUrl,
            treeUriString = tree
        )
    }

    /** Called when the user picks a different provider in Settings — loads that provider's own saved fields. */
    fun selectProvider(provider: AiProvider) {
        viewModelScope.launch {
            settings.saveSelectedProvider(provider)
            val ps = settings.getProviderSettings(provider)
            _state.value = _state.value.copy(
                provider = provider,
                apiKey = ps.apiKey,
                model = ps.model,
                baseUrl = ps.baseUrl
            )
        }
    }

    fun onSourceChanged(text: String) {
        _state.value = _state.value.copy(pastedSource = text)
    }

    fun clearSource() {
        _state.value = _state.value.copy(pastedSource = "")
    }

    fun clearLog() {
        _state.value = _state.value.copy(log = emptyList())
    }

    fun saveApiKey(key: String) = viewModelScope.launch {
        settings.saveApiKey(_state.value.provider, key)
        _state.value = _state.value.copy(apiKey = key)
    }

    fun saveModel(model: String) = viewModelScope.launch {
        settings.saveModel(_state.value.provider, model)
        _state.value = _state.value.copy(model = model)
    }

    fun saveBaseUrl(baseUrl: String) = viewModelScope.launch {
        settings.saveBaseUrl(_state.value.provider, baseUrl)
        _state.value = _state.value.copy(baseUrl = baseUrl)
    }

    fun saveTreeUri(uri: Uri) = viewModelScope.launch {
        settings.saveTreeUri(uri.toString())
        _state.value = _state.value.copy(treeUriString = uri.toString())
    }

    /** Step 1: ask the AI to parse the paste. Stops at a preview — nothing is written yet. */
    fun organize() {
        val current = _state.value
        if (current.isWorking) return

        val startLog = mutableListOf(LogLine("Sending to ${current.provider.displayName} (${current.model})…"))
        if (current.pastedSource.length > LARGE_PASTE_WARNING_CHARS) {
            startLog.add(
                LogLine(
                    "That's a large paste (${current.pastedSource.length} chars) — some models " +
                        "may truncate it. If the result looks incomplete, try splitting it up.",
                    isError = false
                )
            )
        }

        _state.value = current.copy(
            isWorking = true,
            pendingProjectName = null,
            pendingFiles = null,
            log = startLog
        )

        viewModelScope.launch {
            try {
                val client = AiClientFactory.create(current.provider)
                when (val result = client.organize(current.apiKey, current.baseUrl, current.model, current.pastedSource)) {
                    is AiResult.Failure -> {
                        _state.value = _state.value.copy(
                            isWorking = false,
                            log = _state.value.log + LogLine(result.message, isError = true)
                        )
                    }
                    is AiResult.Success -> {
                        _state.value = _state.value.copy(
                            isWorking = false,
                            pendingProjectName = result.project.projectName,
                            pendingFiles = result.project.files,
                            log = _state.value.log + LogLine(
                                "Parsed '${result.project.projectName}' — ${result.project.files.size} file(s). " +
                                    "Review below, then Confirm to write."
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isWorking = false,
                    log = _state.value.log + LogLine("Unexpected error: ${e.message ?: "Unknown error"}", isError = true)
                )
            }
        }
    }

    /** Step 2: the user reviewed the pending file list and confirmed — now actually write them. */
    fun confirmWrite() {
        val current = _state.value
        val projectName = current.pendingProjectName ?: return
        val files = current.pendingFiles ?: return
        if (current.isWorking) return

        val treeUri = current.treeUriString
        if (treeUri.isBlank()) {
            _state.value = current.copy(
                log = current.log + LogLine("Pick a destination folder first.", isError = true)
            )
            return
        }

        _state.value = current.copy(
            isWorking = true,
            pendingProjectName = null,
            pendingFiles = null,
            log = current.log + LogLine("Writing…")
        )

        viewModelScope.launch {
            try {
                val writeLog = fileOrganizer.writeProject(Uri.parse(treeUri), projectName, files)
                _state.value = _state.value.copy(
                    isWorking = false,
                    log = _state.value.log + writeLog + LogLine(
                        "Done: ${writeLog.count { !it.isError }} written, " +
                            "${writeLog.count { it.isError }} failed."
                    )
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isWorking = false,
                    log = _state.value.log + LogLine("Unexpected error while writing: ${e.message ?: "Unknown error"}", isError = true)
                )
            }
        }
    }

    /** The user reviewed the pending file list and backed out — discard it, nothing was written. */
    fun cancelPending() {
        val current = _state.value
        _state.value = current.copy(
            pendingProjectName = null,
            pendingFiles = null,
            log = current.log + LogLine("Cancelled — nothing was written.")
        )
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MainViewModel(app) as T
            }
    }
}
