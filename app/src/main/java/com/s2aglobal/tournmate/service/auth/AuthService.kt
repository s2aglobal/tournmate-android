package com.s2aglobal.tournmate.service.auth

import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.s2aglobal.tournmate.BuildConfig

sealed class AuthResult {
    /** [displayName] is the provider-supplied name (e.g. Google profile), when available. */
    data class Success(val user: FirebaseUser, val displayName: String? = null) : AuthResult()
    data class Error(val message: String) : AuthResult()
    /** The user dismissed the provider sheet — no error is shown (matches iOS). */
    object Cancelled : AuthResult()
}

/**
 * Email verification is enforced for every build that talks to the prod Firebase
 * project (prodDebug included), so a debug build can't bypass it against prod data.
 * iOS Debug can't reach prod at all; dev keeps verification off for testing.
 */
val requiresEmailVerification: Boolean
    get() = BuildConfig.FLAVOR == "prod"

/** Converts Firebase auth errors to user-friendly messages (mirrors iOS `friendlyAuthError`). */
fun friendlyAuthError(error: Throwable): String {
    if (error is FirebaseTooManyRequestsException) return "Too many failed attempts. Please try again later."
    return when ((error as? FirebaseAuthException)?.errorCode) {
        "ERROR_WRONG_PASSWORD" -> "Incorrect password. Please try again."
        "ERROR_USER_NOT_FOUND" -> "No account found with this email."
        // Returned instead of wrong-password / user-not-found when email-enumeration protection is on.
        "ERROR_INVALID_CREDENTIAL" -> "Incorrect email or password. Please try again."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "An account with this email already exists. Try signing in."
        "ERROR_WEAK_PASSWORD" -> "Password must be at least 6 characters."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many failed attempts. Please try again later."
        else -> error.localizedMessage ?: "Something went wrong. Please try again."
    }
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
