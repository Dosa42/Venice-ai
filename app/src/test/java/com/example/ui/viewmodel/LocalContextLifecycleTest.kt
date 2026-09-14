package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.model.LocalContextAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LocalContextLifecycleTest {
    private lateinit var viewModel: VeniceViewModel
    private val snapshot = LocalContextAttachment(
        path = "/root/notes.txt", kind = "text", capturedAt = 123L, content = "Selected text"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel = VeniceViewModel()
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `selecting and removing an attachment never sends it`() {
        val messages = viewModel.uiState.value.currentSession.messages
        viewModel.attachLocalContext(snapshot)
        assertEquals(snapshot, viewModel.uiState.value.attachedLocalContext)
        assertEquals(messages, viewModel.uiState.value.currentSession.messages)
        assertFalse(viewModel.uiState.value.isGeneratingChat)

        viewModel.removeLocalContext()
        assertNull(viewModel.uiState.value.attachedLocalContext)
        assertEquals(messages, viewModel.uiState.value.currentSession.messages)
    }

    @Test
    fun `missing key restores attachment-only draft without a fabricated assistant reply`() = runTest {
        assumeMissingKey()
        val originalMessages = viewModel.uiState.value.currentSession.messages
        viewModel.attachLocalContext(snapshot)
        viewModel.sendChatMessage()
        assertTrue(viewModel.uiState.value.isGeneratingChat)
        assertNull(viewModel.uiState.value.attachedLocalContext)
        assertEquals(snapshot, viewModel.uiState.value.currentSession.messages.last().localContext)

        runCurrent()
        val failed = awaitChatCompletion()
        assertEquals(originalMessages, failed.currentSession.messages)
        assertEquals(snapshot, failed.attachedLocalContext)
        assertEquals("", failed.chatInputText)
        assertTrue(failed.chatErrorMessage.orEmpty().contains("API key is not configured"))
    }

    @Test
    fun `missing key cannot attach the old snapshot to a changed draft`() = runTest {
        assumeMissingKey()
        viewModel.setChatInput("Read the selected file")
        viewModel.attachLocalContext(snapshot)
        viewModel.sendChatMessage()
        // Main is queued, so these user edits deterministically precede the API failure.
        viewModel.setChatInput("A different question")
        viewModel.attachLocalContext(snapshot.copy(path = "/root/new.txt", content = "New text"))
        viewModel.removeLocalContext()

        runCurrent()
        val failed = awaitChatCompletion()
        assertEquals("A different question", failed.chatInputText)
        assertNull(failed.attachedLocalContext)
        assertTrue(failed.chatErrorMessage.orEmpty().contains("API key is not configured"))
        assertTrue(failed.currentSession.messages.none { it.localContext != null })
    }

    @Test
    fun `new and switched sessions clear local text and image drafts and cancel queued sends`() = runTest {
        val originalSession = viewModel.uiState.value.currentSession
        viewModel.setChatInput("Old draft")
        viewModel.attachImage(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
        viewModel.attachLocalContext(snapshot)
        viewModel.sendChatMessage()
        viewModel.startNewSession()
        runCurrent()

        val fresh = viewModel.uiState.value
        assertNotEquals(originalSession.id, fresh.currentSession.id)
        assertDraftCleared(fresh)
        assertFalse(fresh.isGeneratingChat)
        assertNull(fresh.chatErrorMessage)
        assertTrue(fresh.currentSession.messages.none { it.localContext != null })

        viewModel.setChatInput("Draft before switching")
        viewModel.attachImage(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
        viewModel.attachLocalContext(snapshot)
        viewModel.switchSession(originalSession)
        runCurrent()
        val switched = viewModel.uiState.value
        assertEquals(originalSession.id, switched.currentSession.id)
        assertDraftCleared(switched)
        assertEquals(originalSession.messages, switched.currentSession.messages)
    }

    private fun assumeMissingKey() {
        assumeTrue(
            "This case requires an unconfigured Gemini build and must not send a live request.",
            BuildConfig.GEMINI_API_KEY.isBlank() || BuildConfig.GEMINI_API_KEY == "MY_GEMINI_API_KEY"
        )
    }

    private suspend fun awaitChatCompletion(): VeniceUiState = withContext(Dispatchers.Default) {
        // The actual missing-key check runs on Dispatchers.IO, outside virtual test time.
        withTimeout(10_000) { viewModel.uiState.first { !it.isGeneratingChat } }
    }

    private fun assertDraftCleared(state: VeniceUiState) {
        assertEquals("", state.chatInputText)
        assertNull(state.attachedLocalContext)
        assertNull(state.attachedImageBase64)
        assertNull(state.attachedImageBitmap)
        assertNull(state.localFiles.directory)
        assertNull(state.localFiles.file)
    }
}
