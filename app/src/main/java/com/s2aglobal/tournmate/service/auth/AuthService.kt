package com.s2aglobal.tournmate.service.auth

import com.google.firebase.auth.FirebaseUser

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

interface AuthService {
    val currentUser: FirebaseUser?
    val isSignedIn: Boolean

    suspend fun signInWithGoogle(activityContext: android.content.Context): AuthResult
    suspend fun signInWithEmail(email: String, password: String): AuthResult
    suspend fun createAccount(email: String, password: String): AuthResult
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun reloadCurrentUser(): Result<Unit>
    suspend fun signOut()
    suspend fun deleteAccount(): Result<Unit>

    fun addAuthStateListener(listener: (FirebaseUser?) -> Unit)
    fun removeAuthStateListener(listener: (FirebaseUser?) -> Unit)
}
