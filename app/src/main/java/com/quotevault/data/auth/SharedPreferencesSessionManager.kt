package com.quotevault.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.gotrue.SessionManager
import io.github.jan.supabase.gotrue.user.UserSession
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SharedPreferencesSessionManager(
    private val context: Context
) : SessionManager {

    private val sharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        EncryptedSharedPreferences.create(
            context,
            "supabase_session_encrypted",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun saveSession(session: UserSession) {
        withContext(Dispatchers.IO) {
            val sessionJson = Json.encodeToString(UserSession.serializer(), session)
            sharedPreferences.edit().putString("session", sessionJson).apply()
        }
    }

    override suspend fun loadSession(): UserSession? {
        return withContext(Dispatchers.IO) {
            val sessionJson = sharedPreferences.getString("session", null) ?: return@withContext null
            try {
                Json.decodeFromString(UserSession.serializer(), sessionJson)
            } catch (e: Exception) {
                null
            }
        }
    }

    override suspend fun deleteSession() {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit().remove("session").apply()
        }
    }
}
