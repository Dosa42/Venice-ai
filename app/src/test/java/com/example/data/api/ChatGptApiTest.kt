package com.example.data.api

import com.example.data.model.ChatMessage
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChatGptApiTest {
    private val model = ChatGptModel("account-model", "Account Model", "", listOf("low", "high"), "low", true)

    @Test fun catalogueUsesVisibleAccountModelsAndSupportedEfforts() {
        val models = ChatGptApi.parseModels("""{"models":[
          {"slug":"hidden","visibility":"hidden","priority":0},
          {"slug":"second","display_name":"Second","visibility":"list","priority":2,
           "supported_in_api":false,"input_modalities":["text"],
           "supported_reasoning_levels":[{"effort":"high","description":"More effort"}],"default_reasoning_level":"high"},
          {"slug":"first","visibility":"list","priority":1}
        ]}""")
        assertEquals(listOf("first", "second"), models.map { it.id })
        assertEquals(listOf("high"), models[1].reasoningLevels)
        assertEquals("high", models[1].defaultReasoning)
        assertFalse(models[1].acceptsImages)
    }

    @Test fun requestUsesResponsesHistoryImagesAndSelectedReasoning() {
        val body = ChatGptApi.responseBody(model, "high", listOf(
            ChatMessage(role = "model", text = "Local welcome"),
            ChatMessage(role = "user", text = "Read this", imageBase64 = "image-data"),
            ChatMessage(role = "model", text = "Previous answer"),
            ChatMessage(role = "user", text = "Continue")
        ), "Persona instructions")
        assertEquals("account-model", body.getString("model"))
        assertEquals("high", body.getJSONObject("reasoning").getString("effort"))
        assertFalse(body.getBoolean("store"))
        assertTrue(body.getBoolean("stream"))
        assertEquals("Persona instructions", body.getString("instructions"))
        val input = body.getJSONArray("input")
        assertEquals(3, input.length())
        assertEquals("data:image/jpeg;base64,image-data", input.getJSONObject(0)
            .getJSONArray("content").getJSONObject(1).getString("image_url"))
        assertEquals("assistant", input.getJSONObject(1).getString("role"))
        assertEquals("output_text", input.getJSONObject(1).getJSONArray("content").getJSONObject(0).getString("type"))
    }

    @Test fun unsupportedReasoningAndImagesAreRejectedBeforeSending() {
        assertThrows(IllegalArgumentException::class.java) {
            ChatGptApi.responseBody(model, "invented", emptyList(), "")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ChatGptApi.responseBody(model.copy(acceptsImages = false), null,
                listOf(ChatMessage(role = "user", text = "", imageBase64 = "image")), "")
        }
        assertFalse(ChatGptApi.responseBody(model, null, emptyList(), "").has("reasoning"))
    }

    @Test fun streamHandlesCommentsCrLfUnicodeAndCompletionWithoutTrailingBlank() {
        val events = ": heartbeat\r\n\r\ndata: {\"type\":\"response.output_text.delta\",\"delta\":\"Hello 🌍\"}\r\n\r\n" +
            "data: {\"type\":\"response.completed\",\"response\":{\"status\":\"completed\",\"output\":[]}}"
        val updates = mutableListOf<String>()
        assertEquals("Hello 🌍", ChatGptApi.readStream(events.reader().buffered()) { updates.add(it) })
        assertEquals("Hello 🌍", updates.last())
    }

    @Test fun completedOutputCanSupplyTextWithoutDeltas() {
        val events = """data: {"type":"response.completed","response":{"status":"completed","output":[{"type":"message","content":[{"type":"output_text","text":"Complete answer"}]}]}}

"""
        assertEquals("Complete answer", ChatGptApi.readStream(events.reader().buffered()) {})
    }

    @Test fun truncatedFailedAndEmptyStreamsNeverReportSuccess() {
        val partial = "data: {\"type\":\"response.output_text.delta\",\"delta\":\"Partial\"}\n\n"
        val endings = listOf("", "data: [DONE]\n\n",
            "data: {\"type\":\"response.failed\",\"response\":{\"error\":{\"message\":\"Quota exhausted\"}}}\n\n",
            "data: {\"type\":\"response.incomplete\",\"response\":{}}\n\n")
        endings.forEach { ending ->
            assertThrows(IOException::class.java) { ChatGptApi.readStream((partial + ending).reader().buffered()) {} }
        }
        assertThrows(IOException::class.java) {
            ChatGptApi.readStream("data: {\"type\":\"response.completed\",\"response\":{\"status\":\"completed\"}}\n\n".reader().buffered()) {}
        }
    }
}
