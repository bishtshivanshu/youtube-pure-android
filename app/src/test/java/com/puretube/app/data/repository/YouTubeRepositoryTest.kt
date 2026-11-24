package com.puretube.app.data.repository

import com.puretube.app.core.security.SecureStorageManager
import com.puretube.app.core.shorts.ShortsFilterEngine
import com.puretube.app.data.api.YouTubeApiService
import com.puretube.app.data.db.PureTubeDatabase
import com.puretube.app.domain.model.VideoItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class YouTubeRepositoryTest {

    private val apiService = mockk<YouTubeApiService>()
    private val database = mockk<PureTubeDatabase>(relaxed = true)
    private val secureStorage = mockk<SecureStorageManager>(relaxed = true)
    private val shortsFilterEngine = ShortsFilterEngine()

    private lateinit var repository: YouTubeRepository

    @Before
    fun setUp() {
        repository = YouTubeRepositoryImpl(
            apiService = apiService,
            database = database,
            secureStorage = secureStorage,
            shortsFilterEngine = shortsFilterEngine
        )
    }

    @Test
    fun `getHomeFeed purges all shorts from stream emission`() = runBlocking {
        val mixedList = listOf(
            VideoItem(id = "1", title = "Long video", channelId = "c1", channelTitle = "C1", thumbnailUrl = "", durationSeconds = 1800, durationFormatted = "30:00"),
            VideoItem(id = "2", title = "Crazy move #shorts", channelId = "c2", channelTitle = "C2", thumbnailUrl = "", durationSeconds = 20, durationFormatted = "0:20", isShort = true),
            VideoItem(id = "3", title = "Another normal video", channelId = "c3", channelTitle = "C3", thumbnailUrl = "", durationSeconds = 900, durationFormatted = "15:00")
        )

        coEvery { apiService.fetchHomeFeed() } returns mixedList

        val result = repository.getHomeFeed().first()

        assertEquals(2, result.size)
        assertEquals("1", result[0].id)
        assertEquals("3", result[1].id)
        assertFalse(result.any { it.isShort })
    }

    @Test
    fun `searchVideos with blank query returns empty list immediately`() = runBlocking {
        val result = repository.searchVideos("   ").first()
        assertEquals(0, result.size)
    }
}
