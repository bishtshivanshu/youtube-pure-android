package com.puretube.app.core.shorts

import com.puretube.app.domain.model.VideoItem
import java.util.Locale

/**
 * Deterministic multi-stage filter that eradicates YouTube Shorts across:
 * 1. Feed payloads (Home, Subscriptions, Related, Search)
 * 2. Duration & aspect ratio heuristics
 * 3. Title/description/tag heuristics
 * 4. Deep-link URL rewriting
 */
class ShortsFilterEngine(
    private val maxShortDurationSeconds: Long = 65L, // Shorts are nominally <= 60s
    private val strictShortsBlackout: Boolean = true
) {

    private val shortsPatterns = listOf(
        "#shorts",
        "#short",
        "#ytshorts",
        "#youtubeshorts",
        "youtube shorts",
        "yt shorts"
    )

    /**
     * Determines whether a VideoItem represents a Short video.
     * Returns true if it should be purged.
     */
    fun isShortVideo(item: VideoItem): Boolean {
        // 1. Explicit flag
        if (item.isShort) return true

        val titleLower = item.title.lowercase(Locale.ROOT)
        val descLower = item.description.lowercase(Locale.ROOT)

        // 2. Hashtag or marker in title or description
        for (pattern in shortsPatterns) {
            if (titleLower.contains(pattern) || descLower.contains(pattern)) {
                return true
            }
        }

        // 3. Aspect Ratio check: vertical video (height > width, aspect ratio < 1.0)
        // Shorts are always vertical (9:16 ~ 0.5625)
        if (item.aspectRatio < 1.0f && item.durationSeconds <= maxShortDurationSeconds && item.durationSeconds > 0) {
            return true
        }

        // 4. Strict duration & title heuristic: any video <= 60s with typical shorts indicators
        if (strictShortsBlackout && item.durationSeconds in 1..maxShortDurationSeconds) {
            if (item.aspectRatio <= 1.0f || titleLower.contains("short")) {
                return true
            }
        }

        return false
    }

    /**
     * Purges all Shorts from a list of videos.
     */
    fun sanitizeList(items: List<VideoItem>): List<VideoItem> {
        return items.filterNot { isShortVideo(it) }
    }

    /**
     * Inspects raw JSON / InnerTube renderer names to reject Shorts containers early.
     * E.g. reelItemRenderer, shortsShelfRenderer, reelShelfRenderer.
     */
    fun isShortsRenderer(rendererType: String): Boolean {
        val lower = rendererType.lowercase(Locale.ROOT)
        return lower.contains("reel") ||
                lower.contains("shorts") ||
                lower.contains("shortsshelf") ||
                lower.contains("reelshelf")
    }

    /**
     * Rewrites deep links: If a user taps an external link like
     * `https://youtube.com/shorts/{videoId}`, this extracts the videoId
     * so it can be played in standard landscape player without triggering Shorts carousel.
     */
    fun sanitizeUrl(url: String): SanitizedUrlResult {
        if (!url.contains("/shorts/")) {
            return SanitizedUrlResult.StandardUrl(url)
        }
        val videoId = url.substringAfter("/shorts/").substringBefore("?").substringBefore("/")
        return if (videoId.isNotBlank()) {
            SanitizedUrlResult.ConvertedShort(
                videoId = videoId,
                standardWatchUrl = "https://www.youtube.com/watch?v=$videoId"
            )
        } else {
            SanitizedUrlResult.StandardUrl(url)
        }
    }
}

sealed class SanitizedUrlResult {
    data class StandardUrl(val url: String) : SanitizedUrlResult()
    data class ConvertedShort(val videoId: String, val standardWatchUrl: String) : SanitizedUrlResult()
}
