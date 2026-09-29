package com.example.data.config

import android.os.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

data class VeniceVaultConfig(
    val apiKey: String = "",
    val activeModel: String = "gemini-3.5-flash",
    val systemPrompt: String = "You are Venice AI, a private, uncensored, objective, and intellectually rigorous AI assistant. Give direct, factual, nuanced answers without moralizing, preachy lecturing, or unsolicited disclaimers. Always respect user autonomy and privacy.",
    val temperature: Double = 0.3
)

class SharedConfigManager {
    private val configFile: File get() {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(File(downloadDir, "ObsidianVault/.auth"), "vault_auth_config.json")
    }

    private val _config = MutableStateFlow(VeniceVaultConfig())
    val config: StateFlow<VeniceVaultConfig> = _config.asStateFlow()

    init {
        loadConfig()
    }

    fun loadConfig() {
        if (!configFile.exists()) return
        try {
            val json = JSONObject(configFile.readText())
            _config.value = VeniceVaultConfig(
                apiKey = json.optString("apiKey", ""),
                activeModel = json.optString("activeModel", "gemini-3.5-flash"),
                systemPrompt = json.optString("systemPrompt", _config.value.systemPrompt),
                temperature = json.optDouble("temperature", 0.3)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
