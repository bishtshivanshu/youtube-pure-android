package com.puretube.app.core.security

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Hardened network interceptor:
 * 1. Automatically attaches Google OAuth Bearer token to authenticated endpoints.
 * 2. Injects custom YouTube API key if configured.
 * 3. Strips privacy-invasive headers (e.g. advertiser IDs, unique hardware fingerprints).
 */
class AuthInterceptor(
    private val secureStorage: SecureStorageManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // Strip non-essential tracking headers
        requestBuilder.removeHeader("X-Client-Data")
        requestBuilder.removeHeader("X-Device-Id")

        // Add standard secure user-agent
        requestBuilder.header("User-Agent", "Mozilla/5.0 (Linux; Android 16; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")

        val token = secureStorage.getAccessToken()
        if (token != null && originalRequest.header("Authorization") == null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        // Add API key query param for Google APIs if applicable
        val customKey = secureStorage.getCustomApiKey()
        if (customKey != null && originalRequest.url.host.contains("googleapis.com")) {
            val url = originalRequest.url.newBuilder()
                .addQueryParameter("key", customKey)
                .build()
            requestBuilder.url(url)
        }

        return chain.proceed(requestBuilder.build())
    }
}
