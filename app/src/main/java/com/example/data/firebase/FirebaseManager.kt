package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.model.ChatMessage
import com.example.data.model.ChatSession
import com.example.data.model.GeneratedArt
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FirebaseManager {
    private const val TAG = "VeniceFirebase"

    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    val currentUser: FirebaseUser?
        get() = try {
            auth.currentUser
        } catch (e: Exception) {
            null
        }

    val isUserSignedIn: Boolean
        get() = currentUser != null

    val isAnonymous: Boolean
        get() = currentUser?.isAnonymous == true

    /**
     * Sign in anonymously for Venice Private / Zero-Knowledge Guest Mode
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: throw IllegalStateException("Anonymous user is null")
            Log.d(TAG, "Signed in anonymously: ${user.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign-in failed", e)
            Result.failure(e)
        }
    }

    /**
     * Launch Google Sign-In using modern Android Credential Manager
     */
    suspend fun signInWithGoogle(context: Context, serverClientId: String? = null): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val credentialManager = CredentialManager.create(context)

            // If serverClientId is not set, use a fallback client ID or sign in anonymously
            val clientId = serverClientId?.ifBlank { null } 
                ?: "168629668627-venice-placeholder.apps.googleusercontent.com"

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(context = context, request = request)
            val credential = response.credential

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user ?: throw IllegalStateException("User is null after Google auth")
            Log.d(TAG, "Signed in with Google: ${user.email}")
            Result.success(user)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
        }
    }

    /**
     * Firestore: Save or update session metadata
     */
    suspend fun syncSessionToFirestore(session: ChatSession): Boolean = withContext(Dispatchers.IO) {
        val uid = currentUser?.uid ?: return@withContext false
        try {
            val sessionMap = hashMapOf(
                "id" to session.id,
                "title" to session.title,
                "personaId" to session.personaId,
                "selectedModel" to session.selectedModel,
                "highThinkingEnabled" to session.highThinkingEnabled,
                "updatedAt" to session.updatedAt,
                "createdAt" to session.createdAt
            )
            firestore.collection("users")
                .document(uid)
                .collection("sessions")
                .document(session.id)
                .set(sessionMap, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing session to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Firestore: Save chat message under session
     */
    suspend fun syncMessageToFirestore(sessionId: String, message: ChatMessage): Boolean = withContext(Dispatchers.IO) {
        val uid = currentUser?.uid ?: return@withContext false
        try {
            val msgMap = hashMapOf(
                "id" to message.id,
                "role" to message.role,
                "text" to message.text,
                "thoughtProcess" to (message.thoughtProcess ?: ""),
                "modelUsed" to (message.modelUsed ?: ""),
                "thinkingEnabled" to message.thinkingEnabled,
                "timestamp" to message.timestamp,
                "hasImage" to (message.imageBase64 != null)
            )
            firestore.collection("users")
                .document(uid)
                .collection("sessions")
                .document(sessionId)
                .collection("messages")
                .document(message.id)
                .set(msgMap, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing message to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Firestore: Load user's synced sessions
     */
    suspend fun loadSessionsFromFirestore(): List<ChatSession> = withContext(Dispatchers.IO) {
        val uid = currentUser?.uid ?: return@withContext emptyList()
        try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("sessions")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: doc.id
                val title = doc.getString("title") ?: "Conversation"
                val personaId = doc.getString("personaId") ?: "venice_unfiltered"
                val model = doc.getString("selectedModel") ?: "gemini-3.5-flash"
                val thinking = doc.getBoolean("highThinkingEnabled") ?: false
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

                ChatSession(
                    id = id,
                    title = title,
                    personaId = personaId,
                    selectedModel = model,
                    highThinkingEnabled = thinking,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading sessions from Firestore", e)
            emptyList()
        }
    }

    /**
     * Firestore: Save generated art to user's gallery
     */
    suspend fun saveArtToFirestore(art: GeneratedArt): Boolean = withContext(Dispatchers.IO) {
        val uid = currentUser?.uid ?: return@withContext false
        try {
            val artMap = hashMapOf(
                "id" to art.id,
                "prompt" to art.prompt,
                "aspectRatio" to art.aspectRatio,
                "style" to art.style,
                "isEdit" to art.isEdit,
                "originalPrompt" to (art.originalPrompt ?: ""),
                "timestamp" to art.timestamp,
                "imageBase64" to art.imageBase64
            )
            firestore.collection("users")
                .document(uid)
                .collection("gallery")
                .document(art.id)
                .set(artMap, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving art to Firestore", e)
            false
        }
    }

    /**
     * Firestore: Load user gallery
     */
    suspend fun loadGalleryFromFirestore(): List<GeneratedArt> = withContext(Dispatchers.IO) {
        val uid = currentUser?.uid ?: return@withContext emptyList()
        try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("gallery")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(30)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val base64 = doc.getString("imageBase64") ?: return@mapNotNull null
                GeneratedArt(
                    id = doc.getString("id") ?: doc.id,
                    prompt = doc.getString("prompt") ?: "",
                    imageBase64 = base64,
                    aspectRatio = doc.getString("aspectRatio") ?: "1:1",
                    style = doc.getString("style") ?: "Cinematic",
                    isEdit = doc.getBoolean("isEdit") ?: false,
                    originalPrompt = doc.getString("originalPrompt"),
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading gallery from Firestore", e)
            emptyList()
        }
    }

    /**
     * Venice Panic / Burn Data: Deletes user's cloud documents in Firestore
     */
    suspend fun burnCloudData(): Boolean = withContext(Dispatchers.IO) {
        val uid = currentUser?.uid ?: return@withContext false
        try {
            val sessions = firestore.collection("users").document(uid).collection("sessions").get().await()
            for (doc in sessions.documents) {
                val msgs = doc.reference.collection("messages").get().await()
                for (m in msgs.documents) {
                    m.reference.delete().await()
                }
                doc.reference.delete().await()
            }
            val gallery = firestore.collection("users").document(uid).collection("gallery").get().await()
            for (g in gallery.documents) {
                g.reference.delete().await()
            }
            firestore.collection("users").document(uid).delete().await()
            Log.d(TAG, "Successfully wiped user cloud data")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error burning cloud data", e)
            false
        }
    }
}
