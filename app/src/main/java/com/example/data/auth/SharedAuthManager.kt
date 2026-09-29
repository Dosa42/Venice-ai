package com.example.data.auth

import android.os.Environment
import android.os.FileObserver
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File

data class SharedAuthSession(
    val accessToken: String = "",
    val refreshToken: String = "",
    val clientId: String = "app_EMoamEEZ73f0CkXaXp7hrann",
    val expiresAt: Long = 0L
) {
    val isValid: Boolean get() = accessToken.isNotBlank() && (expiresAt == 0L || System.currentTimeMillis() < expiresAt)
}

class SharedAuthManager(private val scope: CoroutineScope) {
    private val TAG = "VeniceSharedAuth"

    // 1. Het universele ankerpunt op het toestel
    private val sharedVaultAuthDir: File get() {
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(File(downloadDir, "ObsidianVault"), ".auth")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private val sessionFile: File get() = File(sharedVaultAuthDir, "chatgpt_session.json")

    private val _session = MutableStateFlow<SharedAuthSession?>(null)
    val session: StateFlow<SharedAuthSession?> = _session.asStateFlow()

    init {
        loadSession()
        startInotifyWatcher()
    }

    fun loadSession() {
        if (!sessionFile.exists()) {
            _session.value = null
            return
        }
        try {
            val json = JSONObject(sessionFile.readText())
            val s = SharedAuthSession(
                accessToken = json.optString("accessToken", ""),
                refreshToken = json.optString("refreshToken", ""),
                clientId = json.optString("clientId", "app_EMoamEEZ73f0CkXaXp7hrann"),
                expiresAt = json.optLong("expiresAt", 0L)
            )
            if (s.isValid) {
                _session.value = s
                Log.d(TAG, "Instant SSO Login geslaagd vanuit shared vault node!")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fout bij inlezen shared session", e)
        }
    }

    // 2. Realtime Linux Inotify: als de andere app token ververst, update Venice-ai binnen 50ms
    private fun startInotifyWatcher() {
        try {
            val observer = object : FileObserver(sharedVaultAuthDir.absolutePath,
                FileObserver.MODIFY or FileObserver.CREATE) {
                override fun onEvent(event: Int, path: String?) {
                    if (path == "chatgpt_session.json") {
                        loadSession()
                    }
                }
            }
            observer.startWatching()
        } catch (e: Exception) {
            Log.w(TAG, "FileObserver niet beschikbaar", e)
        }
    }
}
