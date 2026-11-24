package com.puretube.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.puretube.app.core.player.ExoPlayerManager
import com.puretube.app.data.repository.YouTubeRepository
import com.puretube.app.domain.model.VideoItem
import com.puretube.app.ui.components.VideoCard
import com.puretube.app.ui.components.VideoPlayerView
import kotlinx.coroutines.launch

@Composable
fun PlayerScreen(
    video: VideoItem,
    playerManager: ExoPlayerManager,
    repository: YouTubeRepository,
    onRelatedVideoClick: (VideoItem) -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLiked by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    var isSubscribed by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var isAudioOnlyMode by remember { mutableStateOf(false) }

    val relatedVideos by repository.getHomeFeed().collectAsState(initial = emptyList())
    val isSavedInDb by repository.isVideoSaved(video.id).collectAsState(initial = false)

    LaunchedEffect(isSavedInDb) {
        isSaved = isSavedInDb
    }

    // Load stream and record watch history
    LaunchedEffect(video.id) {
        repository.recordWatchHistory(video)
        repository.getStreamInfo(video.id).collect { streamInfo ->
            playerManager.playStream(
                videoId = video.id,
                title = video.title,
                streamUrl = streamInfo.streamUrl,
                qualities = streamInfo.qualities,
                initialQuality = streamInfo.qualities.firstOrNull()?.label ?: "1080p"
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Sticky Video Player
        VideoPlayerView(
            playerManager = playerManager,
            onToggleFullscreen = onToggleFullscreen
        )

        // 2. Video Details & Related Videos (Scrollable)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Views & Date (Clickable description card)
                val metaText = buildString {
                    if (video.viewCountFormatted.isNotBlank()) append("${video.viewCountFormatted} • ")
                    if (video.publishedAtFormatted.isNotBlank()) append(video.publishedAtFormatted)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = metaText.ifBlank { "YouTube Video • PureTube No-Shorts Edition" },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isDescriptionExpanded) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = video.description.ifBlank { "Enjoy full-length, uninterrupted content. All recommendations, reels, and Shorts have been strictly removed." },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                            )
                        } else {
                            Text(
                                text = "...more",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Channel Row & Subscribe
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!video.channelAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = video.channelAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = video.channelTitle.take(1),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.channelTitle,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Verified Channel",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    Button(
                        onClick = { isSubscribed = !isSubscribed },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = if (isSubscribed) "Subscribed" else "Subscribe",
                            color = if (isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                            fontSize = 13.sp
                        )
                    }
                }

                // Action Bar: Like, Dislike, Share, Save, Background Audio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like & Dislike Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        isLiked = !isLiked
                                        if (isLiked) isDisliked = false
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                    contentDescription = "Like",
                                    tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isLiked) "Liked" else "Like", fontSize = 12.sp)
                            }

                            Text(" | ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))

                            Icon(
                                imageVector = if (isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                contentDescription = "Dislike",
                                tint = if (isDisliked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        isDisliked = !isDisliked
                                        if (isDisliked) isLiked = false
                                    }
                            )
                        }
                    }

                    // Share Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=${video.id}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Video"))
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 12.sp)
                        }
                    }

                    // Save Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            coroutineScope.launch {
                                repository.toggleSaveVideo(video, isLiked)
                                isSaved = !isSaved
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Save",
                                tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isSaved) "Saved" else "Save", fontSize = 12.sp)
                        }
                    }

                    // Audio Mode
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isAudioOnlyMode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            isAudioOnlyMode = !isAudioOnlyMode
                            val audioQuality = playerManager.uiState.value.availableQualities.find {
                                it.label.contains("Audio", ignoreCase = true)
                            }
                            if (isAudioOnlyMode && audioQuality != null) {
                                playerManager.switchQuality(audioQuality)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Headphones, contentDescription = "Audio Mode", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isAudioOnlyMode) "Audio On" else "Audio", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Up Next Header (Guaranteed No Shorts)
                Text(
                    text = "Up Next (100% Shorts Filtered)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Related Videos List (Excludes current video and guarantees 0 Shorts)
            val filteredRelated = relatedVideos.filter { it.id != video.id && !it.isShort }
            items(filteredRelated) { relatedVideo ->
                VideoCard(
                    video = relatedVideo,
                    onClick = { onRelatedVideoClick(relatedVideo) },
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}
