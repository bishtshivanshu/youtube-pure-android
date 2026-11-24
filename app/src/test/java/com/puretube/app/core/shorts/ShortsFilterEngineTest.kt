package com.puretube.app.core.shorts

import com.puretube.app.domain.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShortsFilterEngineTest {

    private lateinit var filterEngine: ShortsFilterEngine

    @Before
    fun setUp() {
        filterEngine = ShortsFilterEngine()
    }

    @Test
    fun `isShortVideo detects explicit isShort flag`() {
        val video = VideoItem(
            id = "1",
            title = "A cool video",
            channelId = "c1",
            channelTitle = "Channel",
            thumbnailUrl = "http://thumb.jpg",
            durationSeconds = 45,
            durationFormatted = "0:45",
            isShort = true
        )
        assertTrue(filterEngine.isShortVideo(video))
    }

    @Test
    fun `isShortVideo detects #shorts in title`() {
        val video = VideoItem(
            id = "2",
            title = "Insane football skill #shorts",
            channelId = "c1",
            channelTitle = "Channel",
            thumbnailUrl = "http://thumb.jpg",
            durationSeconds = 30,
            durationFormatted = "0:30",
            isShort = false
        )
        assertTrue(filterEngine.isShortVideo(video))
    }

    @Test
    fun `isShortVideo detects vertical video under 60 seconds`() {
        val verticalShort = VideoItem(
            id = "3",
            title = "Quick tip",
            channelId = "c1",
            channelTitle = "Channel",
            thumbnailUrl = "http://thumb.jpg",
            durationSeconds = 58,
            durationFormatted = "0:58",
            isShort = false,
            aspectRatio = 0.5625f // 9:16 vertical
        )
        assertTrue(filterEngine.isShortVideo(verticalShort))
    }

    @Test
    fun `isShortVideo preserves standard long-form landscape video`() {
        val standardVideo = VideoItem(
            id = "4",
            title = "Building an Android 16 App with Kotlin",
            channelId = "c2",
            channelTitle = "Tech Channel",
            thumbnailUrl = "http://thumb.jpg",
            durationSeconds = 1240, // ~20 minutes
            durationFormatted = "20:40",
            isShort = false,
            aspectRatio = 1.778f // 16:9 landscape
        )
        assertFalse(filterEngine.isShortVideo(standardVideo))
    }

    @Test
    fun `sanitizeList filters all shorts from list`() {
        val list = listOf(
            VideoItem(id = "1", title = "Full Documentary", channelId = "c1", channelTitle = "Docu", thumbnailUrl = "", durationSeconds = 3600, durationFormatted = "1:00:00"),
            VideoItem(id = "2", title = "Funny cat #shorts", channelId = "c1", channelTitle = "Cats", thumbnailUrl = "", durationSeconds = 15, durationFormatted = "0:15", isShort = true),
            VideoItem(id = "3", title = "Podcast Episode 42", channelId = "c2", channelTitle = "Pod", thumbnailUrl = "", durationSeconds = 5400, durationFormatted = "1:30:00"),
            VideoItem(id = "4", title = "Trick shot", channelId = "c3", channelTitle = "Sports", thumbnailUrl = "", durationSeconds = 25, durationFormatted = "0:25", aspectRatio = 0.56f)
        )

        val sanitized = filterEngine.sanitizeList(list)
        assertEquals(2, sanitized.size)
        assertEquals("1", sanitized[0].id)
        assertEquals("3", sanitized[1].id)
    }

    @Test
    fun `isShortsRenderer catches reel and shorts renderers`() {
        assertTrue(filterEngine.isShortsRenderer("reelItemRenderer"))
        assertTrue(filterEngine.isShortsRenderer("shortsShelfRenderer"))
        assertTrue(filterEngine.isShortsRenderer("reelShelfRenderer"))
        assertFalse(filterEngine.isShortsRenderer("videoRenderer"))
        assertFalse(filterEngine.isShortsRenderer("gridVideoRenderer"))
    }

    @Test
    fun `sanitizeUrl converts shorts url to standard watch url`() {
        val shortUrl = "https://www.youtube.com/shorts/dQw4w9WgXcQ?feature=share"
        val result = filterEngine.sanitizeUrl(shortUrl)

        assertTrue(result is SanitizedUrlResult.ConvertedShort)
        val converted = result as SanitizedUrlResult.ConvertedShort
        assertEquals("dQw4w9WgXcQ", converted.videoId)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", converted.standardWatchUrl)
    }
}
