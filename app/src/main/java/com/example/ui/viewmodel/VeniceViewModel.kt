package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DirectorySnapshot
import com.example.data.local.FileSnapshot
import com.example.data.local.RootFileRepository
import com.example.data.model.LocalContextAttachment
import com.example.data.api.GeminiApi
import com.example.data.firebase.FirebaseManager
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.GeneratedArt
import com.example.data.model.Persona
import com.example.data.model.PrivacyTelemetry
import com.example.data.model.VeniceModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class VeniceNavTab {
    CHAT,
    IMAGE_STUDIO,
    INTELLIGENCE,
    PRIVACY_VAULT,
    LOCAL_FILES
}

data class LocalFilesUiState(
    val pathInput: String = "/root",
    val directory: DirectorySnapshot? = null,
    val file: FileSnapshot? = null,
    val busy: Boolean = false,
    val error: String? = null
)

data class VeniceUiState(
    val currentTab: VeniceNavTab = VeniceNavTab.CHAT,
    // Chat state
    val currentSession: ChatSession = ChatSession(),
    val savedSessions: List<ChatSession> = emptyList(),
    val selectedPersona: Persona = Persona.DEFAULT_PERSONAS.first(),
    val availablePersonas: List<Persona> = Persona.DEFAULT_PERSONAS,
    val selectedModel: VeniceModel = VeniceModel.BALANCED,
    val isHighThinkingEnabled: Boolean = false,
    val isGeneratingChat: Boolean = false,
    val chatInputText: String = "",
    val attachedLocalContext: LocalContextAttachment? = null,
    val localFiles: LocalFilesUiState = LocalFilesUiState(),
    val attachedImageBase64: String? = null,
    val attachedImageBitmap: Bitmap? = null,
    val chatErrorMessage: String? = null,

    // Image Studio state
    val imagePrompt: String = "",
    val selectedAspectRatio: String = "1:1",
    val selectedStylePreset: String = "Cinematic",
    val imageToEditBase64: String? = null,
    val isGeneratingImage: Boolean = false,
    val galleryArt: List<GeneratedArt> = emptyList(),
    val selectedArtDetail: GeneratedArt? = null,
    val imageErrorMessage: String? = null,

    // Intelligence Tools state
    val intelligenceTool: String = "ENHANCE_PROMPT",
    val intelligenceInput: String = "",
    val intelligenceOutput: String = "",
    val isRunningIntelligence: Boolean = false,

    // Privacy & Auth
    val currentUser: FirebaseUser? = null,
    val isZeroRetentionMode: Boolean = false,
    val isCloudSyncEnabled: Boolean = true,
    val privacyTelemetry: PrivacyTelemetry = PrivacyTelemetry(),
    val statusNotice: String? = null
)

class VeniceViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VeniceUiState())
    val uiState: StateFlow<VeniceUiState> = _uiState.asStateFlow()

    private val localRepository = RootFileRepository()
    private var localReadJob: Job? = null
    private var chatJob: Job? = null
    private var localReadVersion = 0L
    private var draftRevision = 0L

    init {
        checkAuthStatus()
        initializeInitialSession()
    }

    private fun checkAuthStatus() {
        val user = FirebaseManager.currentUser
        _uiState.value = _uiState.value.copy(
            currentUser = user,
            privacyTelemetry = _uiState.value.privacyTelemetry.copy(
                cloudSyncActive = user != null && _uiState.value.isCloudSyncEnabled
            )
        )
        if (user != null && _uiState.value.isCloudSyncEnabled) {
            loadCloudData()
        }
    }

    private fun initializeInitialSession() {
        val initialSession = ChatSession(
            title = "New Private Chat",
            personaId = Persona.DEFAULT_PERSONAS.first().id,
            selectedModel = VeniceModel.BALANCED.id,
            highThinkingEnabled = false,
            messages = listOf(
                ChatMessage(
                    role = "model",
                    text = "Welcome to Venice AI. Private, permissionless, and unrestricted intelligence. All sessions operate under client-first privacy architecture.",
                    modelUsed = VeniceModel.BALANCED.displayName
                )
            )
        )
        _uiState.value = _uiState.value.copy(
            currentSession = initialSession,
            savedSessions = listOf(initialSession)
        )
    }

    fun setNavTab(tab: VeniceNavTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab, statusNotice = null)
    }

    // Root access is requested only by an explicit browse/read action.
    fun setLocalPath(path: String) {
        _uiState.value = _uiState.value.copy(localFiles = _uiState.value.localFiles.copy(pathInput = path))
    }

    fun browseLocalDirectory(path: String = _uiState.value.localFiles.pathInput) {
        runLocalRead(path) { version ->
            val snapshot = localRepository.listDirectory(path)
            if (version == localReadVersion) {
                _uiState.value = _uiState.value.copy(localFiles = _uiState.value.localFiles.copy(
                    directory = snapshot, file = null, busy = false, error = null
                ))
            }
        }
    }

    fun previewLocalFile(path: String = _uiState.value.localFiles.pathInput) {
        runLocalRead(path) { version ->
            val snapshot = localRepository.readTextFile(path)
            if (version == localReadVersion) {
                _uiState.value = _uiState.value.copy(localFiles = _uiState.value.localFiles.copy(
                    file = snapshot, busy = false, error = null
                ))
            }
        }
    }

    private fun runLocalRead(path: String, operation: suspend (Long) -> Unit) {
        cancelLocalRead()
        val version = localReadVersion
        _uiState.value = _uiState.value.copy(localFiles = LocalFilesUiState(pathInput = path, busy = true))
        localReadJob = viewModelScope.launch {
            try {
                operation(version)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (version == localReadVersion) {
                    _uiState.value = _uiState.value.copy(localFiles = _uiState.value.localFiles.copy(
                        busy = false, error = e.message ?: "Local read failed (${e.javaClass.simpleName})."
                    ))
                }
            }
        }
    }

    fun cancelLocalRead() {
        localReadVersion++
        localReadJob?.cancel()
        localReadJob = null
        _uiState.value = _uiState.value.copy(localFiles = _uiState.value.localFiles.copy(busy = false))
    }

    fun clearLocalPreview() {
        cancelLocalRead()
        _uiState.value = _uiState.value.copy(localFiles = LocalFilesUiState(pathInput = _uiState.value.localFiles.pathInput))
    }

    fun attachLocalDirectory() {
        val snapshot = _uiState.value.localFiles.directory ?: return
        if (_uiState.value.localFiles.busy) return
        // Encode filenames as JSON so embedded newlines cannot impersonate listing rows.
        val entries = JSONArray()
        snapshot.entries.forEach { entry ->
            entries.put(JSONObject().put("name", entry.name).put("path", entry.path).put("kind", entry.kind.name))
        }
        attachLocalContext(LocalContextAttachment(
            path = snapshot.path, kind = "directory", capturedAt = snapshot.capturedAtEpochMs,
            content = entries.toString(), truncated = snapshot.truncated
        ))
    }

    fun attachLocalFile() {
        val snapshot = _uiState.value.localFiles.file ?: return
        if (_uiState.value.localFiles.busy) return
        attachLocalContext(LocalContextAttachment(
            path = snapshot.path, kind = "text", capturedAt = snapshot.capturedAtEpochMs,
            content = snapshot.text, truncated = snapshot.truncated
        ))
    }

    internal fun attachLocalContext(attachment: LocalContextAttachment) {
        draftRevision++
        _uiState.value = _uiState.value.copy(attachedLocalContext = attachment, currentTab = VeniceNavTab.CHAT)
    }

    fun removeLocalContext() {
        draftRevision++
        _uiState.value = _uiState.value.copy(attachedLocalContext = null)
    }

    // --- Chat Actions ---

    fun setChatInput(text: String) {
        draftRevision++
        _uiState.value = _uiState.value.copy(chatInputText = text)
    }

    fun attachImage(bitmap: Bitmap) {
        draftRevision++
        val base64 = GeminiApi.bitmapToBase64(bitmap)
        _uiState.value = _uiState.value.copy(
            attachedImageBitmap = bitmap,
            attachedImageBase64 = base64
        )
    }

    fun removeAttachedImage() {
        draftRevision++
        _uiState.value = _uiState.value.copy(
            attachedImageBitmap = null,
            attachedImageBase64 = null
        )
    }

    fun selectModel(model: VeniceModel) {
        _uiState.value = _uiState.value.copy(
            selectedModel = model,
            isHighThinkingEnabled = if (model != VeniceModel.PRO) false else _uiState.value.isHighThinkingEnabled,
            currentSession = _uiState.value.currentSession.copy(selectedModel = model.id)
        )
    }

    fun toggleHighThinking(enabled: Boolean) {
        // High thinking requirement: When High Thinking is enabled, model MUST be gemini-3.1-pro-preview
        val newModel = if (enabled) VeniceModel.PRO else _uiState.value.selectedModel
        _uiState.value = _uiState.value.copy(
            isHighThinkingEnabled = enabled,
            selectedModel = newModel,
            currentSession = _uiState.value.currentSession.copy(
                highThinkingEnabled = enabled,
                selectedModel = newModel.id
            )
        )
    }

    fun selectPersona(persona: Persona) {
        _uiState.value = _uiState.value.copy(
            selectedPersona = persona,
            currentSession = _uiState.value.currentSession.copy(personaId = persona.id)
        )
    }

    fun createCustomPersona(name: String, title: String, systemPrompt: String) {
        val newPersona = Persona(
            id = "custom_${UUID.randomUUID().toString().take(6)}",
            name = name,
            title = title,
            description = "User-defined custom persona",
            systemPrompt = systemPrompt,
            tag = "Custom",
            isCustom = true
        )
        val updatedList = _uiState.value.availablePersonas + newPersona
        _uiState.value = _uiState.value.copy(
            availablePersonas = updatedList,
            selectedPersona = newPersona
        )
    }

    fun startNewSession() {
        draftRevision++
        chatJob?.cancel()
        clearLocalPreview()
        val newSession = ChatSession(
            title = "New Private Chat",
            personaId = _uiState.value.selectedPersona.id,
            selectedModel = _uiState.value.selectedModel.id,
            highThinkingEnabled = _uiState.value.isHighThinkingEnabled,
            messages = listOf(
                ChatMessage(
                    role = "model",
                    text = "Venice session reset. Memory cleared. Operating in zero-retention mode.",
                    modelUsed = _uiState.value.selectedModel.displayName
                )
            )
        )
        _uiState.value = _uiState.value.copy(
            currentSession = newSession,
            isGeneratingChat = false,
            attachedLocalContext = null,
            chatInputText = "",
            attachedImageBase64 = null,
            attachedImageBitmap = null,
            chatErrorMessage = null,
            savedSessions = listOf(newSession) + _uiState.value.savedSessions.filter { it.id != newSession.id }
        )
    }

    fun switchSession(session: ChatSession) {
        draftRevision++
        chatJob?.cancel()
        clearLocalPreview()
        val persona = _uiState.value.availablePersonas.find { it.id == session.personaId } 
            ?: Persona.DEFAULT_PERSONAS.first()
        val model = VeniceModel.fromId(session.selectedModel)
        _uiState.value = _uiState.value.copy(
            currentSession = session,
            chatInputText = "",
            attachedImageBase64 = null,
            attachedImageBitmap = null,
            attachedLocalContext = null,
            isGeneratingChat = false,
            selectedPersona = persona,
            selectedModel = model,
            isHighThinkingEnabled = session.highThinkingEnabled,
            chatErrorMessage = null
        )
    }

    fun sendChatMessage() {
        val submitted = _uiState.value
        val submittedRevision = draftRevision
        if (submitted.isGeneratingChat) return
        val prompt = submitted.chatInputText.trim()
        if (prompt.isBlank() && submitted.attachedImageBase64 == null && submitted.attachedLocalContext == null) return

        val userMessage = ChatMessage(
            role = "user", text = prompt, imageBase64 = submitted.attachedImageBase64,
            localContext = submitted.attachedLocalContext, timestamp = System.currentTimeMillis()
        )
        val updatedMessages = submitted.currentSession.messages + userMessage
        val titleSource = prompt.ifBlank { submitted.attachedLocalContext?.path.orEmpty() }
        val updatedSession = submitted.currentSession.copy(
            title = if (submitted.currentSession.messages.size <= 1 && titleSource.isNotBlank())
                titleSource.take(30) + if (titleSource.length > 30) "..." else ""
            else submitted.currentSession.title,
            messages = updatedMessages, updatedAt = System.currentTimeMillis()
        )
        _uiState.value = submitted.copy(
            currentSession = updatedSession, chatInputText = "", attachedImageBase64 = null,
            attachedImageBitmap = null, attachedLocalContext = null,
            isGeneratingChat = true, chatErrorMessage = null
        )

        chatJob = viewModelScope.launch {
            val result = GeminiApi.generateChatResponse(
                modelName = if (submitted.isHighThinkingEnabled) "gemini-3.1-pro-preview" else submitted.selectedModel.id,
                history = updatedMessages,
                systemInstruction = submitted.selectedPersona.systemPrompt,
                enableHighThinking = submitted.isHighThinkingEnabled
            )
            if (_uiState.value.currentSession.id != updatedSession.id) return@launch
            if (!result.isSuccess) {
                // Keep the attachment available for retry; never turn an API failure into an assistant reply.
                val current = _uiState.value
                val restoreDraft = draftRevision == submittedRevision
                _uiState.value = current.copy(
                    currentSession = current.currentSession.copy(
                        messages = submitted.currentSession.messages, title = submitted.currentSession.title
                    ),
                    chatInputText = if (restoreDraft) submitted.chatInputText else current.chatInputText,
                    attachedLocalContext = if (restoreDraft) submitted.attachedLocalContext else current.attachedLocalContext,
                    attachedImageBase64 = if (restoreDraft) submitted.attachedImageBase64 else current.attachedImageBase64,
                    attachedImageBitmap = if (restoreDraft) submitted.attachedImageBitmap else current.attachedImageBitmap,
                    isGeneratingChat = false,
                    chatErrorMessage = result.errorMessage ?: "Gemini request failed."
                )
                return@launch
            }
            val assistantMessage = ChatMessage(
                role = "model", text = result.text, thoughtProcess = result.thoughtProcess,
                modelUsed = if (submitted.isHighThinkingEnabled) "Venice Pro (High Thinking)" else submitted.selectedModel.displayName,
                thinkingEnabled = submitted.isHighThinkingEnabled, timestamp = System.currentTimeMillis()
            )
            val finalSession = _uiState.value.currentSession.copy(
                messages = updatedMessages + assistantMessage, updatedAt = System.currentTimeMillis()
            )
            _uiState.value = _uiState.value.copy(
                currentSession = finalSession,
                savedSessions = _uiState.value.savedSessions.map { if (it.id == finalSession.id) finalSession else it },
                isGeneratingChat = false, chatErrorMessage = null
            )
            // Local attachment bytes stay in memory. Firebase's explicit message map omits them.
            if (!_uiState.value.isZeroRetentionMode && _uiState.value.isCloudSyncEnabled && FirebaseManager.isUserSignedIn) {
                FirebaseManager.syncSessionToFirestore(finalSession)
                FirebaseManager.syncMessageToFirestore(finalSession.id, userMessage)
                FirebaseManager.syncMessageToFirestore(finalSession.id, assistantMessage)
            }
        }
    }

    // --- Venice Image Studio (Create & Edit Images) ---

    fun setImagePrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(imagePrompt = prompt)
    }

    fun setAspectRatio(ratio: String) {
        _uiState.value = _uiState.value.copy(selectedAspectRatio = ratio)
    }

    fun setStylePreset(style: String) {
        _uiState.value = _uiState.value.copy(selectedStylePreset = style)
    }

    fun selectArtForEditing(art: GeneratedArt) {
        _uiState.value = _uiState.value.copy(
            imageToEditBase64 = art.imageBase64,
            imagePrompt = "Modify ${art.prompt}: ",
            selectedArtDetail = art
        )
    }

    fun clearImageToEdit() {
        _uiState.value = _uiState.value.copy(imageToEditBase64 = null)
    }

    fun setSelectedArtDetail(art: GeneratedArt?) {
        _uiState.value = _uiState.value.copy(selectedArtDetail = art)
    }

    fun generateOrEditImage() {
        val rawPrompt = _uiState.value.imagePrompt.trim()
        if (rawPrompt.isBlank()) return

        val isEdit = _uiState.value.imageToEditBase64 != null
        val fullPrompt = if (isEdit) {
            rawPrompt
        } else {
            "$rawPrompt, ${_uiState.value.selectedStylePreset} style, cinematic lighting, 8k render, masterpiece"
        }

        _uiState.value = _uiState.value.copy(
            isGeneratingImage = true,
            imageErrorMessage = null
        )

        viewModelScope.launch {
            val result = GeminiApi.generateOrEditImage(
                prompt = fullPrompt,
                aspectRatio = _uiState.value.selectedAspectRatio,
                baseImageToEdit = _uiState.value.imageToEditBase64
            )

            if (result.imageBase64 != null) {
                val newArt = GeneratedArt(
                    prompt = rawPrompt,
                    imageBase64 = result.imageBase64,
                    aspectRatio = _uiState.value.selectedAspectRatio,
                    style = _uiState.value.selectedStylePreset,
                    isEdit = isEdit,
                    originalPrompt = if (isEdit) _uiState.value.selectedArtDetail?.prompt else null
                )

                val updatedGallery = listOf(newArt) + _uiState.value.galleryArt
                _uiState.value = _uiState.value.copy(
                    galleryArt = updatedGallery,
                    selectedArtDetail = newArt,
                    isGeneratingImage = false,
                    imageToEditBase64 = null
                )

                // Sync to Firestore if authenticated
                if (!_uiState.value.isZeroRetentionMode && _uiState.value.isCloudSyncEnabled && FirebaseManager.isUserSignedIn) {
                    FirebaseManager.saveArtToFirestore(newArt)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isGeneratingImage = false,
                    imageErrorMessage = result.errorMessage ?: result.text
                )
            }
        }
    }

    // --- Venice Intelligence Tools ---

    fun setIntelligenceTool(tool: String) {
        _uiState.value = _uiState.value.copy(intelligenceTool = tool, intelligenceOutput = "")
    }

    fun setIntelligenceInput(text: String) {
        _uiState.value = _uiState.value.copy(intelligenceInput = text)
    }

    fun runIntelligenceTask() {
        val input = _uiState.value.intelligenceInput.trim()
        if (input.isBlank()) return

        _uiState.value = _uiState.value.copy(isRunningIntelligence = true)
        viewModelScope.launch {
            val output = GeminiApi.runIntelligenceTask(_uiState.value.intelligenceTool, input)
            _uiState.value = _uiState.value.copy(
                intelligenceOutput = output,
                isRunningIntelligence = false
            )
        }
    }

    fun applyEnhancedPromptToChat() {
        draftRevision++
        val enhanced = _uiState.value.intelligenceOutput
        if (enhanced.isNotBlank()) {
            _uiState.value = _uiState.value.copy(
                chatInputText = enhanced,
                currentTab = VeniceNavTab.CHAT
            )
        }
    }

    fun applyEnhancedPromptToStudio() {
        val enhanced = _uiState.value.intelligenceOutput
        if (enhanced.isNotBlank()) {
            _uiState.value = _uiState.value.copy(
                imagePrompt = enhanced,
                currentTab = VeniceNavTab.IMAGE_STUDIO
            )
        }
    }

    // --- Privacy & Firebase Auth ---

    fun signInAnonymously() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(statusNotice = "Connecting to Venice Anonymous Session...")
            val result = FirebaseManager.signInAnonymously()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    currentUser = result.getOrNull(),
                    statusNotice = "Venice Anonymous Session Active"
                )
                if (_uiState.value.isCloudSyncEnabled) {
                    loadCloudData()
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    statusNotice = "Anonymous mode error: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(statusNotice = "Initiating Google Sign-In...")
            val result = FirebaseManager.signInWithGoogle(context)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    currentUser = result.getOrNull(),
                    statusNotice = "Authenticated as ${result.getOrNull()?.email ?: "User"}"
                )
                if (_uiState.value.isCloudSyncEnabled) {
                    loadCloudData()
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    statusNotice = "Sign-in failed: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            FirebaseManager.signOut()
            _uiState.value = _uiState.value.copy(
                currentUser = null,
                statusNotice = "Signed out. Operating locally."
            )
        }
    }

    fun toggleZeroRetention(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            isZeroRetentionMode = enabled,
            statusNotice = if (enabled) "Zero Retention Mode Active: No logs stored" else "Normal Local Mode"
        )
    }

    fun toggleCloudSync(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            isCloudSyncEnabled = enabled,
            privacyTelemetry = _uiState.value.privacyTelemetry.copy(cloudSyncActive = enabled && FirebaseManager.isUserSignedIn),
            statusNotice = if (enabled) "Cloud Vault Sync Enabled" else "Cloud Sync Disabled"
        )
    }

    fun burnSessionAndData() {
        draftRevision++
        chatJob?.cancel()
        clearLocalPreview()
        _uiState.value = _uiState.value.copy(attachedLocalContext = null, isGeneratingChat = false)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(statusNotice = "Burning all data and session logs...")
            if (FirebaseManager.isUserSignedIn) {
                FirebaseManager.burnCloudData()
            }
            // Stop work started while the cloud deletion was pending before clearing memory.
            chatJob?.cancel()
            clearLocalPreview()
            draftRevision++
            val cleanSession = ChatSession(
                title = "New Private Chat",
                messages = listOf(
                    ChatMessage(
                        role = "model",
                        text = "Venice Panic Burn Complete. All chat history, local cache, and cloud vault records have been purged.",
                        modelUsed = VeniceModel.BALANCED.displayName
                    )
                )
            )
            _uiState.value = _uiState.value.copy(
                currentSession = cleanSession,
                attachedLocalContext = null,
                localFiles = LocalFilesUiState(),
                isGeneratingChat = false,
                savedSessions = listOf(cleanSession),
                galleryArt = emptyList(),
                selectedArtDetail = null,
                chatInputText = "",
                attachedImageBase64 = null,
                attachedImageBitmap = null,
                intelligenceInput = "",
                intelligenceOutput = "",
                statusNotice = "All data securely wiped."
            )
        }
    }

    private fun loadCloudData() {
        viewModelScope.launch {
            val sessions = FirebaseManager.loadSessionsFromFirestore()
            val gallery = FirebaseManager.loadGalleryFromFirestore()
            _uiState.value = _uiState.value.copy(
                savedSessions = if (sessions.isNotEmpty()) sessions else _uiState.value.savedSessions,
                galleryArt = if (gallery.isNotEmpty()) gallery else _uiState.value.galleryArt
            )
        }
    }
}
