package com.example

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.data.local.TerminalCommand
import com.example.data.model.ChatMessage
import com.example.ui.components.VeniceTopBar
import com.example.ui.screens.ChatScreen
import com.example.ui.theme.VeniceTheme
import com.example.ui.viewmodel.VeniceNavTab
import com.example.ui.viewmodel.VeniceViewModel
import java.io.File
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w360dp-h800dp-xhdpi")
class ChatLayoutAndPickerTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun narrowReducedViewportKeepsFourTypedLinesVisibleWithTerminalResults() {
        val viewModel = VeniceViewModel()
        compose.setContent {
            val state by viewModel.uiState.collectAsState()
            VeniceTheme {
                Column(Modifier.requiredSize(320.dp, 340.dp).testTag("phone_viewport")) {
                    VeniceTopBar(isHighThinking = true, isZeroRetention = false,
                        onOpenModelSelector = {}, onToggleHighThinking = {},
                        onOpenPersonaSelector = {}, onBurnSession = {}, compact = true,
                        modelLabel = "A very long account model name", reasoningLabel = "medium")
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        ChatScreen(viewModel, state.copy(
                            selectedPersona = state.selectedPersona.copy(name = "Venice Unfiltered with a very long name"),
                            currentSession = state.currentSession.copy(messages = listOf(
                                ChatMessage(role = "user", text = "Inspect the terminal"),
                                ChatMessage(role = "model", text = "Command completed successfully.")
                            )),
                            terminalCommands = listOf(TerminalCommand("command", "id; pwd; uname -m",
                                "/root", "nethunter", status = "completed", exitCode = 0,
                                stdoutPath = "/tmp/stdout", stderrPath = "/tmp/stderr"))
                        ))
                    }
                }
            }
        }
        val text = "First line\nSecond line\nThird line\nFourth line"
        compose.onNodeWithTag("chat_input_field").performClick().performTextInput(text)
        compose.onNodeWithTag("chat_input_field").assertIsDisplayed().assertTextContains(text)
        val viewport = compose.onNodeWithTag("phone_viewport").getUnclippedBoundsInRoot()
        val field = compose.onNodeWithTag("chat_input_field").getUnclippedBoundsInRoot()
        val context = compose.onNodeWithTag("chat_context_row").getUnclippedBoundsInRoot()
        val header = compose.onNodeWithTag("compact_chat_header").getUnclippedBoundsInRoot()
        assertTrue("Composer must remain inside the viewport", field.bottom <= viewport.bottom)
        assertTrue("Four lines must have room to display", field.height >= 96.dp)
        assertTrue("Context must remain one short row", context.height < 36.dp)
        assertTrue("Header must remain compact", header.height <= 56.dp)
        assertEquals(text, viewModel.uiState.value.chatInputText)
    }

    @Test fun keyboardInsetsPutComposerAboveKeyboardWithoutTheNavigationGap() {
        lateinit var composeView: View
        val viewModel = VeniceViewModel()
        compose.runOnUiThread { compose.activity.enableEdgeToEdge() }
        compose.setContent {
            composeView = LocalView.current
            VeniceTheme { VeniceApp(viewModel) }
        }
        compose.onNodeWithTag("chat_input_field").performClick().performTextInput("Visible while typing")
        val density = compose.activity.resources.displayMetrics.density
        fun applyKeyboard(visible: Boolean) {
            compose.runOnUiThread {
                ViewCompat.dispatchApplyWindowInsets(composeView, WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, (24 * density).toInt(), 0, (24 * density).toInt()))
                    .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, if (visible) (280 * density).toInt() else 0))
                    .setVisible(WindowInsetsCompat.Type.ime(), visible).build())
            }
            compose.waitForIdle()
        }
        applyKeyboard(true)
        compose.onNodeWithTag("nav_chat").assertDoesNotExist()
        compose.onNodeWithTag("chat_input_field").assertIsDisplayed().assertTextContains("Visible while typing")
        val root = compose.onNodeWithTag("venice_app").getUnclippedBoundsInRoot()
        val composer = compose.onNodeWithTag("chat_composer").getUnclippedBoundsInRoot()
        assertTrue("Composer must touch the keyboard edge without an extra navigation gap",
            abs((root.bottom - 280.dp - composer.bottom).value) <= 2f)
        applyKeyboard(false)
        compose.onNodeWithTag("nav_chat").assertIsDisplayed()
    }

    @Test fun bothDocumentButtonsTargetMiXplorerAndImportTheReturnedFile() {
        val viewModel = VeniceViewModel()
        val file = File(compose.activity.cacheDir, "MiXplorer UTF-8 file.txt")
        val text = "Actual selected file: Nederlands, Türkçe, 🌍\nsecond line"
        file.writeText(text)
        compose.setContent { VeniceTheme { VeniceApp(viewModel) } }
        fun selectFile(tag: String) {
            compose.onNodeWithTag(tag).performClick()
            val launched = shadowOf(compose.activity).nextStartedActivityForResult
            assertNotNull("The button must launch an external picker", launched)
            assertEquals("com.mixplorer", launched.intent.`package`)
            assertEquals(Intent.ACTION_GET_CONTENT, launched.intent.action)
            compose.runOnUiThread {
                shadowOf(compose.activity).receiveResult(launched.intent, Activity.RESULT_OK,
                    Intent().setData(Uri.fromFile(file)))
            }
        }
        try {
            selectFile("attach_document_button")
            compose.waitUntil(5_000) { viewModel.uiState.value.attachedFileContent == text }
            assertEquals(file.name, viewModel.uiState.value.attachedFileName)
            compose.runOnUiThread { viewModel.setNavTab(VeniceNavTab.INTELLIGENCE) }
            compose.onNodeWithTag("intelligence_import_file").performScrollTo()
            selectFile("intelligence_import_file")
            compose.waitUntil(5_000) { viewModel.uiState.value.intelligenceInput == text }
        } finally {
            file.delete()
        }
    }
}
