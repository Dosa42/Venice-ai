package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ModelSelectorDialog
import com.example.ui.components.PersonaSelectorSheet
import com.example.ui.components.VeniceBottomNav
import com.example.ui.components.VeniceTopBar
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.IntelligenceScreen
import com.example.ui.screens.PrivacyAccountScreen
import com.example.ui.theme.VeniceBackground
import com.example.ui.theme.VeniceTheme
import com.example.ui.viewmodel.VeniceNavTab
import com.example.ui.viewmodel.VeniceViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: VeniceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.loadOpenAiSession(this)
        viewModel.initAdaptiveFramework(this)
        handleIncomingIntent(intent)
        setContent {
            VeniceTheme {
                VeniceApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.handleIncomingSharedContent(sharedText)
                return
            }

            @Suppress("DEPRECATION")
            val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (streamUri != null) {
                try {
                    contentResolver.openInputStream(streamUri)?.bufferedReader()?.use { reader ->
                        val buffer = CharArray(65536)
                        val numRead = reader.read(buffer, 0, buffer.size)
                        if (numRead > 0) {
                            val content = String(buffer, 0, numRead)
                            viewModel.handleIncomingSharedContent(content)
                        }
                    }
                } catch (_: Exception) {
                    // Safely ignore file read errors
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun VeniceApp(
    viewModel: VeniceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val chatKeyboardOpen = uiState.currentTab == VeniceNavTab.CHAT && WindowInsets.isImeVisible

    var showModelDialog by remember { mutableStateOf(false) }
    var showPersonaSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            VeniceTopBar(
                isHighThinking = uiState.chatThinkingEnabled,
                modelLabel = uiState.chatModelLabel,
                reasoningLabel = uiState.chatGptReasoning ?: "Reasoning",
                isZeroRetention = uiState.isZeroRetentionMode,
                compact = uiState.currentTab == VeniceNavTab.CHAT,
                onOpenModelSelector = { showModelDialog = true },
                onToggleHighThinking = { showModelDialog = true },
                onOpenPersonaSelector = { showPersonaSheet = true },
                onBurnSession = { viewModel.burnSessionAndData() }
            )
        },
        bottomBar = {
            if (!chatKeyboardOpen) {
                VeniceBottomNav(
                    activeTab = uiState.currentTab,
                    onTabSelected = { tab -> viewModel.setNavTab(tab) }
                )
            }
        },
        containerColor = VeniceBackground,
        modifier = Modifier.fillMaxSize().testTag("venice_app")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .background(VeniceBackground)
        ) {
            Crossfade(targetState = uiState.currentTab, label = "tab_crossfade") { tab ->
                when (tab) {
                    VeniceNavTab.CHAT -> ChatScreen(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                    VeniceNavTab.IMAGE_STUDIO -> ImageStudioScreen(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                    VeniceNavTab.INTELLIGENCE -> IntelligenceScreen(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                    VeniceNavTab.PRIVACY_VAULT -> PrivacyAccountScreen(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                }
            }
        }
    }

    // Model Selector Dialog
    if (showModelDialog) {
        ModelSelectorDialog(
            onDismiss = { showModelDialog = false },
            chatState = uiState,
            onSelectChatGptModel = viewModel::selectChatGptModel,
            onSelectReasoning = viewModel::selectChatGptReasoning,
            onRefreshChatGptModels = viewModel::loadChatGptModels
        )
    }

    // Persona Selector Dialog / Sheet
    if (showPersonaSheet) {
        PersonaSelectorSheet(
            personas = uiState.availablePersonas,
            selectedPersona = uiState.selectedPersona,
            onSelectPersona = { persona ->
                viewModel.selectPersona(persona)
                showPersonaSheet = false
            },
            onCreateCustomPersona = { name, title, prompt ->
                viewModel.createCustomPersona(name, title, prompt)
            },
            onDismiss = { showPersonaSheet = false }
        )
    }
}
