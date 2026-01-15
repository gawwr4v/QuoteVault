package com.quotevault.data.repository

import com.quotevault.data.remote.ProfileDto
import com.quotevault.domain.model.UserPreferences
import com.quotevault.domain.model.UserProfile
import com.quotevault.domain.repository.AuthRepository
import com.quotevault.domain.repository.AuthState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import javax.inject.Inject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            supabase.auth.sessionStatus.collectLatest { status ->
                android.util.Log.d("AuthRepo", "SessionStatus: $status")
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = supabase.auth.currentUserOrNull()
                        if (user != null) {
                            android.util.Log.d("AuthRepo", "User authenticated: ${user.id}")
                            _authState.value = AuthState.Authenticated(user)
                        } else {
                            _authState.value = AuthState.Unauthenticated
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        android.util.Log.d("AuthRepo", "User not authenticated")
                        _authState.value = AuthState.Unauthenticated
                    }
                    is SessionStatus.LoadingFromStorage -> {
                        // keep loading state while supabase restores session
                        android.util.Log.d("AuthRepo", "Loading session from storage...")
                        _authState.value = AuthState.Loading
                    }
                    is SessionStatus.NetworkError -> {
                        // network error but might have valid session, keep current state or try offline
                        android.util.Log.w("AuthRepo", "Network error during session check")
                        // Keep user authenticated if they were already
                        if (_authState.value !is AuthState.Authenticated) {
                            _authState.value = AuthState.Unauthenticated
                        }
                    }
                }
            }
        }
    }

    override suspend fun signUp(email: String, password: String, fullName: String): Result<Unit> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("full_name", fullName)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        android.util.Log.d("AuthRepo", "signIn called with email=$email")
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            android.util.Log.d("AuthRepo", "signIn success")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepo", "signIn failed", e)
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            android.util.Log.e("AuthRepo", "signOut failed remotely", e)
        } finally {
            withContext(NonCancellable) {
                try {
                    // 1. clear supabase session (memory + storage)
                    supabase.auth.clearSession()
                } catch (localEx: Exception) {
                    android.util.Log.e("AuthRepo", "clearSession failed", localEx)
                }

                // 2. force state update
                _authState.value = AuthState.Unauthenticated
                
                // 3. manual clearing of supabase preferences
                try {
                    val prefsName = "supabase-gotrue-store"
                    context.getSharedPreferences(prefsName, android.content.Context.MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply()
                        
                    android.util.Log.d("AuthRepo", "Manually cleared SharedPreferences: $prefsName")
                } catch (e: Exception) {
                     android.util.Log.e("AuthRepo", "Manual pref clear failed", e)
                }
            }
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            supabase.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepo", "resetPassword failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getCurrentProfile(): Result<UserProfile> {
        return try {
            val user = supabase.auth.currentUserOrNull() ?: throw Exception("No user logged in")
            
            try {
                val profileDto = supabase.postgrest.from("profiles")
                    .select {
                        filter {
                            eq("id", user.id)
                        }
                    }.decodeSingle<ProfileDto>()
                Result.success(profileDto.toDomain())
            } catch (e: Exception) {
                // fallback to auth user data if profile table is missing/empty (Todo: public.profiles table missing)
                android.util.Log.w("AuthRepo", "Failed to fetch profile from DB, using Auth Session data", e)
                val fallbackProfile = UserProfile(
                    id = user.id,
                    email = user.email ?: "",
                    fullName = user.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: "",
                    avatarUrl = null,
                    preferences = UserPreferences()
                )
                Result.success(fallbackProfile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun ProfileDto.toDomain(): UserProfile {
        return UserProfile(
            id = id,
            email = email ?: "",
            fullName = fullName,
            avatarUrl = avatarUrl,
            preferences = UserPreferences(
                theme = preferences?.theme ?: "system",
                fontScale = preferences?.fontScale ?: 1.0f
            )
        )
    }
    override suspend fun getCurrentUser(): UserInfo? {
        return supabase.auth.currentUserOrNull()
    }
}
