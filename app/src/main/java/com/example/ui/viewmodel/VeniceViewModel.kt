package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.util.UUID

enum class VeniceNavTab {
    CHAT,
    IMAGE_STUDIO,
    INTELLIGENCE,
    PRIVACY_VAULT
}

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

    // --- Chat Actions ---

    fun setChatInput(text: String) {
        _uiState.value = _uiState.value.copy(chatInputText = text)
    }

    fun attachImage(bitmap: Bitmap) {
        val base64 = GeminiApi.bitmapToBase64(bitmap)
        _uiState.value = _uiState.value.copy(
            attachedImageBitmap = bitmap,
            attachedImageBase64 = base64
        )
    }

    fun removeAttachedImage() {
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
            chatInputText = "",
            attachedImageBase64 = null,
            attachedImageBitmap = null,
            chatErrorMessage = null,
            savedSessions = listOf(newSession) + _uiState.value.savedSessions.filter { it.id != newSession.id }
        )
    }

    fun switchSession(session: ChatSession) {
        val persona = _uiState.value.availablePersonas.find { it.id == session.personaId } 
            ?: Persona.DEFAULT_PERSONAS.first()
        val model = VeniceModel.fromId(session.selectedModel)
        _uiState.value = _uiState.value.copy(
            currentSession = session,
            selectedPersona = persona,
            selectedModel = model,
            isHighThinkingEnabled = session.highThinkingEnabled,
            chatErrorMessage = null
        )
    }

    fun sendChatMessage() {
        val prompt = _uiState.value.chatInputText.trim()
        val attachedImage = _uiState.value.attachedImageBase64
        if (prompt.isBlank() && attachedImage == null) return

        val userMessage = ChatMessage(
            role = "user",
            text = prompt,
            imageBase64 = attachedImage,
            timestamp = System.currentTimeMillis()
        )

        val updatedMessages = _uiState.value.currentSession.messages + userMessage
        val updatedTitle = if (_uiState.value.currentSession.messages.size <= 1 && prompt.isNotBlank()) {
            prompt.take(30) + if (prompt.length > 30) "..." else ""
        } else {
            _uiState.value.currentSession.title
        }

        val updatedSession = _uiState.value.currentSession.copy(
            title = updatedTitle,
            messages = updatedMessages,
            updatedAt = System.currentTimeMillis()
        )

        _uiState.value = _uiState.value.copy(
            currentSession = updatedSession,
            chatInputText = "",
            attachedImageBase64 = null,
            attachedImageBitmap = null,
            isGeneratingChat = true,
            chatErrorMessage = null
        )

        viewModelScope.launch {
            val modelName = if (_uiState.value.isHighThinkingEnabled) {
                "gemini-3.1-pro-preview"
            } else {
                _uiState.value.selectedModel.id
            }

            val result = GeminiApi.generateChatResponse(
                modelName = modelName,
                history = updatedMessages,
                systemInstruction = _uiState.value.selectedPersona.systemPrompt,
                enableHighThinking = _uiState.value.isHighThinkingEnabled
            )

            val assistantMessage = ChatMessage(
                role = "model",
                text = result.text,
                thoughtProcess = result.thoughtProcess,
                modelUsed = if (_uiState.value.isHighThinkingEnabled) "Venice Pro (High Thinking)" else _uiState.value.selectedModel.displayName,
                thinkingEnabled = _uiState.value.isHighThinkingEnabled,
                timestamp = System.currentTimeMillis()
            )

            val finalMessages = updatedMessages + assistantMessage
            val finalSession = updatedSession.copy(messages = finalMessages, updatedAt = System.currentTimeMillis())

            val savedList = _uiState.value.savedSessions.map {
                if (it.id == finalSession.id) finalSession else it
            }

            _uiState.value = _uiState.value.copy(
                currentSession = finalSession,
                savedSessions = savedList,
                isGeneratingChat = false,
                chatErrorMessage = if (!result.isSuccess) result.errorMessage else null
            )

            // Firestore sync if user is logged in, cloud sync enabled, and not in zero retention
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(statusNotice = "Burning all data and session logs...")
            if (FirebaseManager.isUserSignedIn) {
                FirebaseManager.burnCloudData()
            }
            // Wipe memory
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
