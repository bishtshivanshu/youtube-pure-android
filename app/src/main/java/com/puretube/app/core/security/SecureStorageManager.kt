package com.puretube.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Hardware-backed encrypted storage for tokens, credentials, and user data.
 * Leverages Android KeyStore with AES-256 GCM scheme (backed by StrongBox / TEE).
 */
class SecureStorageManager(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .setRequestStrongBoxBacked(false) // Fallback gracefully if hardware StrongBox is absent
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val PREFS_NAME = "puretube_secure_prefs"
        private const val KEY_ACCESS_TOKEN = "sec_access_token"
        private const val KEY_REFRESH_TOKEN = "sec_refresh_token"
        private const val KEY_TOKEN_EXPIRY = "sec_token_expiry"
        private const val KEY_USER_ID = "sec_user_id"
        private const val KEY_USER_NAME = "sec_user_name"
        private const val KEY_USER_EMAIL = "sec_user_email"
        private const val KEY_USER_AVATAR = "sec_user_avatar"
        private const val KEY_CUSTOM_API_KEY = "sec_custom_api_key"
        private const val KEY_WEB_CLIENT_ID = "sec_web_client_id"
    }

    fun saveWebClientId(clientId: String) {
        sharedPreferences.edit().putString(KEY_WEB_CLIENT_ID, clientId).apply()
    }

    fun getWebClientId(): String? = sharedPreferences.getString(KEY_WEB_CLIENT_ID, null)

    fun saveTokens(accessToken: String, refreshToken: String? = null, expiresInSeconds: Long = 3600) {
        val expiryTimeMillis = System.currentTimeMillis() + (expiresInSeconds * 1000)
        sharedPreferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .apply {
                if (refreshToken != null) putString(KEY_REFRESH_TOKEN, refreshToken)
                putLong(KEY_TOKEN_EXPIRY, expiryTimeMillis)
            }
            .apply()
    }

    fun getAccessToken(): String? {
        val token = sharedPreferences.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiry = sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0L)
        // If expired or near expiration (within 60s), consider expired
        if (System.currentTimeMillis() > (expiry - 60_000)) {
            return null
        }
        return token
    }

    fun getRefreshToken(): String? = sharedPreferences.getString(KEY_REFRESH_TOKEN, null)

    fun saveUserProfile(id: String, name: String, email: String, avatarUrl: String?) {
        sharedPreferences.edit()
            .putString(KEY_USER_ID, id)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_AVATAR, avatarUrl)
            .apply()
    }

    fun getUserProfile(): UserProfile? {
        val id = sharedPreferences.getString(KEY_USER_ID, null) ?: return null
        val name = sharedPreferences.getString(KEY_USER_NAME, "") ?: ""
        val email = sharedPreferences.getString(KEY_USER_EMAIL, "") ?: ""
        val avatar = sharedPreferences.getString(KEY_USER_AVATAR, null)
        return UserProfile(id = id, displayName = name, email = email, avatarUrl = avatar)
    }

    fun isLoggedIn(): Boolean = getAccessToken() != null || getUserProfile() != null

    fun saveCustomApiKey(apiKey: String) {
        sharedPreferences.edit().putString(KEY_CUSTOM_API_KEY, apiKey).apply()
    }

    fun getCustomApiKey(): String? = sharedPreferences.getString(KEY_CUSTOM_API_KEY, null)

    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}

data class UserProfile(
    val id: String,
    val displayName: String,
    val email: String,
    val avatarUrl: String?
)
