package com.example.data.api

import com.example.data.auth.OpenAIOAuthSession
import com.example.data.model.ChatMessage
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ChatGptModel(
    val id: String,
    val displayName: String,
    val description: String,
    val reasoningLevels: List<String>,
    val defaultReasoning: String?,
    val acceptsImages: Boolean
)

class ChatGptHttpException(val status: Int, message: String) : IOException(message)

/** ChatGPT OAuth uses the Codex backend, not the API-key endpoint. */
object ChatGptApi {
    private const val BASE_URL = "https://chatgpt.com/backend-api/codex"
    // Versioned model catalogue contract verified against openai/codex rust-v0.154.0.
    private const val CLIENT_VERSION = "0.154.0"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.MINUTES)
        .build()

    private fun request(session: OpenAIOAuthSession, path: String): Request.Builder =
        Request.Builder().url("$BASE_URL/$path")
            .header("Authorization", "Bearer ${session.accessToken}")
            .header("ChatGPT-Account-ID", session.accountId)
            .header("originator", "venice_android")

    suspend fun models(session: OpenAIOAuthSession): List<ChatGptModel> =
        execute(request(session, "models?client_version=$CLIENT_VERSION").build()) { response ->
            parseModels(response.body?.string() ?: throw IOException("Empty model catalogue."))
        }

    internal fun parseModels(raw: String): List<ChatGptModel> {
        val models = JSONObject(raw).getJSONArray("models")
        return (0 until models.length()).map { models.getJSONObject(it) }
            .filter { it.optString("visibility") == "list" }
            .sortedBy { it.optInt("priority", Int.MAX_VALUE) }
            .map { model ->
                val levels = model.optJSONArray("supported_reasoning_levels") ?: JSONArray()
                val efforts = (0 until levels.length()).map {
                    levels.getJSONObject(it).getString("effort")
                }.filter { it.isNotBlank() }.distinct()
                val modalities = model.optJSONArray("input_modalities")
                ChatGptModel(
                    id = model.getString("slug"),
                    displayName = model.optString("display_name").ifBlank { model.getString("slug") },
                    description = model.optString("description"),
                    reasoningLevels = efforts,
                    defaultReasoning = model.optString("default_reasoning_level").takeIf { it in efforts },
                    acceptsImages = modalities == null || (0 until modalities.length()).any {
                        modalities.optString(it) == "image"
                    }
                )
            }.filter { it.id.isNotBlank() }.distinctBy { it.id }
            .also { if (it.isEmpty()) throw IOException("ChatGPT returned no selectable models for this account.") }
    }

    internal fun responseBody(
        model: ChatGptModel,
        effort: String?,
        history: List<ChatMessage>,
        instructions: String,
        tools: JSONArray = JSONArray()
    ): JSONObject {
        require(effort == null || effort in model.reasoningLevels) { "Unsupported reasoning level." }
        // Local welcome/reset notices precede the first user message; they are not model replies.
        val messages = history.dropWhile { it.role != "user" }
        require(model.acceptsImages || messages.none { it.imageBase64 != null }) {
            "This model does not accept images. Select an image-capable model or start a new chat."
        }
        val input = JSONArray()
        messages.forEach { message ->
            val user = message.role == "user"
            if (!user && message.responseItemsJson != null) {
                val items = JSONArray(message.responseItemsJson)
                for (i in 0 until items.length()) input.put(items.getJSONObject(i))
                return@forEach
            }
            val content = JSONArray()
            if (message.text.isNotEmpty()) content.put(JSONObject()
                .put("type", if (user) "input_text" else "output_text").put("text", message.text))
            if (user && message.imageBase64 != null) content.put(JSONObject()
                .put("type", "input_image").put("image_url", "data:image/jpeg;base64,${message.imageBase64}"))
            input.put(JSONObject().put("type", "message")
                .put("role", if (user) "user" else "assistant").put("content", content))
        }
        return JSONObject().put("model", model.id).put("instructions", instructions)
            .put("input", input).put("tools", tools).put("tool_choice", "auto")
            .put("parallel_tool_calls", false).put("store", false).put("stream", true)
            .put("include", JSONArray().put("reasoning.encrypted_content"))
            .apply { if (effort != null) put("reasoning", JSONObject().put("effort", effort)) }
    }

    internal suspend fun streamTurn(
        session: OpenAIOAuthSession,
        body: JSONObject,
        onText: (String) -> Unit
    ): ChatGptTurn {
        val request = request(session, "responses").header("Accept", "text/event-stream")
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        return execute(request) { response ->
            val reader = response.body?.charStream()?.buffered()
                ?: throw IOException("ChatGPT returned an empty response.")
            readTurn(reader, onText)
        }
    }

    internal fun readStream(reader: BufferedReader, onText: (String) -> Unit): String {
        val turn = readTurn(reader, onText)
        if (turn.text.isBlank()) throw IOException("ChatGPT completed without a text reply.")
        return turn.text
    }

    internal fun readTurn(reader: BufferedReader, onText: (String) -> Unit): ChatGptTurn {
        val text = StringBuilder()
        val data = StringBuilder()
        var completed = false
        var output = JSONArray()
        val doneItems = sortedMapOf<Int, JSONObject>()
        var lastUpdate = 0L
        fun dispatch() {
            if (data.isEmpty()) return
            val raw = data.toString()
            data.setLength(0)
            if (raw == "[DONE]") return
            val event = JSONObject(raw)
            when (event.optString("type")) {
                "response.output_item.done" -> {
                    doneItems[event.getInt("output_index")] = event.getJSONObject("item")
                }
                "response.output_text.delta" -> {
                    text.append(event.getString("delta"))
                    val now = System.nanoTime()
                    if (now - lastUpdate >= 50_000_000L) {
                        onText(text.toString())
                        lastUpdate = now
                    }
                }
                "response.completed" -> {
                    val response = event.getJSONObject("response")
                    if (response.optString("status") != "completed") throw IOException("ChatGPT did not complete the response.")
                    output = response.optJSONArray("output") ?: JSONArray()
                    if (output.length() == 0) doneItems.values.forEach { output.put(it) }
                    val finalText = StringBuilder()
                    for (i in 0 until output.length()) {
                        val content = output.getJSONObject(i).optJSONArray("content") ?: continue
                        for (j in 0 until content.length()) {
                            val part = content.getJSONObject(j)
                            when (part.optString("type")) {
                                "output_text" -> finalText.append(part.optString("text"))
                                "refusal" -> finalText.append(part.optString("refusal"))
                            }
                        }
                    }
                    if (finalText.isNotEmpty()) { text.setLength(0); text.append(finalText) }
                    completed = true
                }
                "response.failed", "response.incomplete", "error" -> {
                    val response = event.optJSONObject("response")
                    val error = event.optJSONObject("error") ?: response?.optJSONObject("error")
                    throw IOException(error?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: event.optString("message").ifBlank { "ChatGPT response failed or was incomplete." })
                }
            }
        }
        while (!completed) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) dispatch()
            else if (line.startsWith("data:")) {
                if (data.isNotEmpty()) data.append('\n')
                data.append(line.substring(5).removePrefix(" "))
            }
        }
        if (!completed) dispatch()
        if (!completed) throw IOException("ChatGPT connection ended before the reply completed. Please retry.")
        val hasCalls = (0 until output.length()).any { output.getJSONObject(it).optString("type") == "function_call" }
        if (text.isBlank() && !hasCalls) throw IOException("ChatGPT completed without text or tool calls.")
        if (output.length() == 0 && text.isNotBlank()) {
            output.put(JSONObject().put("type", "message").put("role", "assistant")
                .put("content", JSONArray().put(JSONObject().put("type", "output_text").put("text", text.toString()))))
        }
        return ChatGptTurn(text.toString().also(onText), output)
    }

    private suspend fun <T> execute(request: Request, parse: (Response) -> T): T =
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val result = response.use {
                            if (!it.isSuccessful) {
                                val detail = try {
                                    JSONObject(it.body?.string().orEmpty()).optJSONObject("error")
                                        ?.optString("message").orEmpty()
                                } catch (_: Exception) { "" }
                                throw ChatGptHttpException(it.code,
                                    "ChatGPT HTTP ${it.code}" + if (detail.isBlank()) ". Please retry or reconnect your account." else ": $detail")
                            }
                            parse(it)
                        }
                        if (continuation.isActive) continuation.resume(result)
                    } catch (e: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }
            })
        }
}
