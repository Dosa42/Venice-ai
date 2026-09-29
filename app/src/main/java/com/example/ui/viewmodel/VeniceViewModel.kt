package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.adaptive.AdaptiveFact
import com.example.data.adaptive.DynamicAdaptiveEngine
import com.example.data.local.NetHunterTerminal
import com.example.data.local.TerminalCommand
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VeniceNavTab { WORKSPACE, TERMINAL, VAULT }

data class VeniceUiState(
    val currentTab: VeniceNavTab = VeniceNavTab.WORKSPACE,
    val terminalInput: String = "",
    val terminalEnvironment: String = "nethunter",
    val terminalCommands: List<TerminalCommand> = emptyList(),
    val adaptiveFacts: List<AdaptiveFact> = emptyList(),
    val selectedFileName: String? = null,
    val selectedFileContent: String? = null,
    val sharedText: String? = null,
    val error: String? = null
)

/** Local workspace only. No account, login, token, cloud sync, or model backend. */
class VeniceViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VeniceUiState())
    val uiState: StateFlow<VeniceUiState> = _uiState.asStateFlow()
    private var terminal: NetHunterTerminal? = null

    fun initialize(context: Context) {
        if (terminal != null) return
        val appContext = context.applicationContext
        val executor = NetHunterTerminal(viewModelScope, File(appContext.filesDir, "terminal"))
        terminal = executor
        _uiState.update { it.copy(adaptiveFacts = DynamicAdaptiveEngine.loadFacts(appContext)) }
        viewModelScope.launch {
            executor.commands.collect { commands ->
                _uiState.update { it.copy(terminalCommands = commands) }
            }
        }
    }

    fun selectTab(tab: VeniceNavTab) { _uiState.update { it.copy(currentTab = tab, error = null) } }
    fun setTerminalInput(value: String) { _uiState.update { it.copy(terminalInput = value) } }
    fun setTerminalEnvironment(value: String) {
        require(value == "nethunter" || value == "android")
        _uiState.update { it.copy(terminalEnvironment = value) }
    }

    fun runTerminalCommand() {
        val executor = terminal ?: return
        val state = _uiState.value
        val command = state.terminalInput.trim()
        if (command.isEmpty()) return
        _uiState.update { it.copy(terminalInput = "", error = null, currentTab = VeniceNavTab.TERMINAL) }
        viewModelScope.launch {
            try {
                executor.start(
                    command = command,
                    workdir = if (state.terminalEnvironment == "android") "/" else "/root",
                    environment = state.terminalEnvironment,
                    chroot = "/data/local/nhsystem/kali-arm64"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Terminal command could not start") }
            }
        }
    }

    fun stopTerminalCommand(id: String) {
        try { terminal?.stop(id) }
        catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
    }
    fun stopAllTerminalCommands() { terminal?.stopAll() }

    fun showPickedFile(name: String, content: String) {
        _uiState.update { it.copy(selectedFileName = name, selectedFileContent = content, error = null,
            currentTab = VeniceNavTab.WORKSPACE) }
    }

    fun showSharedText(text: String) {
        if (text.isNotBlank()) _uiState.update { it.copy(sharedText = text, currentTab = VeniceNavTab.WORKSPACE) }
    }
    fun showError(message: String) { _uiState.update { it.copy(error = message) } }

    override fun onCleared() {
        terminal?.stopAll()
        super.onCleared()
    }
}
