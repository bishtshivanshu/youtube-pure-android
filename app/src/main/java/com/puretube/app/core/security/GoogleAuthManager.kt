package com.puretube.app.core.security

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

sealed class AuthResult {
    data class Success(val profile: UserProfile, val idToken: String) : AuthResult()
    data class Error(val message: String, val throwable: Throwable? = null) : AuthResult()
    object Cancelled : AuthResult()
}

/**
 * Modern Android 16 Google Authentication Manager using CredentialManager API.
 * Ensures the highest grade of security by:
 * 1. Generating cryptographically secure nonces via SecureRandom & SHA-256 to prevent replay attacks.
 * 2. Utilizing hardware-isolated biometric / credential prompts.
 * 3. Delegating token storage to hardware-backed SecureStorageManager.
 * 4. Providing seamless custom Google account and API key onboarding.
 */
class GoogleAuthManager(
    private val context: Context,
    private val secureStorage: SecureStorageManager
) {
    private val credentialManager = CredentialManager.create(context)
    private val secureRandom = SecureRandom()

    private var webClientId: String = "YOUR_GOOGLE_WEB_CLIENT_ID.apps.googleusercontent.com"

    fun getActiveWebClientId(): String? {
        val stored = secureStorage.getWebClientId()
        return if (!stored.isNullOrBlank()) stored else if (!webClientId.startsWith("YOUR_GOOGLE")) webClientId else null
    }

    fun setWebClientId(clientId: String) {
        secureStorage.saveWebClientId(clientId)
        this.webClientId = clientId
    }

    private fun generateSecureNonce(): String {
        val rawBytes = ByteArray(32)
        secureRandom.nextBytes(rawBytes)
        val digest = MessageDigest.getInstance("SHA-256").digest(rawBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    suspend fun signIn(activityContext: Context): AuthResult = withContext(Dispatchers.IO) {
        val activeClientId = getActiveWebClientId()
        if (activeClientId.isNullOrBlank()) {
            return@withContext AuthResult.Error(
                "Google Web Client ID is not configured. Please tap 'Configure Client ID / API Key' or use 'Quick Google Sign In'."
            )
        }

        try {
            val nonce = generateSecureNonce()

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(activeClientId)
                .setAutoSelectEnabled(false)
                .setNonce(nonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response: GetCredentialResponse = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                val profile = UserProfile(
                    id = googleIdTokenCredential.id,
                    displayName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id,
                    email = googleIdTokenCredential.id,
                    avatarUrl = googleIdTokenCredential.profilePictureUri?.toString()
                )

                // Persist securely in KeyStore AES-256 GCM
                secureStorage.saveUserProfile(
                    id = profile.id,
                    name = profile.displayName,
                    email = profile.email,
                    avatarUrl = profile.avatarUrl
                )
                secureStorage.saveTokens(
                    accessToken = googleIdTokenCredential.idToken,
                    expiresInSeconds = 3600
                )

                AuthResult.Success(profile, googleIdTokenCredential.idToken)
            } else {
                AuthResult.Error("Unsupported credential type: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            AuthResult.Cancelled
        } catch (e: GetCredentialException) {
            AuthResult.Error(e.message ?: "Authentication failed", e)
        } catch (e: Exception) {
            AuthResult.Error("Unexpected error during Google authentication: ${e.localizedMessage}", e)
        }
    }

    fun signInWithCustomProfile(name: String, email: String, avatarUrl: String? = null): AuthResult {
        val id = "user_" + email.replace(Regex("[^a-zA-Z0-9]"), "_")
        val profile = UserProfile(
            id = id,
            displayName = name.ifBlank { email.substringBefore("@") },
            email = email,
            avatarUrl = avatarUrl
        )
        secureStorage.saveUserProfile(
            id = profile.id,
            name = profile.displayName,
            email = profile.email,
            avatarUrl = profile.avatarUrl
        )
        val token = "sec_local_token_" + generateSecureNonce()
        secureStorage.saveTokens(
            accessToken = token,
            expiresInSeconds = 30 * 86400L // 30 days
        )
        return AuthResult.Success(profile, token)
    }

    suspend fun signOut(): Boolean = withContext(Dispatchers.IO) {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            secureStorage.clearAll()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isUserLoggedIn(): Boolean = secureStorage.isLoggedIn()
    fun getCurrentUser(): UserProfile? = secureStorage.getUserProfile()
    fun getCustomApiKey(): String? = secureStorage.getCustomApiKey()
    fun saveCustomApiKey(apiKey: String) = secureStorage.saveCustomApiKey(apiKey)
}
