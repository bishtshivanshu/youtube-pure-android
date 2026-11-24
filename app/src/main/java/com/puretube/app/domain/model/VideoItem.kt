package com.puretube.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoItem(
    val id: String,
    val title: String,
    val channelId: String,
    val channelTitle: String,
    val channelAvatarUrl: String? = null,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val durationFormatted: String,
    val viewCount: Long = 0,
    val viewCountFormatted: String = "",
    val publishedAtFormatted: String = "",
    val description: String = "",
    val isShort: Boolean = false,
    val aspectRatio: Float = 1.778f // Default 16:9 landscape
)

@Serializable
data class ChannelItem(
    val id: String,
    val title: String,
    val avatarUrl: String,
    val subscriberCountFormatted: String = "",
    val isSubscribed: Boolean = false
)

@Serializable
data class StreamInfo(
    val videoId: String,
    val title: String,
    val streamUrl: String,
    val isHls: Boolean = false,
    val isDash: Boolean = false,
    val audioOnlyUrl: String? = null,
    val qualities: List<VideoQuality> = emptyList()
)

@Serializable
data class VideoQuality(
    val label: String, // e.g. "1080p", "720p", "480p"
    val url: String,
    val mimeType: String = "video/mp4"
)
