package com.example.ui.viewmodel

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.adaptive.AdaptiveFact
import com.example.data.adaptive.DynamicAdaptiveEngine
import com.example.data.auth.ChatGPTAuthManager
import com.example.data.auth.ChatGPTSession
import com.example.data.config.VaultAuthConfig
import com.example.data.config.VaultAuthConfigManager
import com.example.data.filesystem.VaultChangeKind
import com.example.data.filesystem.VaultFileObserverManager
import com.example.data.filesystem.VaultFileSystemManager
import com.example.data.filesystem.VaultNoteFile
import com.example.data.local.NetHunterTerminal
import com.example.data.local.TerminalCommand
import com.example.data.local.VaultIndex
import com.example.data.local.VaultIndexDatabase
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class VeniceNavTab { WORKSPACE, TERMINAL, VAULT }

data class VeniceUiState(
    val currentTab: VeniceNavTab = VeniceNavTab.WORKSPACE,
    val terminalInput: String = "",
    val terminalEnvironment: String = "nethunter",
    val terminalCommands: List<TerminalCommand> = emptyList(),
    val adaptiveFacts: List<AdaptiveFact> = emptyList(),
    val vaultPath: String = "",
    val vaultError: String? = null,
    val vaultNotes: List<VaultNoteFile> = emptyList(),
    val indexedNotes: Int = 0,
    val indexError: String? = null,
    val sharedSession: ChatGPTSession? = null,
    val sharedConfig: VaultAuthConfig? = null,
    val selectedNotePath: String? = null,
    val selectedNoteText: String = "",
    val selectedNoteOriginalText: String = "",
    val noteDirty: Boolean = false,
    val newNoteTitle: String = "",
    val selectedFileName: String? = null,
    val selectedFileContent: String? = null,
    val sharedText: String? = null,
    val error: String? = null
)

