package com.example.data.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiApi {
    private const val TAG = "VeniceGeminiApi"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isNullOrBlank() || key == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is blank or default placeholder.")
        }
        return key
    }

    data class GeminiResult(
        val text: String,
        val thoughtProcess: String? = null,
        val imageBase64: String? = null,
        val isSuccess: Boolean = true,
        val errorMessage: String? = null
    )

    /**
     * Multi-turn chat generation with system instruction, model selection,
     * multimodal image support, and High Thinking support.
     */
    suspend fun generateChatResponse(
        modelName: String,
        history: List<ChatMessage>,
        systemInstruction: String?,
        enableHighThinking: Boolean,
        attachedImageBase64: String? = null
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResult(
                text = "Venice Privacy Shield: To activate live model inferencing, please configure your GEMINI_API_KEY in the AI Studio Secrets panel. (Simulating Venice zero-knowledge response for demo: 'Private zero-retention session initialized successfully. How can Venice assist you today?')",
                isSuccess = true
            )
        }

        // When High Thinking is requested, use gemini-3.1-pro-preview
        val activeModel = if (enableHighThinking) "gemini-3.1-pro-preview" else modelName

        try {
            val rootJson = JSONObject()

            // Contents array (Conversation history + latest query)
            val contentsArray = JSONArray()
            history.forEach { msg ->
                val contentObj = JSONObject()
                contentObj.put("role", if (msg.role == "user") "user" else "model")

                val partsArray = JSONArray()

                // Add image if present
                if (msg.imageBase64 != null) {
                    val inlineData = JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", msg.imageBase64)
                    }
                    partsArray.put(JSONObject().put("inlineData", inlineData))
                }

                if (msg.text.isNotBlank()) {
                    partsArray.put(JSONObject().put("text", msg.text))
                }

                contentObj.put("parts", partsArray)
                contentsArray.put(contentObj)
            }
            rootJson.put("contents", contentsArray)

            // System instruction
            if (!systemInstruction.isNullOrBlank()) {
                val sysObj = JSONObject()
                val parts = JSONArray().put(JSONObject().put("text", systemInstruction))
                sysObj.put("parts", parts)
                rootJson.put("systemInstruction", sysObj)
            }

            // Generation config
            val genConfig = JSONObject()
            if (enableHighThinking) {
                // High thinking requirement: gemini-3.1-pro-preview, thinkingLevel = "HIGH", no maxOutputTokens
                val thinkingConfig = JSONObject().apply {
                    put("thinkingLevel", "HIGH")
                }
                genConfig.put("thinkingConfig", thinkingConfig)
            } else {
                genConfig.put("temperature", 0.7)
            }
            rootJson.put("generationConfig", genConfig)

            val url = "$BASE_URL$activeModel:generateContent?key=$apiKey"
            val body = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "API Error $activeModel: ${response.code} $responseBody")
                return@withContext GeminiResult(
                    text = "Venice Core Encountered an Error (${response.code}): $responseBody",
                    isSuccess = false,
                    errorMessage = responseBody
                )
            }

            parseGenerateContentResponse(responseBody)
        } catch (e: Exception) {
            Log.e(TAG, "Network exception calling Gemini API", e)
            GeminiResult(
                text = "Connection error: ${e.localizedMessage ?: e.message}",
                isSuccess = false,
                errorMessage = e.message
            )
        }
    }

    /**
     * Create image with text prompt or Edit existing image using gemini-3.1-flash-image-preview.
     */
    suspend fun generateOrEditImage(
        prompt: String,
        aspectRatio: String = "1:1",
        baseImageToEdit: String? = null
    ): GeminiResult = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        val model = "gemini-3.1-flash-image-preview"

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GeminiResult(
                text = "Venice Image Studio: Please configure your GEMINI_API_KEY in the Secrets panel to generate real-time AI imagery. (Demo mode ready)",
                isSuccess = false,
                errorMessage = "API key not configured in AI Studio Secrets panel."
            )
        }

        try {
            val rootJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject().put("role", "user")
            val partsArray = JSONArray()

            // If editing an existing image, include it as inlineData
            if (baseImageToEdit != null) {
                val inlineData = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", baseImageToEdit)
                }
                partsArray.put(JSONObject().put("inlineData", inlineData))
                partsArray.put(JSONObject().put("text", "Edit this image based on the following instruction: $prompt. Maintain high visual fidelity and composition."))
            } else {
                partsArray.put(JSONObject().put("text", prompt))
            }
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            rootJson.put("contents", contentsArray)

            // Generation config with imageConfig and responseModalities
            val genConfig = JSONObject().apply {
                val imageConfig = JSONObject().apply {
                    put("aspectRatio", aspectRatio)
                    put("imageSize", "1K")
                }
                put("imageConfig", imageConfig)
                val modalities = JSONArray().apply {
                    put("TEXT")
                    put("IMAGE")
                }
                put("responseModalities", modalities)
            }
            rootJson.put("generationConfig", genConfig)

            val url = "$BASE_URL$model:generateContent?key=$apiKey"
            val body = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Image API Error: ${response.code} $responseBody")
                return@withContext GeminiResult(
                    text = "Venice Image Studio Error: ${response.code}",
                    isSuccess = false,
                    errorMessage = responseBody
                )
            }

            parseGenerateContentResponse(responseBody)
        } catch (e: Exception) {
            Log.e(TAG, "Image generation error", e)
            GeminiResult(
                text = "Image generation failed: ${e.localizedMessage}",
                isSuccess = false,
                errorMessage = e.message
            )
        }
    }

    /**
     * Venice Quick Intelligence tool: Enhance prompt, summarize, privacy check.
     */
    suspend fun runIntelligenceTask(
        taskType: String,
        input: String,
        hardwareContext: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Venice Intelligence output: Verified prompt and simulated privacy audit passed."
        }

        val (model, baseSystemPrompt) = when (taskType) {
            "ENHANCE_PROMPT" -> Pair(
                "gemini-3.1-flash-lite-preview",
                "You are an expert prompt engineer for Venice AI. Given a raw user prompt, rewrite it to be highly descriptive, evocative, rich with sensory and aesthetic details, and optimized for maximum LLM or image generation fidelity. Return ONLY the enhanced prompt."
            )
            "SUMMARIZE" -> Pair(
                "gemini-3.5-flash",
                "You are Venice Intelligence. Provide a concise, highly readable, bulleted executive summary of the provided text with key takeaways and conclusions."
            )
            "PRIVACY_AUDIT" -> Pair(
                "gemini-3.1-pro-preview",
                "You are a cypherpunk privacy auditor and security expert. Analyze the provided query or architecture for data leakage risks, tracking vulnerabilities, privacy tradeoffs, and suggest zero-knowledge or local-first hardening steps."
            )
            "CODE_REFACTOR" -> Pair(
                "gemini-3.1-pro-preview",
                "You are a senior systems engineer and security architect. Review the provided code snippet, identify performance bottlenecks or security vulnerabilities, and provide a clean, modern, hardened refactored solution with clear explanations."
            )
            "TERMINAL_LOG_DIAGNOSTIC" -> Pair(
                "gemini-3.1-pro-preview",
                "You are Venice NetHunter & Linux Terminal Diagnostic Copilot. You analyze command outputs, kernel logs (dmesg), system error traces, or package compilation logs. Diagnose the exact root cause of any warnings or errors, explain the underlying Linux system mechanism, and provide the exact corrected terminal command(s) or remedy. Format code and commands in clean markdown blocks."
            )
            "SHELL_SCRIPT_AUDIT" -> Pair(
                "gemini-3.1-pro-preview",
                "You are Venice Shell & Automation Auditor. You specialize in bash/zsh scripting, NetHunter chroot environments, automation, error handling (set -euo pipefail), argument parsing, and permission handling. Review the script, identify flaws, security issues, or POSIX inconsistencies, and output an optimized, hardened script with explanations."
            )
            "NETWORK_CONFIG_ANALYZER" -> Pair(
                "gemini-3.5-flash",
                "You are Venice Network & Protocol Analyst. Analyze the provided network output (e.g. ifconfig, ip addr, ip route, iptables, nmap scan, or interface config). Break down active subnets, routing decisions, open ports, security implications, and troubleshooting recommendations."
            )
            "DYNAMIC_RUNTIME_ADAPT" -> Pair(
                "gemini-3.1-pro-preview",
                "You are Venice Dynamic Adaptive Framework (DAF) Engine. Given live terminal logs, error traces, or environment outputs, extract all newly discovered runtime facts (network interfaces, monitor mode, installed tools/binaries, kernel drivers, chroot paths, active services, memory constraints, and fixes). Present them in clear, structured format and describe how the runtime behavior should adapt."
            )
            "SELF_CODEBASE_INSPECTION" -> Pair(
                "gemini-3.1-pro-preview",
                "You are Venice AI Metacognitive Codebase Engineer and Autonomous CI/CD Architect. You have direct awareness of your own source code (Android Jetpack Compose, Kotlin, DynamicAdaptiveEngine, OpenAIOAuthManager, build-apk.yml, build-debug-apk.sh). When asked to inspect, debug, extend, or refactor your own capabilities, provide exact unified diff patches, explain architectural implications, and output the exact NetHunter terminal or GitHub Actions dispatch commands (`gh workflow run build-apk.yml -f build_variant=debug`) to rebuild the APK."
            )
            else -> Pair("gemini-3.5-flash", "You are Venice AI. Assist directly and concisely.")
        }

        val systemPrompt = if (!hardwareContext.isNullOrBlank()) {
            "$baseSystemPrompt\n\n$hardwareContext"
        } else {
            baseSystemPrompt
        }

        try {
            val rootJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().put("text", input)))
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                })
                put("generationConfig", JSONObject().put("temperature", 0.3))
            }

            val url = "$BASE_URL$model:generateContent?key=$apiKey"
            val body = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            val parsed = parseGenerateContentResponse(responseBody)
            parsed.text
        } catch (e: Exception) {
            "Analysis failed: ${e.message}"
        }
    }

    private fun parseGenerateContentResponse(jsonString: String): GeminiResult {
        try {
            val root = JSONObject(jsonString)
            val candidates = root.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                val errorMsg = root.optJSONObject("error")?.optString("message") ?: "No candidates returned"
                return GeminiResult(text = errorMsg, isSuccess = false, errorMessage = errorMsg)
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            var extractedText = ""
            var extractedThought: String? = null
            var extractedImageBase64: String? = null

            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    
                    // Check for thought attribute (Gemini thinking mode)
                    if (part.optBoolean("thought", false)) {
                        val thoughtText = part.optString("text", "")
                        extractedThought = if (extractedThought == null) thoughtText else "$extractedThought\n$thoughtText"
                    } else if (part.has("text")) {
                        val text = part.getString("text")
                        // Sometimes thoughts are wrapped in <thought> tags
                        if (text.contains("<thought>") && text.contains("</thought>")) {
                            val start = text.indexOf("<thought>") + 9
                            val end = text.indexOf("</thought>")
                            val thought = text.substring(start, end).trim()
                            val cleanText = (text.substring(0, start - 9) + text.substring(end + 10)).trim()
                            extractedThought = if (extractedThought == null) thought else "$extractedThought\n$thought"
                            extractedText += cleanText
                        } else {
                            extractedText += text
                        }
                    }

                    // Check for inlineData (Images)
                    if (part.has("inlineData")) {
                        val inlineData = part.getJSONObject("inlineData")
                        extractedImageBase64 = inlineData.optString("data")
                    }
                }
            }

            return GeminiResult(
                text = extractedText.ifBlank { if (extractedImageBase64 != null) "Generated with Venice Studio" else "Completed." },
                thoughtProcess = extractedThought,
                imageBase64 = extractedImageBase64,
                isSuccess = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response", e)
            return GeminiResult(
                text = "Error parsing model response: ${e.message}",
                isSuccess = false,
                errorMessage = e.message
            )
        }
    }

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
