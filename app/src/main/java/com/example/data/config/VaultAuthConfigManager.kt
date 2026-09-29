package com.example.data.config

import android.content.Context
import com.example.data.filesystem.VaultFileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class VaultAuthConfig(
    val provider: String,
    val endpoint: String,
    val apiKey: String,
    val activeModel: String,
    val fallbackModel: String,
    val personaName: String,
    val personaTitle: String,
    val systemPrompt: String,
    val temperature: Double,
    val maxOutputTokens: Int,
    val topP: Double
)

/** Reads the existing shared config without generating keys, endpoints, or a default prompt. */
class VaultAuthConfigManager(context: Context) {
    private val vault = VaultFileSystemManager(context)
    private val _config = MutableStateFlow<VaultAuthConfig?>(null)
    val config: StateFlow<VaultAuthConfig?> = _config.asStateFlow()

    suspend fun reloadFromDisk(): VaultAuthConfig? = withContext(Dispatchers.IO) {
        val json = vault.readJson(".auth/vault_auth_config.json")
        val loaded = json?.let {
            VaultAuthConfig(
                provider = it.optString("provider", ""),
                endpoint = it.optString("endpoint", ""),
                apiKey = it.optString("apiKey", ""),
                activeModel = it.optString("activeModel", ""),
                fallbackModel = it.optString("fallbackModel", ""),
                personaName = it.optString("personaName", ""),
                personaTitle = it.optString("personaTitle", ""),
                systemPrompt = it.optString("systemPrompt", ""),
                temperature = it.optDouble("temperature", 0.0),
                maxOutputTokens = it.optInt("maxOutputTokens", 0),
                topP = it.optDouble("topP", 0.0)
            )
        }
        _config.value = loaded
        loaded
    }

    /** Preserves unknown fields in the caller's JSON; never writes during initialization. */
    suspend fun saveConfig(json: JSONObject) = withContext(Dispatchers.IO) {
        vault.writeJson(".auth/vault_auth_config.json", json)
        reloadFromDisk()
    }

    fun clearMemory() { _config.value = null }
}
