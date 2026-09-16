package com.example.data.api

import com.example.data.local.TerminalTools
import com.example.data.model.ChatMessage
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChatGptToolLoopTest {
    private val model = ChatGptModel("account-model", "Account model", "", listOf("high"), "high", true)
    private fun body() = ChatGptApi.responseBody(model, "high", listOf(ChatMessage(role = "user", text = "Run pwd")),
        "Original persona and knowledge instructions", TerminalTools.definitions())
    private fun call(id: String = "call-1", args: String = "{\"command\":\"pwd\"}") = JSONObject()
        .put("type", "function_call").put("call_id", id).put("name", "exec_command").put("arguments", args)
    private fun completed(output: JSONArray) = "data: " + JSONObject().put("type", "response.completed")
        .put("response", JSONObject().put("status", "completed").put("output", output)).toString() + "\n\n"
    private fun message(text: String) = JSONObject().put("type", "message").put("role", "assistant")
        .put("content", JSONArray().put(JSONObject().put("type", "output_text").put("text", text)))

    @Test fun toolOnlyResponsePreservesReasoningAndCompletedArguments() {
        val reason = JSONObject().put("type", "reasoning").put("id", "r1").put("encrypted_content", "opaque-data").put("summary", JSONArray())
        val turn = ChatGptApi.readTurn(completed(JSONArray().put(reason).put(call())).reader().buffered()) {}
        assertEquals("", turn.text)
        assertEquals("opaque-data", turn.output.getJSONObject(0).getString("encrypted_content"))
        assertEquals("pwd", JSONObject(turn.output.getJSONObject(1).getString("arguments")).getString("command"))
    }

    @Test fun completedItemEventsAreUsedWhenFinalEventHasNoItems() {
        val event = JSONObject().put("type", "response.output_item.done").put("output_index", 0).put("item", call())
        val stream = "data: $event\n\n" + completed(JSONArray())
        assertEquals("call-1", ChatGptApi.readTurn(stream.reader().buffered()) {}.output.getJSONObject(0).getString("call_id"))
    }

    @Test fun executesCallAndReturnsMatchingOutputBeforeContinuingWithOriginalInstructions() = runBlocking {
        var requests = 0
        var executions = 0
        var saved = ""
        val turn = ChatGptToolLoop.run(body(), send = { request, _ ->
            assertEquals("Original persona and knowledge instructions", request.getString("instructions"))
            assertEquals("high", request.getJSONObject("reasoning").getString("effort"))
            assertEquals(4, request.getJSONArray("tools").length())
            requests++
            if (requests == 1) ChatGptTurn("", JSONArray().put(call())) else {
                val input = request.getJSONArray("input")
                assertEquals("function_call", input.getJSONObject(1).getString("type"))
                assertEquals("function_call_output", input.getJSONObject(2).getString("type"))
                assertEquals("call-1", input.getJSONObject(2).getString("call_id"))
                assertEquals("/root", input.getJSONObject(2).getString("output"))
                ChatGptTurn("Directory is /root", JSONArray().put(message("Directory is /root")))
            }
        }, execute = { name, args ->
            executions++
            assertEquals("exec_command", name)
            assertEquals("pwd", args.getString("command"))
            "/root"
        }, onItems = { saved = it }, onText = {})
        assertEquals(2, requests)
        assertEquals(1, executions)
        assertEquals("Directory is /root", turn.text)
        assertEquals(3, JSONArray(saved).length())
        val next = ChatGptApi.responseBody(model, "high", listOf(
            ChatMessage(role = "user", text = "Run pwd"),
            ChatMessage(role = "model", text = turn.text, responseItemsJson = saved),
            ChatMessage(role = "user", text = "Continue")
        ), "Original persona and knowledge instructions")
        assertEquals(5, next.getJSONArray("input").length())
        assertEquals("function_call_output", next.getJSONArray("input").getJSONObject(2).getString("type"))
    }

    @Test fun commandFailureIsReturnedToModelWithoutPretendingSuccess() = runBlocking<Unit> {
        var requests = 0
        ChatGptToolLoop.run(body(), send = { request, _ ->
            if (++requests == 1) ChatGptTurn("", JSONArray().put(call())) else {
                val result = JSONObject(request.getJSONArray("input").getJSONObject(2).getString("output"))
                assertEquals("Cannot start su", result.getString("error"))
                ChatGptTurn("Command failed", JSONArray().put(message("Command failed")))
            }
        }, execute = { _, _ -> throw IOException("Cannot start su") }, onItems = {}, onText = {})
    }

    @Test fun duplicateCallIdDoesNotRunTheCommandAgain() = runBlocking {
        var requests = 0
        var executions = 0
        ChatGptToolLoop.run(body(), send = { _, _ ->
            if (++requests <= 2) ChatGptTurn("", JSONArray().put(call()))
            else ChatGptTurn("Done", JSONArray().put(message("Done")))
        }, execute = { _, _ -> executions++; "result" }, onItems = {}, onText = {})
        assertEquals(1, executions)
    }

    @Test fun disconnectedStreamNeverExecutesPartialArguments() {
        val stream = "data: {\"type\":\"response.output_item.done\",\"output_index\":0,\"item\":${call()}}\n\n"
        assertThrows(IOException::class.java) { ChatGptApi.readTurn(stream.reader().buffered()) {} }
    }

    @Test fun laterNetworkFailureKeepsCompletedCommandResults() = runBlocking {
        var executions = 0
        var requests = 0
        var saved = ""
        try {
            ChatGptToolLoop.run(body(), send = { _, _ ->
                if (++requests == 1) ChatGptTurn("", JSONArray().put(call()))
                else throw IOException("Connection lost")
            }, execute = { _, _ -> executions++; "completed command output" },
                onItems = { saved = it }, onText = {})
            fail("Expected the network failure")
        } catch (e: IOException) {
            assertEquals("Connection lost", e.message)
        }
        assertEquals(1, executions)
        val items = JSONArray(saved)
        assertEquals("function_call_output", items.getJSONObject(1).getString("type"))
        assertEquals("completed command output", items.getJSONObject(1).getString("output"))
    }
}
