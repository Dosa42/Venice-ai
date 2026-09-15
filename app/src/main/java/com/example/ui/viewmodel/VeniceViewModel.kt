package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.adaptive.AdaptiveCategory
import com.example.data.adaptive.AdaptiveFact
import com.example.data.adaptive.DynamicAdaptiveEngine
import com.example.data.api.ChatGptApi
import com.example.data.api.ChatGptModel
import com.example.data.api.ChatGptHttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import com.example.data.api.GeminiApi
import com.example.data.auth.OpenAIOAuthManager
import com.example.data.auth.OpenAIOAuthSession
import com.example.data.firebase.FirebaseManager
import com.example.data.introspection.CodebaseManifestEngine
import com.example.data.skills.AdaptivePromptEngine
import com.example.data.skills.NativeSkillsEngine
import com.example.data.skills.PromptHookResult
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.GeneratedArt
import com.example.data.model.NetHunterHardwareProfile
import com.example.data.model.Persona
import com.example.data.model.PrivacyTelemetry
import com.example.data.model.VeniceModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val useChatGpt: Boolean = false,
    val chatGptModels: List<ChatGptModel> = emptyList(),
    val selectedChatGptModelId: String? = null,
    val chatGptReasoning: String? = null,
    val isLoadingChatGptModels: Boolean = false,
    val chatGptModelError: String? = null,
    val streamingReply: ChatMessage? = null,
    val chatInputText: String = "",
    val attachedImageBase64: String? = null,
    val attachedImageBitmap: Bitmap? = null,
    val attachedFileName: String? = null,
    val attachedFileContent: String? = null,
    val attachedFileSize: Long = 0L,
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
    val intelligenceErrorMessage: String? = null,
    val isRunningIntelligence: Boolean = false,

    // Privacy & Auth
    val currentUser: FirebaseUser? = null,
    val isZeroRetentionMode: Boolean = false,
    val isCloudSyncEnabled: Boolean = true,
    val privacyTelemetry: PrivacyTelemetry = PrivacyTelemetry(),
    val statusNotice: String? = null,

    // Host & NetHunter Hardware Profile
    val hardwareProfile: NetHunterHardwareProfile = NetHunterHardwareProfile.DEFAULT,
    val injectHardwareProfile: Boolean = true,

    // Incoming Share Payload from NetHunter Terminal / External Apps
    val sharedTerminalPayload: String? = null,

    // OpenAI / ChatGPT OAuth PKCE
    val openAiSession: OpenAIOAuthSession? = null,
    val isOpenAiAuthenticating: Boolean = false,
    val openAiAuthStatus: String? = null,
    val openAiCustomClientId: String = OpenAIOAuthManager.DEFAULT_CLIENT_ID,

    // Dynamic Adaptive Framework (DAF)
    val adaptiveFacts: List<AdaptiveFact> = emptyList(),
    val isAutoLearningEnabled: Boolean = true,
    val lastLearnedNotice: String? = null,
    val dynamicAdaptivePromptContext: String = "",

    // Metacognitive Codebase Self-Awareness Engine
    val isSelfAwarenessEnabled: Boolean = true,
    val selectedIntrospectionFile: String? = null,

    // Native Android Skills & Adaptive Prompt Engine
    val skillsInjectionMode: AdaptivePromptEngine.InjectionMode = AdaptivePromptEngine.InjectionMode.ADAPTIVE,
    val enabledNativeSkills: Set<String> = NativeSkillsEngine.SKILLS.map { it.id }.toSet(),
    val lastPromptHookResult: PromptHookResult? = null
) {
    val chatModelLabel: String get() = if (useChatGpt) selectedChatGptModelId ?: "ChatGPT — select model"
        else if (isHighThinkingEnabled) "Venice Pro (High Thinking)" else selectedModel.displayName
    val chatThinkingEnabled: Boolean get() = if (useChatGpt) chatGptReasoning != null && chatGptReasoning != "none"
        else isHighThinkingEnabled
}

class VeniceViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VeniceUiState())
    val uiState: StateFlow<VeniceUiState> = _uiState.asStateFlow()

    private var appContext: Context? = null
    private var modelsJob: Job? = null
    private var chatJob: Job? = null
    private var chatGeneration = 0L
    private var accountGeneration = 0L

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
        if (_uiState.value.isGeneratingChat) return
        _uiState.value = _uiState.value.copy(
            selectedModel = model,
            useChatGpt = false,
            isHighThinkingEnabled = if (model != VeniceModel.PRO) false else _uiState.value.isHighThinkingEnabled,
            currentSession = _uiState.value.currentSession.copy(selectedModel = model.id, useChatGpt = false, reasoningEffort = null)
        )
    }

    fun toggleHighThinking(enabled: Boolean) {
        if (_uiState.value.useChatGpt || _uiState.value.isGeneratingChat) return
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
        cancelChat()
        val newSession = ChatSession(
            title = "New Private Chat",
            personaId = _uiState.value.selectedPersona.id,
            selectedModel = if (_uiState.value.useChatGpt) _uiState.value.selectedChatGptModelId.orEmpty() else _uiState.value.selectedModel.id,
            useChatGpt = _uiState.value.useChatGpt,
            reasoningEffort = _uiState.value.chatGptReasoning,
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
            attachedFileName = null,
            attachedFileContent = null,
            attachedFileSize = 0L,
            chatErrorMessage = null,
            savedSessions = listOf(newSession) + _uiState.value.savedSessions.filter { it.id != newSession.id }
        )
    }

    fun attachDocument(name: String, content: String, size: Long) {
        _uiState.value = _uiState.value.copy(
            attachedFileName = name,
            attachedFileContent = content,
            attachedFileSize = size
        )
    }

    fun removeAttachedDocument() {
        _uiState.value = _uiState.value.copy(
            attachedFileName = null,
            attachedFileContent = null,
            attachedFileSize = 0L
        )
    }

    fun switchSession(session: ChatSession) {
        cancelChat()
        val persona = _uiState.value.availablePersonas.find { it.id == session.personaId } 
            ?: Persona.DEFAULT_PERSONAS.first()
        val model = VeniceModel.fromId(session.selectedModel)
        _uiState.value = _uiState.value.copy(
            currentSession = session,
            useChatGpt = session.useChatGpt,
            selectedChatGptModelId = if (session.useChatGpt) session.selectedModel else _uiState.value.selectedChatGptModelId,
            chatGptReasoning = if (session.useChatGpt) session.reasoningEffort else _uiState.value.chatGptReasoning,
            selectedPersona = persona,
            selectedModel = model,
            isHighThinkingEnabled = session.highThinkingEnabled,
            chatErrorMessage = null
        )
    }

    fun sendChatMessage() {
        val snapshot = _uiState.value
        if (snapshot.isGeneratingChat) return
        val chatGptModel = snapshot.chatGptModels.find { it.id == snapshot.selectedChatGptModelId }
        if (snapshot.useChatGpt && (snapshot.openAiSession == null || chatGptModel == null || snapshot.isLoadingChatGptModels)) {
            _uiState.value = snapshot.copy(chatErrorMessage = "Connect ChatGPT and select an available model in the model menu.")
            return
        }
        if (snapshot.useChatGpt && snapshot.chatGptReasoning != null && snapshot.chatGptReasoning !in chatGptModel!!.reasoningLevels) {
            _uiState.value = snapshot.copy(chatErrorMessage = "Select a supported reasoning level in the model menu.")
            return
        }
        if (snapshot.useChatGpt && !chatGptModel!!.acceptsImages &&
            (snapshot.attachedImageBase64 != null || snapshot.currentSession.messages.any { it.imageBase64 != null })) {
            _uiState.value = snapshot.copy(chatErrorMessage = "This model does not accept images. Select an image-capable model or start a new chat.")
            return
        }
        val prompt = _uiState.value.chatInputText.trim()
        val attachedImage = _uiState.value.attachedImageBase64
        val attachedFileContent = _uiState.value.attachedFileContent
        val attachedFileName = _uiState.value.attachedFileName

        if (prompt.isBlank() && attachedImage == null && attachedFileContent == null) return

        val fullText = if (attachedFileContent != null) {
            if (prompt.isNotBlank()) {
                "Attached Document ($attachedFileName):\n```\n$attachedFileContent\n```\n\n$prompt"
            } else {
                "Please analyze this attached document ($attachedFileName):\n```\n$attachedFileContent\n```"
            }
        } else {
            prompt
        }

        val userMessage = ChatMessage(
            role = "user",
            text = fullText,
            imageBase64 = attachedImage,
            attachedFileName = attachedFileName,
            timestamp = System.currentTimeMillis()
        )

        val updatedMessages = _uiState.value.currentSession.messages + userMessage
        val updatedTitle = if (_uiState.value.currentSession.messages.size <= 1 && prompt.isNotBlank()) {
            prompt.take(30) + if (prompt.length > 30) "..." else ""
        } else if (_uiState.value.currentSession.messages.size <= 1 && attachedFileName != null) {
            "Analysis: $attachedFileName"
        } else {
            _uiState.value.currentSession.title
        }

        val updatedSession = _uiState.value.currentSession.copy(
            title = updatedTitle,
            useChatGpt = snapshot.useChatGpt,
            selectedModel = if (snapshot.useChatGpt) chatGptModel!!.id else snapshot.selectedModel.id,
            reasoningEffort = if (snapshot.useChatGpt) snapshot.chatGptReasoning else null,
            highThinkingEnabled = !snapshot.useChatGpt && snapshot.isHighThinkingEnabled,
            messages = updatedMessages,
            updatedAt = System.currentTimeMillis()
        )

        _uiState.value = _uiState.value.copy(
            currentSession = updatedSession,
            chatInputText = "",
            attachedImageBase64 = null,
            attachedImageBitmap = null,
            attachedFileName = null,
            attachedFileContent = null,
            attachedFileSize = 0L,
            isGeneratingChat = true,
            chatErrorMessage = null,
            streamingReply = null
        )

        val generation = ++chatGeneration
        chatJob = viewModelScope.launch {
          try {
            val modelName = if (snapshot.isHighThinkingEnabled) {
                "gemini-3.1-pro-preview"
            } else {
                snapshot.selectedModel.id
            }

            val basePrompt = snapshot.selectedPersona.systemPrompt
            val hardwarePart = if (snapshot.injectHardwareProfile && snapshot.hardwareProfile.isEnabled) {
                "\n\n${snapshot.hardwareProfile.toSystemPromptContext()}"
            } else ""
            val adaptivePart = if (snapshot.dynamicAdaptivePromptContext.isNotBlank()) {
                "\n\n${snapshot.dynamicAdaptivePromptContext}"
            } else ""
            val selfAwarenessPart = if (snapshot.isSelfAwarenessEnabled) {
                "\n\n${CodebaseManifestEngine.buildSelfIntrospectionContext(snapshot.selectedIntrospectionFile)}"
            } else ""

            val skillsHookResult = AdaptivePromptEngine.synthesizePromptHook(
                userQuery = fullText,
                mode = snapshot.skillsInjectionMode,
                manualSelectedSkills = snapshot.enabledNativeSkills
            )
            val skillsPart = if (skillsHookResult.promptContext.isNotBlank()) {
                "\n\n${skillsHookResult.promptContext}"
            } else ""

            val effectiveSystemPrompt = "$basePrompt$hardwarePart$adaptivePart$selfAwarenessPart$skillsPart"

            _uiState.value = _uiState.value.copy(
                lastPromptHookResult = skillsHookResult
            )

            val assistantMessage: ChatMessage
            if (snapshot.useChatGpt) {
                val preview = ChatMessage(role = "model", text = "", modelUsed = chatGptModel!!.id,
                    thinkingEnabled = snapshot.chatThinkingEnabled)
                val text = withChatGptSession { session ->
                    ChatGptApi.stream(session, chatGptModel, snapshot.chatGptReasoning,
                        updatedMessages, effectiveSystemPrompt) { partial ->
                        viewModelScope.launch {
                            if (generation == chatGeneration && _uiState.value.isGeneratingChat) {
                                _uiState.value = _uiState.value.copy(streamingReply = preview.copy(text = partial))
                            }
                        }
                    }
                }
                assistantMessage = preview.copy(text = text)
            } else {
                val result = GeminiApi.generateChatResponse(
                    modelName = modelName, history = updatedMessages,
                    systemInstruction = effectiveSystemPrompt,
                    enableHighThinking = snapshot.isHighThinkingEnabled
                )
                if (!result.isSuccess) {
                    coroutineContext.ensureActive()
                    if (generation == chatGeneration) {
                        _uiState.value = _uiState.value.copy(isGeneratingChat = false,
                            chatErrorMessage = result.errorMessage ?: "Gemini request failed.")
                    }
                    return@launch
                }
                assistantMessage = ChatMessage(role = "model", text = result.text,
                    thoughtProcess = result.thoughtProcess, modelUsed = snapshot.chatModelLabel,
                    thinkingEnabled = snapshot.isHighThinkingEnabled)
            }
            coroutineContext.ensureActive()
            if (generation != chatGeneration) return@launch

            val finalMessages = updatedMessages + assistantMessage
            val finalSession = updatedSession.copy(messages = finalMessages, updatedAt = System.currentTimeMillis())

            val savedList = _uiState.value.savedSessions.map {
                if (it.id == finalSession.id) finalSession else it
            }

            _uiState.value = _uiState.value.copy(
                currentSession = finalSession,
                savedSessions = savedList,
                isGeneratingChat = false,
                chatErrorMessage = null,
                streamingReply = null
            )

            // Firestore sync if user is logged in, cloud sync enabled, and not in zero retention
            if (!_uiState.value.isZeroRetentionMode && _uiState.value.isCloudSyncEnabled && FirebaseManager.isUserSignedIn) {
                FirebaseManager.syncSessionToFirestore(finalSession)
                FirebaseManager.syncMessageToFirestore(finalSession.id, userMessage)
                FirebaseManager.syncMessageToFirestore(finalSession.id, assistantMessage)
            }
          } catch (e: CancellationException) {
              throw e
          } catch (e: Exception) {
              if (generation == chatGeneration) {
                  _uiState.value = _uiState.value.copy(isGeneratingChat = false,
                      chatErrorMessage = e.message ?: "Chat request failed. Please retry.")
              }
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

            if (result.isSuccess && result.imageBase64 != null) {
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
        _uiState.value = _uiState.value.copy(intelligenceTool = tool, intelligenceOutput = "", intelligenceErrorMessage = null)
    }

    fun setIntelligenceInput(text: String) {
        _uiState.value = _uiState.value.copy(intelligenceInput = text)
    }

    fun runIntelligenceTask() {
        val input = _uiState.value.intelligenceInput.trim()
        if (input.isBlank()) return

        val hardwareContext = buildString {
            if (_uiState.value.injectHardwareProfile && _uiState.value.hardwareProfile.isEnabled) {
                append(_uiState.value.hardwareProfile.toSystemPromptContext())
            }
            if (_uiState.value.dynamicAdaptivePromptContext.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append(_uiState.value.dynamicAdaptivePromptContext)
            }
            if (_uiState.value.isSelfAwarenessEnabled || _uiState.value.intelligenceTool == "SELF_CODEBASE_INSPECTION") {
                if (isNotEmpty()) append("\n\n")
                append(CodebaseManifestEngine.buildSelfIntrospectionContext(_uiState.value.selectedIntrospectionFile))
            }
        }.takeIf { it.isNotBlank() }

        _uiState.value = _uiState.value.copy(isRunningIntelligence = true, intelligenceOutput = "", intelligenceErrorMessage = null)
        viewModelScope.launch {
            val result = GeminiApi.runIntelligenceTask(
                taskType = _uiState.value.intelligenceTool,
                input = input,
                hardwareContext = hardwareContext
            )
            _uiState.value = _uiState.value.copy(
                intelligenceOutput = if (result.isSuccess) result.text else "",
                intelligenceErrorMessage = if (result.isSuccess) null else result.errorMessage ?: "Gemini request failed.",
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

    fun sendAnalysisToChat(taskTitle: String, content: String) {
        if (content.isBlank()) return
        val formatted = "Terminal / Diagnostic Analysis for [$taskTitle]:\n\n$content\n\nHow do I test or apply this in my NetHunter terminal?"
        _uiState.value = _uiState.value.copy(
            chatInputText = formatted,
            currentTab = VeniceNavTab.CHAT
        )
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
        cancelChat()
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

    // --- Host & NetHunter Hardware Profile Controls ---

    fun toggleHardwareProfileInjection(enabled: Boolean) {
        val updatedProfile = _uiState.value.hardwareProfile.copy(isEnabled = enabled)
        _uiState.value = _uiState.value.copy(
            hardwareProfile = updatedProfile,
            injectHardwareProfile = enabled,
            statusNotice = if (enabled) "NetHunter SM-A326B Hardware Blueprint Active" else "Hardware Blueprint Inactive"
        )
    }

    fun updateHardwareProfile(profile: NetHunterHardwareProfile) {
        _uiState.value = _uiState.value.copy(
            hardwareProfile = profile,
            statusNotice = "Hardware Blueprint Updated"
        )
    }

    fun resetHardwareProfileToDefaults() {
        _uiState.value = _uiState.value.copy(
            hardwareProfile = NetHunterHardwareProfile.DEFAULT,
            injectHardwareProfile = true,
            statusNotice = "Restored Samsung SM-A326B NetHunter Blueprint"
        )
    }

    // --- External NetHunter Terminal Share Intent Receiver ---

    fun handleIncomingSharedContent(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        _uiState.value = _uiState.value.copy(
            currentTab = VeniceNavTab.CHAT,
            sharedTerminalPayload = trimmed,
            chatInputText = if (_uiState.value.chatInputText.isBlank()) {
                trimmed
            } else {
                "${_uiState.value.chatInputText}\n\n$trimmed"
            },
            statusNotice = "📥 Received payload from NetHunter Terminal"
        )
    }

    fun dismissSharedPayload() {
        _uiState.value = _uiState.value.copy(sharedTerminalPayload = null)
    }

    fun sendSharedPayloadToDiagnostic() {
        val payload = _uiState.value.sharedTerminalPayload ?: _uiState.value.chatInputText
        if (payload.isNotBlank()) {
            _uiState.value = _uiState.value.copy(
                currentTab = VeniceNavTab.INTELLIGENCE,
                intelligenceTool = "DIAGNOSE_TERMINAL",
                intelligenceInput = payload,
                sharedTerminalPayload = null,
                statusNotice = "Transferred to Terminal Diagnostic Tool"
            )
        }
    }

    // --- OpenAI / ChatGPT OAuth PKCE Actions ---

    fun loadOpenAiSession(context: Context) {
        val firstLoad = appContext == null
        appContext = context.applicationContext
        val session = OpenAIOAuthManager.loadSession(context)
        val changed = session?.accountId != _uiState.value.openAiSession?.accountId
        _uiState.value = _uiState.value.copy(openAiSession = session,
            openAiCustomClientId = OpenAIOAuthManager.getClientId(context))
        if (session != null && (firstLoad || changed)) {
            _uiState.value = _uiState.value.copy(useChatGpt = true)
            loadChatGptModels()
        }
    }

    private fun cancelChat() {
        ++chatGeneration
        chatJob?.cancel()
        _uiState.value = _uiState.value.copy(isGeneratingChat = false, streamingReply = null)
    }

    private suspend fun <T> withChatGptSession(action: suspend (OpenAIOAuthSession) -> T): T {
        val context = appContext ?: error("Open ChatGPT account settings first.")
        val generation = accountGeneration
        val session = OpenAIOAuthManager.refreshIfNeeded(context).getOrThrow()
        coroutineContext.ensureActive()
        check(generation == accountGeneration) { "ChatGPT account changed. Please retry." }
        _uiState.value = _uiState.value.copy(openAiSession = session)
        return try {
            action(session)
        } catch (e: ChatGptHttpException) {
            if (e.status != 401) throw e
            val refreshed = OpenAIOAuthManager.refreshIfNeeded(context, session.accessToken).getOrThrow()
            coroutineContext.ensureActive()
            check(generation == accountGeneration) { "ChatGPT account changed. Please retry." }
            _uiState.value = _uiState.value.copy(openAiSession = refreshed)
            action(refreshed) // A rejected HTTP request has not started streaming; retry once only.
        }
    }

    fun loadChatGptModels() {
        if (_uiState.value.openAiSession == null || _uiState.value.isGeneratingChat) return
        modelsJob?.cancel()
        _uiState.value = _uiState.value.copy(isLoadingChatGptModels = true, chatGptModelError = null)
        modelsJob = viewModelScope.launch {
            try {
                val models = withChatGptSession { ChatGptApi.models(it) }
                val state = _uiState.value
                val selected = if (state.selectedChatGptModelId == null) models.first()
                    else models.find { it.id == state.selectedChatGptModelId }
                val effort = state.chatGptReasoning?.takeIf { it in selected?.reasoningLevels.orEmpty() }
                    ?: selected?.defaultReasoning
                _uiState.value = state.copy(chatGptModels = models, isLoadingChatGptModels = false,
                    selectedChatGptModelId = selected?.id ?: state.selectedChatGptModelId,
                    chatGptReasoning = effort,
                    chatGptModelError = if (selected == null) "Previously selected model is unavailable. Choose a model below." else null,
                    currentSession = if (state.useChatGpt) state.currentSession.copy(useChatGpt = true,
                        selectedModel = selected?.id ?: state.selectedChatGptModelId.orEmpty(), reasoningEffort = effort)
                        else state.currentSession)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(chatGptModels = emptyList(), isLoadingChatGptModels = false,
                    chatGptModelError = e.message ?: "Could not load ChatGPT models.")
            }
        }
    }

    fun selectChatGptModel(id: String) {
        val state = _uiState.value
        if (state.isGeneratingChat) return
        val model = state.chatGptModels.find { it.id == id } ?: return
        val effort = if (id == state.selectedChatGptModelId) state.chatGptReasoning else model.defaultReasoning
        _uiState.value = state.copy(useChatGpt = true, selectedChatGptModelId = id,
            chatGptReasoning = effort, chatErrorMessage = null,
            currentSession = state.currentSession.copy(useChatGpt = true, selectedModel = id, reasoningEffort = effort))
    }

    fun selectChatGptReasoning(effort: String) {
        val state = _uiState.value
        if (state.isGeneratingChat || !state.useChatGpt) return
        val model = state.chatGptModels.find { it.id == state.selectedChatGptModelId } ?: return
        if (effort !in model.reasoningLevels) return
        _uiState.value = state.copy(chatGptReasoning = effort,
            currentSession = state.currentSession.copy(reasoningEffort = effort))
    }

    fun startOpenAiPkceLogin(context: Context) {
        appContext = context.applicationContext
        if (_uiState.value.isOpenAiAuthenticating) return
        _uiState.value = _uiState.value.copy(
            isOpenAiAuthenticating = true,
            openAiAuthStatus = "Starting ChatGPT OAuth PKCE flow..."
        )

        viewModelScope.launch {
            val result = OpenAIOAuthManager.authenticate(
                context = context,
                onStatusUpdate = { status ->
                    _uiState.update { it.copy(openAiAuthStatus = status) }
                }
            )

            result.onSuccess { session ->
                ++accountGeneration
                cancelChat()
                _uiState.value = _uiState.value.copy(
                    openAiSession = session,
                    useChatGpt = true,
                    selectedChatGptModelId = null,
                    chatGptReasoning = null,
                    chatGptModels = emptyList(),
                    isOpenAiAuthenticating = false,
                    openAiAuthStatus = "Connected to ChatGPT (Account: ${session.accountId.take(12)}...)",
                    statusNotice = "ChatGPT OAuth PKCE Connected"
                )
                loadChatGptModels()
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isOpenAiAuthenticating = false,
                    openAiAuthStatus = "Authentication failed: ${error.message ?: "Unknown error"}",
                    statusNotice = "ChatGPT OAuth failed: ${error.message}"
                )
            }
        }
    }

    fun refreshOpenAiSession(context: Context) {
        val generation = accountGeneration
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                openAiAuthStatus = "Refreshing OpenAI token with refresh_token..."
            )
            val result = OpenAIOAuthManager.refreshIfNeeded(context)
            if (generation != accountGeneration) return@launch
            result.onSuccess { refreshed ->
                _uiState.value = _uiState.value.copy(
                    openAiSession = refreshed,
                    openAiAuthStatus = "Token refreshed successfully.",
                    statusNotice = "OpenAI Token Refreshed"
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    openAiAuthStatus = "Refresh failed: ${err.message}"
                )
            }
        }
    }

    fun disconnectOpenAi(context: Context) {
        ++accountGeneration
        modelsJob?.cancel()
        if (_uiState.value.useChatGpt) cancelChat()
        OpenAIOAuthManager.clearSession(context)
        _uiState.value = _uiState.value.copy(
            openAiSession = null,
            chatGptModels = emptyList(),
            selectedChatGptModelId = null,
            chatGptReasoning = null,
            isLoadingChatGptModels = false,
            chatGptModelError = null,
            openAiAuthStatus = null,
            statusNotice = "ChatGPT OAuth Session Disconnected"
        )
    }

    fun updateOpenAiClientId(context: Context, newClientId: String) {
        OpenAIOAuthManager.setClientId(context, newClientId)
        _uiState.value = _uiState.value.copy(
            openAiCustomClientId = newClientId.ifBlank { OpenAIOAuthManager.DEFAULT_CLIENT_ID }
        )
    }

    // --- Dynamic Adaptive Framework (DAF) Engine Actions ---

    fun initAdaptiveFramework(context: Context) {
        val facts = DynamicAdaptiveEngine.loadFacts(context)
        val isAuto = DynamicAdaptiveEngine.isAutoLearningEnabled(context)
        val contextPrompt = DynamicAdaptiveEngine.buildAdaptiveContext(context)
        _uiState.value = _uiState.value.copy(
            adaptiveFacts = facts,
            isAutoLearningEnabled = isAuto,
            dynamicAdaptivePromptContext = contextPrompt
        )
    }

    fun learnFromText(context: Context, text: String, source: String = "Terminal / User Input") {
        if (text.isBlank()) return
        val discovered = DynamicAdaptiveEngine.autoLearnFromText(context, text, source)
        val updatedFacts = DynamicAdaptiveEngine.loadFacts(context)
        val updatedContext = DynamicAdaptiveEngine.buildAdaptiveContext(context)
        val notice = if (discovered.isNotEmpty()) {
            "Adapted ${discovered.size} runtime fact(s): " + discovered.joinToString { it.key }
        } else {
            "Analyzed input: no new runtime deviations detected"
        }
        _uiState.value = _uiState.value.copy(
            adaptiveFacts = updatedFacts,
            dynamicAdaptivePromptContext = updatedContext,
            lastLearnedNotice = notice,
            statusNotice = notice
        )
    }

    fun addCustomAdaptiveFact(
        context: Context,
        category: AdaptiveCategory,
        key: String,
        value: String
    ) {
        if (key.isBlank() || value.isBlank()) return
        val newFact = AdaptiveFact(
            category = category,
            key = key.trim(),
            value = value.trim(),
            confidence = 1.0f,
            source = "Custom User Directive",
            isEnabled = true
        )
        val updated = DynamicAdaptiveEngine.addOrUpdateFact(context, newFact)
        val updatedContext = DynamicAdaptiveEngine.buildAdaptiveContext(context)
        _uiState.value = _uiState.value.copy(
            adaptiveFacts = updated,
            dynamicAdaptivePromptContext = updatedContext,
            statusNotice = "Added adaptive rule: ${newFact.key}"
        )
    }

    fun removeAdaptiveFact(context: Context, id: String) {
        val updated = DynamicAdaptiveEngine.removeFact(context, id)
        val updatedContext = DynamicAdaptiveEngine.buildAdaptiveContext(context)
        _uiState.value = _uiState.value.copy(
            adaptiveFacts = updated,
            dynamicAdaptivePromptContext = updatedContext,
            statusNotice = "Removed adaptive rule"
        )
    }

    fun toggleAdaptiveFact(context: Context, id: String, isEnabled: Boolean) {
        val updated = DynamicAdaptiveEngine.toggleFact(context, id, isEnabled)
        val updatedContext = DynamicAdaptiveEngine.buildAdaptiveContext(context)
        _uiState.value = _uiState.value.copy(
            adaptiveFacts = updated,
            dynamicAdaptivePromptContext = updatedContext
        )
    }

    fun toggleAutoLearning(context: Context, enabled: Boolean) {
        DynamicAdaptiveEngine.setAutoLearningEnabled(context, enabled)
        _uiState.value = _uiState.value.copy(
            isAutoLearningEnabled = enabled,
            statusNotice = if (enabled) "Runtime Auto-Learning Activated" else "Runtime Auto-Learning Paused"
        )
    }

    fun resetAdaptiveFacts(context: Context) {
        val defaults = DynamicAdaptiveEngine.resetToDefaults(context)
        val updatedContext = DynamicAdaptiveEngine.buildAdaptiveContext(context)
        _uiState.value = _uiState.value.copy(
            adaptiveFacts = defaults,
            dynamicAdaptivePromptContext = updatedContext,
            statusNotice = "Reset adaptive framework to blueprint defaults"
        )
    }

    fun clearAdaptiveFacts(context: Context) {
        val empty = DynamicAdaptiveEngine.clearAll(context)
        _uiState.value = _uiState.value.copy(
            adaptiveFacts = empty,
            dynamicAdaptivePromptContext = "",
            statusNotice = "Cleared all adaptive runtime memory"
        )
    }

    // --- Metacognitive Codebase Self-Awareness Actions ---

    fun toggleSelfAwareness(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            isSelfAwarenessEnabled = enabled,
            statusNotice = if (enabled) "Metacognitive Codebase Introspection Enabled" else "Codebase Introspection Disabled"
        )
    }

    fun selectIntrospectionFile(path: String?) {
        _uiState.value = _uiState.value.copy(
            selectedIntrospectionFile = path
        )
    }

    // --- Native Skills Adaptive Prompt Engine Actions ---

    fun setSkillsInjectionMode(mode: AdaptivePromptEngine.InjectionMode) {
        _uiState.value = _uiState.value.copy(
            skillsInjectionMode = mode,
            statusNotice = "Skills Prompt Mode: ${mode.title}"
        )
    }

    fun toggleNativeSkill(skillId: String) {
        val current = _uiState.value.enabledNativeSkills.toMutableSet()
        if (current.contains(skillId)) {
            current.remove(skillId)
        } else {
            current.add(skillId)
        }
        _uiState.value = _uiState.value.copy(
            enabledNativeSkills = current,
            statusNotice = "Updated active skills (${current.size}/${NativeSkillsEngine.SKILLS.size})"
        )
    }

    fun enableAllNativeSkills() {
        _uiState.value = _uiState.value.copy(
            enabledNativeSkills = NativeSkillsEngine.SKILLS.map { it.id }.toSet(),
            statusNotice = "Enabled all 6 core Android skills"
        )
    }
}
