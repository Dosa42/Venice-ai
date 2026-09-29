package com.example.data.auth

import android.content.Context
import com.example.data.filesystem.VaultFileSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ChatGPTSession(
    val accessToken: String,
    val refreshToken: String,
    val clientId: String,
    val expiresAt: Long,
    val accountId: String = "",
    val idToken: String = "",
    val email: String = ""
) {
    val isValid: Boolean
        get() = accessToken.isNotBlank() && expiresAt > System.currentTimeMillis()
}

/** Shared file reader/writer only. No OAuth, PKCE, token refresh, or provider requests. */
class ChatGPTAuthManager(context: Context) {
    private val vault = VaultFileSystemManager(context)
    private val _session = MutableStateFlow<ChatGPTSession?>(null)
    val session: StateFlow<ChatGPTSession?> = _session.asStateFlow()

    suspend fun reloadFromDisk(): ChatGPTSession? = withContext(Dispatchers.IO) {
        val json = vault.readJson(".auth/chatgpt_session.json")
        val loaded = json?.let {
            ChatGPTSession(
                accessToken = it.optString("accessToken", ""),
                refreshToken = it.optString("refreshToken", ""),
                clientId = it.optString("clientId", ""),
                expiresAt = it.optLong("expiresAt", 0L),
                accountId = it.optString("accountId", ""),
                idToken = it.optString("idToken", ""),
                email = it.optString("email", "")
            )
        }
        _session.value = loaded
        loaded
    }

    /** Called only when another trusted component explicitly supplies a new session. */
    suspend fun saveSession(session: ChatGPTSession) = withContext(Dispatchers.IO) {
        val json = vault.readJson(".auth/chatgpt_session.json") ?: JSONObject()
        json.put("accessToken", session.accessToken)
        json.put("refreshToken", session.refreshToken)
        json.put("clientId", session.clientId)
        json.put("expiresAt", session.expiresAt)
        json.put("accountId", session.accountId)
        json.put("idToken", session.idToken)
        json.put("email", session.email)
        vault.writeJson(".auth/chatgpt_session.json", json)
        _session.value = session
    }

    fun clearMemory() { _session.value = null }
}
