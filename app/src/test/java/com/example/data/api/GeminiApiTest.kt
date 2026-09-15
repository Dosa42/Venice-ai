package com.example.data.api

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GeminiApiTest {
    @Test fun absentKeyIsAnErrorWithNoInventedOutput() {
        listOf("", " ", "MY_GEMINI_API_KEY").forEach { key ->
            val result = requireNotNull(GeminiApi.configurationFailure(key))
            assertFalse(result.isSuccess)
            assertEquals("", result.text)
            assertFalse(result.errorMessage.isNullOrBlank())
        }
        assertNull(GeminiApi.configurationFailure("configured-key"))
    }

    @Test fun emptyAndThoughtOnlyCandidatesAreNotAnswers() {
        listOf("[]", "[{\"thought\":true,\"text\":\"Reasoning only\"}]").forEach { parts ->
            val result = GeminiApi.parseGenerateContentResponse(
                """{"candidates":[{"finishReason":"STOP","content":{"parts":$parts}}]}""")
            assertFalse(result.isSuccess)
            assertEquals("", result.text)
        }
    }

    @Test fun incompleteOutputIsNotSuccessfulHistory() {
        val result = GeminiApi.parseGenerateContentResponse(
            """{"candidates":[{"finishReason":"MAX_TOKENS","content":{"parts":[{"text":"Partial answer"}]}}]}""")
        assertFalse(result.isSuccess)
        assertEquals("", result.text)
        assertTrue(result.errorMessage!!.contains("MAX_TOKENS"))
    }

    @Test fun completedTextIsPreservedExactly() {
        val result = GeminiApi.parseGenerateContentResponse(
            """{"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"Actual answer"}]}}]}""")
        assertTrue(result.isSuccess)
        assertEquals("Actual answer", result.text)
    }

    @Test fun imageRequiresImageAndTextRequiresText() {
        val text = """{"candidates":[{"finishReason":"STOP","content":{"parts":[{"text":"No image"}]}}]}"""
        val image = """{"candidates":[{"finishReason":"STOP","content":{"parts":[{"inlineData":{"mimeType":"image/png","data":"image-bytes"}}]}}]}"""
        assertFalse(GeminiApi.parseGenerateContentResponse(text, expectImage = true).isSuccess)
        assertFalse(GeminiApi.parseGenerateContentResponse(image).isSuccess)
        val result = GeminiApi.parseGenerateContentResponse(image, expectImage = true)
        assertTrue(result.isSuccess)
        assertEquals("image-bytes", result.imageBase64)
        assertEquals("", result.text)
    }
}
