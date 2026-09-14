package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.VeniceModel
import com.example.ui.components.ModelSelectorDialog
import com.example.ui.components.PersonaSelectorSheet
import com.example.ui.components.VeniceBottomNav
import com.example.ui.components.VeniceTopBar
import com.example.ui.screens.LocalFilesScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.IntelligenceScreen
import com.example.ui.screens.PrivacyAccountScreen
import com.example.ui.theme.VeniceBackground
import com.example.ui.theme.VeniceTheme
import com.example.ui.viewmodel.VeniceNavTab
import com.example.ui.viewmodel.VeniceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeniceTheme {
                VeniceApp()
            }
        }
    }
}

@Composable
fun VeniceApp(
    viewModel: VeniceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showModelDialog by remember { mutableStateOf(false) }
    var showPersonaSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            VeniceTopBar(
                currentModel = uiState.selectedModel,
                isHighThinking = uiState.isHighThinkingEnabled,
                isZeroRetention = uiState.isZeroRetentionMode,
                onOpenModelSelector = { showModelDialog = true },
                onToggleHighThinking = {
                    viewModel.toggleHighThinking(!uiState.isHighThinkingEnabled)
                },
                onOpenPersonaSelector = { showPersonaSheet = true },
                onBurnSession = { viewModel.burnSessionAndData() }
            )
        },
        bottomBar = {
            VeniceBottomNav(
                activeTab = uiState.currentTab,
                onTabSelected = { tab -> viewModel.setNavTab(tab) }
            )
        },
        containerColor = VeniceBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(VeniceBackground)
        ) {
            Crossfade(targetState = uiState.currentTab, label = "tab_crossfade") { tab ->
                when (tab) {
                    VeniceNavTab.LOCAL_FILES -> LocalFilesScreen(viewModel = viewModel, uiState = uiState)
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
            selectedModel = uiState.selectedModel,
            isHighThinking = uiState.isHighThinkingEnabled,
            onSelectModel = { model ->
                viewModel.selectModel(model)
                showModelDialog = false
            },
            onToggleHighThinking = { enabled ->
                viewModel.toggleHighThinking(enabled)
            },
            onDismiss = { showModelDialog = false }
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
