package com.puretube.app.data.repository

import com.puretube.app.core.security.SecureStorageManager
import com.puretube.app.core.shorts.ShortsFilterEngine
import com.puretube.app.data.api.YouTubeApiService
import com.puretube.app.data.db.PureTubeDatabase
import com.puretube.app.data.db.SavedVideoDao
import com.puretube.app.data.db.SavedVideoEntity
import com.puretube.app.data.db.WatchHistoryDao
import com.puretube.app.data.db.WatchHistoryEntity
import com.puretube.app.domain.model.StreamInfo
import com.puretube.app.domain.model.VideoItem
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class YouTubeRepositoryTest {

    private val apiService = mockk<YouTubeApiService>()
    private val database = mockk<PureTubeDatabase>()
    private val watchHistoryDao = mockk<WatchHistoryDao>(relaxed = true)
    private val savedVideoDao = mockk<SavedVideoDao>(relaxed = true)
    private val secureStorage = mockk<SecureStorageManager>(relaxed = true)
    private val shortsFilterEngine = ShortsFilterEngine()

    private lateinit var repository: YouTubeRepository

    @Before
    fun setUp() {
        every { database.watchHistoryDao() } returns watchHistoryDao
        every { database.savedVideoDao() } returns savedVideoDao

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
    fun `getSubscriptionsFeed purges all shorts from subscription uploads`() = runBlocking {
        val mixedList = listOf(
            VideoItem(id = "sub_1", title = "Android Deep Dive", channelId = "c1", channelTitle = "Dev", thumbnailUrl = "", durationSeconds = 1200, durationFormatted = "20:00"),
            VideoItem(id = "sub_2", title = "Quick Hack #shorts", channelId = "c2", channelTitle = "Tips", thumbnailUrl = "", durationSeconds = 15, durationFormatted = "0:15", isShort = true)
        )

        coEvery { apiService.fetchHomeFeed() } returns mixedList

        val result = repository.getSubscriptionsFeed().first()

        assertEquals(1, result.size)
        assertEquals("sub_1", result[0].id)
    }

    @Test
    fun `searchVideos with blank query returns empty list immediately`() = runBlocking {
        val result = repository.searchVideos("   ").first()
        assertEquals(0, result.size)
    }

    @Test
    fun `searchVideos queries api and sanitizes shorts results`() = runBlocking {
        val searchResults = listOf(
            VideoItem(id = "s1", title = "Kotlin Tutorial", channelId = "c1", channelTitle = "C1", thumbnailUrl = "", durationSeconds = 3600, durationFormatted = "1:00:00"),
            VideoItem(id = "s2", title = "Viral Reel #shorts", channelId = "c2", channelTitle = "C2", thumbnailUrl = "", durationSeconds = 30, durationFormatted = "0:30", isShort = true)
        )

        coEvery { apiService.searchVideos("kotlin") } returns searchResults

        val result = repository.searchVideos("kotlin").first()

        assertEquals(1, result.size)
        assertEquals("s1", result[0].id)
    }

    @Test
    fun `getStreamInfo resolves stream information`() = runBlocking {
        val expectedStream = StreamInfo(
            videoId = "v1",
            title = "Test Video",
            streamUrl = "https://example.com/stream.mp4",
            isHls = false,
            isDash = false,
            qualities = emptyList()
        )

        coEvery { apiService.resolveStreamInfo("v1") } returns expectedStream

        val result = repository.getStreamInfo("v1").first()

        assertEquals("v1", result.videoId)
        assertEquals("https://example.com/stream.mp4", result.streamUrl)
    }

    @Test
    fun `recordWatchHistory inserts watch history entity`() = runBlocking {
        val video = VideoItem(
            id = "w1",
            title = "History Video",
            channelId = "c1",
            channelTitle = "Channel",
            thumbnailUrl = "thumb.jpg",
            durationSeconds = 600,
            durationFormatted = "10:00"
        )

        repository.recordWatchHistory(video, 45000L)

        coVerify {
            watchHistoryDao.insertOrUpdate(match {
                it.videoId == "w1" && it.playbackPositionMillis == 45000L && it.title == "History Video"
            })
        }
    }

    @Test
    fun `toggleSaveVideo inserts saved video entity`() = runBlocking {
        val video = VideoItem(
            id = "save_1",
            title = "Saved Video",
            channelId = "c1",
            channelTitle = "Channel",
            thumbnailUrl = "thumb.jpg",
            durationSeconds = 300,
            durationFormatted = "5:00"
        )

        repository.toggleSaveVideo(video, isLiked = true)

        coVerify {
            savedVideoDao.saveVideo(match {
                it.videoId == "save_1" && it.isLiked && it.title == "Saved Video"
            })
        }
    }

    @Test
    fun `isVideoSaved checks dao stream`() = runBlocking {
        every { savedVideoDao.isSaved("save_1") } returns flowOf(true)

        val result = repository.isVideoSaved("save_1").first()

        assertTrue(result)
    }
}
