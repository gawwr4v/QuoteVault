package com.quotevault.domain.repository

import com.quotevault.domain.model.UserProfile
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.coroutines.flow.StateFlow

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: UserInfo) : AuthState()
    object Unauthenticated : AuthState()
}

interface AuthRepository {
    val authState: StateFlow<AuthState>
    
    suspend fun signUp(email: String, password: String, fullName: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut()
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun getCurrentProfile(): Result<UserProfile>
    suspend fun getCurrentUser(): UserInfo?
}
