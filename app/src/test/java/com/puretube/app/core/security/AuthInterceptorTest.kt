package com.puretube.app.core.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private val secureStorage = mockk<SecureStorageManager>()
    private val chain = mockk<Interceptor.Chain>()
    private val capturedRequest = slot<Request>()

    private lateinit var interceptor: AuthInterceptor

    @Before
    fun setUp() {
        interceptor = AuthInterceptor(secureStorage)
        every { chain.proceed(capture(capturedRequest)) } answers {
            Response.Builder()
                .request(capturedRequest.captured)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }
    }

    @Test
    fun `attaches Bearer token when access token is present`() {
        every { secureStorage.getAccessToken() } returns "mock_access_token_123"
        every { secureStorage.getCustomApiKey() } returns null

        val originalRequest = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/browse")
            .build()
        every { chain.request() } returns originalRequest

        interceptor.intercept(chain)

        assertEquals("Bearer mock_access_token_123", capturedRequest.captured.header("Authorization"))
    }

    @Test
    fun `does not overwrite existing Authorization header`() {
        every { secureStorage.getAccessToken() } returns "mock_access_token_123"
        every { secureStorage.getCustomApiKey() } returns null

        val originalRequest = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/browse")
            .header("Authorization", "Bearer existing_token")
            .build()
        every { chain.request() } returns originalRequest

        interceptor.intercept(chain)

        assertEquals("Bearer existing_token", capturedRequest.captured.header("Authorization"))
    }

    @Test
    fun `strips tracking headers X-Client-Data and X-Device-Id`() {
        every { secureStorage.getAccessToken() } returns null
        every { secureStorage.getCustomApiKey() } returns null

        val originalRequest = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/browse")
            .header("X-Client-Data", "tracking_data_123")
            .header("X-Device-Id", "hardware_fingerprint_xyz")
            .build()
        every { chain.request() } returns originalRequest

        interceptor.intercept(chain)

        assertNull(capturedRequest.captured.header("X-Client-Data"))
        assertNull(capturedRequest.captured.header("X-Device-Id"))
    }

    @Test
    fun `appends custom api key query parameter for googleapis requests`() {
        every { secureStorage.getAccessToken() } returns null
        every { secureStorage.getCustomApiKey() } returns "my_custom_api_key_456"

        val originalRequest = Request.Builder()
            .url("https://www.googleapis.com/youtube/v3/videos?id=123")
            .build()
        every { chain.request() } returns originalRequest

        interceptor.intercept(chain)

        val queryParam = capturedRequest.captured.url.queryParameter("key")
        assertEquals("my_custom_api_key_456", queryParam)
    }

    @Test
    fun `does not append api key parameter for non googleapis requests`() {
        every { secureStorage.getAccessToken() } returns null
        every { secureStorage.getCustomApiKey() } returns "my_custom_api_key_456"

        val originalRequest = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/browse")
            .build()
        every { chain.request() } returns originalRequest

        interceptor.intercept(chain)

        val queryParam = capturedRequest.captured.url.queryParameter("key")
        assertNull(queryParam)
    }
}
