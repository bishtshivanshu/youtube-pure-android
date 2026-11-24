package com.puretube.app.data.api

import com.puretube.app.core.shorts.ShortsFilterEngine
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class YouTubeApiServiceTest {

    private val okHttpClient = mockk<OkHttpClient>()
    private val call = mockk<Call>()
    private val shortsFilterEngine = ShortsFilterEngine()

    private lateinit var apiService: YouTubeApiService

    @Before
    fun setUp() {
        apiService = YouTubeApiService(okHttpClient, shortsFilterEngine)
        every { okHttpClient.newCall(any()) } returns call
    }

    @Test
    fun `fetchHomeFeed parses valid InnerTube response and ignores Shorts shelves`() = runBlocking {
        val sampleInnerTubeJson = """
        {
          "contents": {
            "twoColumnBrowseResultsRenderer": {
              "tabs": [
                {
                  "tabRenderer": {
                    "content": {
                      "sectionListRenderer": {
                        "contents": [
                          {
                            "itemSectionRenderer": {
                              "contents": [
                                {
                                  "videoRenderer": {
                                    "videoId": "video_valid_1",
                                    "title": { "runs": [{ "text": "Android 16 Deep Dive" }] },
                                    "ownerText": { "runs": [{ "text": "Android Developers" }] },
                                    "lengthText": { "simpleText": "14:20" },
                                    "viewCountText": { "simpleText": "50K views" },
                                    "publishedTimeText": { "simpleText": "2 days ago" },
                                    "thumbnail": { "thumbnails": [{ "url": "https://img.youtube.com/vi/video_valid_1/0.jpg" }] }
                                  }
                                },
                                {
                                  "shortsShelfRenderer": {
                                    "title": { "simpleText": "Shorts" },
                                    "contents": [
                                      {
                                        "videoRenderer": {
                                          "videoId": "short_1",
                                          "title": { "simpleText": "Quick short trick" }
                                        }
                                      }
                                    ]
                                  }
                                },
                                {
                                  "videoRenderer": {
                                    "videoId": "video_valid_2",
                                    "title": { "runs": [{ "text": "Building Secure Apps" }] },
                                    "ownerText": { "runs": [{ "text": "Security Pro" }] },
                                    "lengthText": { "simpleText": "28:15" },
                                    "viewCountText": { "simpleText": "12K views" },
                                    "publishedTimeText": { "simpleText": "1 week ago" }
                                  }
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  }
                }
              ]
            }
          }
        }
        """.trimIndent()

        val mockResponse = Response.Builder()
            .request(Request.Builder().url("https://www.youtube.com/youtubei/v1/browse").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(sampleInnerTubeJson.toResponseBody("application/json".toMediaType()))
            .build()

        every { call.execute() } returns mockResponse

        val videos = apiService.fetchHomeFeed()

        assertEquals(2, videos.size)
        assertEquals("video_valid_1", videos[0].id)
        assertEquals("Android 16 Deep Dive", videos[0].title)
        assertEquals(860L, videos[0].durationSeconds)
        assertEquals("video_valid_2", videos[1].id)
        assertFalse(videos.any { it.id == "short_1" })
    }

    @Test
    fun `fetchHomeFeed falls back to curated long-form feed on network failure`() = runBlocking {
        every { call.execute() } throws java.io.IOException("No internet connection")

        val videos = apiService.fetchHomeFeed()

        assertTrue(videos.isNotEmpty())
        assertTrue(videos.any { it.id == "dQw4w9WgXcQ" })
        assertFalse(videos.any { it.isShort })
    }

    @Test
    fun `searchVideos purges shorts and parses valid results`() = runBlocking {
        val sampleSearchJson = """
        {
          "contents": {
            "sectionListRenderer": {
              "contents": [
                {
                  "videoRenderer": {
                    "videoId": "search_res_1",
                    "title": { "runs": [{ "text": "Jetpack Compose Tutorial" }] },
                    "ownerText": { "runs": [{ "text": "Tech Academy" }] },
                    "lengthText": { "simpleText": "45:00" }
                  }
                },
                {
                  "videoRenderer": {
                    "videoId": "search_short",
                    "title": { "runs": [{ "text": "Cool trick #shorts" }] },
                    "ownerText": { "runs": [{ "text": "Tips" }] },
                    "lengthText": { "simpleText": "0:25" }
                  }
                }
              ]
            }
          }
        }
        """.trimIndent()

        val mockResponse = Response.Builder()
            .request(Request.Builder().url("https://www.youtube.com/youtubei/v1/search").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(sampleSearchJson.toResponseBody("application/json".toMediaType()))
            .build()

        every { call.execute() } returns mockResponse

        val results = apiService.searchVideos("Jetpack Compose")

        assertEquals(1, results.size)
        assertEquals("search_res_1", results[0].id)
        assertEquals(2700L, results[0].durationSeconds)
    }

    @Test
    fun `resolveStreamInfo returns playable stream with multiple qualities`() = runBlocking {
        val streamInfo = apiService.resolveStreamInfo("test_video_123")

        assertEquals("test_video_123", streamInfo.videoId)
        assertTrue(streamInfo.streamUrl.isNotBlank())
        assertTrue(streamInfo.qualities.isNotEmpty())
        assertTrue(streamInfo.qualities.any { it.label == "1080p" })
        assertTrue(streamInfo.qualities.any { it.label == "720p" })
    }
}
