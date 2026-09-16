package com.example.data.api

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONArray
import org.json.JSONObject

internal data class ChatGptTurn(val text: String, val output: JSONArray)

/** Replays complete Responses items, including reasoning and matching tool results. */
internal object ChatGptToolLoop {
    suspend fun run(
        body: JSONObject,
        send: suspend (JSONObject, (String) -> Unit) -> ChatGptTurn,
        execute: suspend (String, JSONObject) -> String,
        onItems: (String) -> Unit,
        onText: (String) -> Unit,
    ): ChatGptTurn {
        val input = body.getJSONArray("input")
        val produced = JSONArray()
        val completedCalls = mutableMapOf<String, Pair<String, String>>()
        val displayed = StringBuilder()
        while (true) {
            currentCoroutineContext().ensureActive()
            val prefix = displayed.toString()
            // Authentication retry surrounds this HTTP exchange only, never executed commands.
            val turn = send(body) { partial -> onText(prefix + partial) }
            val calls = mutableListOf<JSONObject>()
            for (i in 0 until turn.output.length()) {
                val item = turn.output.getJSONObject(i)
                input.put(item)
                produced.put(item)
                if (item.optString("type") == "function_call") calls.add(item)
            }
            if (turn.text.isNotBlank()) {
                displayed.append(turn.text)
                if (calls.isNotEmpty()) displayed.append("\n\n")
            }
            if (calls.isEmpty()) {
                onItems(produced.toString())
                if (displayed.isBlank()) throw IOException("ChatGPT completed without text or a terminal tool call.")
                return ChatGptTurn(displayed.toString().trimEnd().also(onText), produced)
            }
            for (call in calls) {
                currentCoroutineContext().ensureActive()
                val id = call.getString("call_id")
                val name = call.getString("name")
                val arguments = call.getString("arguments")
                val signature = "$name\n$arguments"
                val prior = completedCalls[id]
                val result = if (prior != null) {
                    check(prior.first == signature) { "ChatGPT reused a tool call ID with different arguments." }
                    prior.second
                } else {
                    try {
                        execute(name, JSONObject(arguments))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        JSONObject().put("error", e.message ?: e.javaClass.simpleName).toString()
                    }.also { completedCalls[id] = signature to it }
                }
                val output = JSONObject().put("type", "function_call_output").put("call_id", id).put("output", result)
                input.put(output)
                produced.put(output)
            }
            // Save only complete call/result groups, so future turns can replay them.
            onItems(produced.toString())
        }
    }
}
