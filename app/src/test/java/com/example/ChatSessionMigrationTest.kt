package com.example

import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import org.junit.Assert.*
import org.junit.Test

class ChatSessionMigrationTest {
    @Test fun oldConversationKeepsHistoryAndIdentityWhenContinuedWithChatGpt() {
        val original = ChatSession(
            id = "saved-conversation", title = "Kernel notes", personaId = "nethunter_copilot",
            selectedModel = "previous-provider-model", useChatGpt = false, highThinkingEnabled = true,
            createdAt = 10L, updatedAt = 20L,
            messages = listOf(
                ChatMessage(role = "user", text = "Inspect this", imageBase64 = "image", attachedFileName = "log.txt"),
                ChatMessage(role = "model", text = "Existing answer", modelUsed = "previous-provider-model")
            )
        )
        val migrated = original.forChatGpt("account-model", "high")
        assertEquals(original.copy(useChatGpt = true, selectedModel = "account-model",
            reasoningEffort = "high", highThinkingEnabled = false), migrated)
        assertSame(original.messages, migrated.messages)
    }

    @Test fun existingChatGptConversationKeepsItsOwnSelection() {
        val original = ChatSession(selectedModel = "saved-account-model", reasoningEffort = "low")
        assertSame(original, original.forChatGpt("different-model", "high"))
    }

    @Test fun noSelectedAccountModelLeavesOldHistoryAvailableWithoutInventingAModel() {
        val original = ChatSession(useChatGpt = false, selectedModel = "previous-provider-model",
            messages = listOf(ChatMessage(role = "user", text = "Keep this history")))
        val migrated = original.forChatGpt(null, null)
        assertTrue(migrated.useChatGpt)
        assertEquals("", migrated.selectedModel)
        assertNull(migrated.reasoningEffort)
        assertSame(original.messages, migrated.messages)
    }
}