/** Shared vault reader and local tools; provider login and network backends remain removed. */
class VeniceViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VeniceUiState())
    val uiState: StateFlow<VeniceUiState> = _uiState.asStateFlow()
    private var terminal: NetHunterTerminal? = null
    private var appContext: Context? = null
    private var vault: VaultFileSystemManager? = null
    private var auth: ChatGPTAuthManager? = null
    private var configManager: VaultAuthConfigManager? = null
    private var indexDatabase: VaultIndexDatabase? = null
    private var vaultIndex: VaultIndex? = null
    private var observer: VaultFileObserverManager? = null
    private var observerRoot: String? = null
    private var observerJob: Job? = null
    private val vaultMutex = Mutex()

    fun initialize(context: Context) {
        if (terminal != null) return
        val appContext = context.applicationContext
        this.appContext = appContext
        vault = VaultFileSystemManager()
        auth = ChatGPTAuthManager()
        configManager = VaultAuthConfigManager()
        indexDatabase = VaultIndexDatabase.open(appContext)
        vaultIndex = VaultIndex(indexDatabase!!)
        val executor = NetHunterTerminal(viewModelScope, File(appContext.filesDir, "terminal"))
        terminal = executor
        viewModelScope.launch {
            executor.commands.collect { commands ->
                _uiState.update { it.copy(terminalCommands = commands) }
            }
        }
        refreshVault()
    }

    fun refreshVault() {
        val context = appContext ?: return
        viewModelScope.launch(Dispatchers.IO) {
            vaultMutex.withLock {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                        throw SecurityException("Grant 'Allow access to manage all files' for the shared vault.")
                    }
                    val manager = vault ?: return@withLock
                    val location = manager.resolveLocation()
                    manager.ensureVaultTopology(location.root)
                    val facts = DynamicAdaptiveEngine.loadFacts(context)
                    val notes = manager.listNotes(location.root)
                    var indexError: String? = null
                    val indexed = try {
                        vaultIndex?.syncFilesystemToDatabase(manager) ?: 0
                    } catch (e: Exception) {
                        indexError = e.message ?: "Room index failed"
                        0
                    }
                    val session = auth?.reloadFromDisk()
                    val config = configManager?.reloadFromDisk()
                    val selected = _uiState.value.selectedNotePath
                    val currentNote = if (selected != null && !_uiState.value.noteDirty) {
                        try { manager.readNote(selected) } catch (_: Exception) { null }
                    } else null
                    _uiState.update { it.copy(
                        vaultPath = location.root.absolutePath,
                        vaultError = null,
                        adaptiveFacts = facts,
                        vaultNotes = notes,
                        indexedNotes = indexed,
                        indexError = indexError,
                        sharedSession = session,
                        sharedConfig = config,
                        selectedNoteText = currentNote ?: it.selectedNoteText,
                        selectedNoteOriginalText = currentNote ?: it.selectedNoteOriginalText
                    ) }
                    watch(location.root)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    observerJob?.cancel()
                    observer?.stopWatching()
                    observer = null
                    observerRoot = null
                    auth?.clearMemory()
                    configManager?.clearMemory()
                    _uiState.update { it.copy(
                        vaultError = e.message ?: "Shared vault is unavailable",
                        vaultPath = "",
                        vaultNotes = emptyList(),
                        indexedNotes = 0,
                        indexError = null,
                        adaptiveFacts = emptyList(),
                        sharedSession = null,
                        sharedConfig = null
                    ) }
                }
            }
        }
    }

    private fun watch(root: File) {
        if (observerRoot == root.absolutePath) return
        observerJob?.cancel()
        observer?.stopWatching()
        indexDatabase?.close()
        val next = VaultFileObserverManager(root, viewModelScope)
        observer = next
        observerRoot = root.absolutePath
        observerJob = viewModelScope.launch {
            next.events.collect { event ->
                when (event.kind) {
                    VaultChangeKind.AUTH, VaultChangeKind.FACTS,
                    VaultChangeKind.NOTES, VaultChangeKind.OTHER -> refreshVault()
                    VaultChangeKind.CHAT, VaultChangeKind.SCRIPTS -> Unit
                }
            }
        }
        next.startWatching()
    }

    fun selectNote(path: String) {
        val manager = vault ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val content = manager.readNote(path)
                _uiState.update { it.copy(selectedNotePath = path, selectedNoteText = content,
                    selectedNoteOriginalText = content, noteDirty = false, vaultError = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(vaultError = e.message) }
            }
        }
    }

    fun setNoteText(text: String) {
        _uiState.update { it.copy(selectedNoteText = text, noteDirty = true) }
    }
    fun setNewNoteTitle(title: String) { _uiState.update { it.copy(newNoteTitle = title) } }

    fun saveSelectedNote() {
        val manager = vault ?: return
        val state = _uiState.value
        val path = state.selectedNotePath ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                manager.saveNote(path, state.selectedNoteText, overwrite = true,
                    expectedContent = state.selectedNoteOriginalText)
                _uiState.update { it.copy(noteDirty = false, vaultError = null,
                    selectedNoteOriginalText = state.selectedNoteText) }
                refreshVault()
            } catch (e: Exception) {
                _uiState.update { it.copy(vaultError = e.message) }
            }
        }
    }

    fun createNote() {
        val manager = vault ?: return
        val title = _uiState.value.newNoteTitle.trim().removeSuffix(".md")
        if (title.isBlank() || title == "." || title == ".." ||
            title.contains('/') || title.contains('\\')) {
            _uiState.update { it.copy(vaultError = "Enter a note title without path separators.") }
            return
        }
        val path = "Concepts/$title.md"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                manager.saveNote(path, "", overwrite = false)
                _uiState.update { it.copy(newNoteTitle = "", selectedNotePath = path,
                    selectedNoteText = "", selectedNoteOriginalText = "",
                    noteDirty = false, vaultError = null) }
                refreshVault()
            } catch (e: Exception) {
                _uiState.update { it.copy(vaultError = e.message) }
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
        observerJob?.cancel()
        observer?.stopWatching()
        terminal?.stopAll()
        super.onCleared()
    }
}
