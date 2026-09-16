package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.api.ChatGptModel
import com.example.data.auth.OpenAIOAuthSession
import com.example.ui.components.ModelSelectorDialog
import com.example.ui.theme.VeniceTheme
import com.example.ui.viewmodel.VeniceUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChatGptModelSelectorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun connectedAccountExposesModelsAndOnlyTheirSupportedReasoning() {
        var selectedModel: String? = null
        var selectedEffort: String? = null
        val state = VeniceUiState(
            openAiSession = OpenAIOAuthSession("test", "test", "test", "test-account", Long.MAX_VALUE),
            selectedChatGptModelId = "account-model", chatGptReasoning = "low",
            chatGptModels = listOf(ChatGptModel("account-model", "Account model", "",
                listOf("low", "high"), "low", true)))
        compose.setContent {
            VeniceTheme {
                ModelSelectorDialog(onDismiss = {},
                    chatState = state, onSelectChatGptModel = { selectedModel = it },
                    onSelectReasoning = { selectedEffort = it })
            }
        }
        compose.onNodeWithTag("chatgpt_model_account-model").performScrollTo().performClick()
        assertEquals("account-model", selectedModel)
        compose.onNodeWithTag("chatgpt_reasoning_high").performScrollTo().performClick()
        assertEquals("high", selectedEffort)
        compose.onNodeWithTag("chatgpt_reasoning_xhigh").assertDoesNotExist()
        compose.onNodeWithTag("high_thinking_switch").assertDoesNotExist()
    }

    @Test fun disconnectedAccountShowsConnectionInstructions() {
        compose.setContent {
            VeniceTheme {
                ModelSelectorDialog(onDismiss = {}, chatState = VeniceUiState())
            }
        }
        compose.onNodeWithText("Connect ChatGPT in Privacy Vault to load your models.").assertExists()
        compose.onNodeWithTag("high_thinking_switch").assertDoesNotExist()
    }
}
