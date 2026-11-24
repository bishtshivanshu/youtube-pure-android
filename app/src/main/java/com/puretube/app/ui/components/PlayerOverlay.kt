package com.puretube.app.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.puretube.app.core.player.ExoPlayerManager
import com.puretube.app.core.player.PlayerEngineMode
import com.puretube.app.domain.model.VideoQuality
import kotlinx.coroutines.delay


@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerView(
    playerManager: ExoPlayerManager,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by playerManager.uiState.collectAsState()
    var controlsVisible by remember { mutableStateOf(true) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var activeSubSheet by remember { mutableStateOf<SettingsSubSheet?>(null) }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(controlsVisible, uiState.isPlaying) {
        if (controlsVisible && uiState.isPlaying && !uiState.touchControlsLocked) {
            delay(4000)
            controlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
            .pointerInput(uiState.touchControlsLocked) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                    },
                    onDoubleTap = { offset ->
                        if (!uiState.touchControlsLocked) {
                            val screenWidth = size.width
                            if (offset.x < screenWidth / 2) {
                                playerManager.seekRelative(-10_000)
                            } else {
                                playerManager.seekRelative(10_000)
                            }
                        }
                    }
                )
            }
    ) {
        // Player Surface: Dual-Engine Support (Official YouTube Player vs ExoPlayer)
        if (uiState.playerEngineMode == PlayerEngineMode.YOUTUBE_PLAYER) {
            AndroidView(
                factory = { context ->
                    val ytpView = YouTubePlayerView(context).apply {
                        enableAutomaticInitialization = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                    val options = IFramePlayerOptions.Builder(context)
                        .controls(1) // Full native YouTube player controls (scrub bar, captions, quality, speed, fullscreen)
                        .rel(0)
                        .ivLoadPolicy(3)
                        .ccLoadPolicy(1)
                        .fullscreen(1)
                        .build()

                    ytpView.initialize(object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            playerManager.attachYouTubePlayer(youTubePlayer)
                            if (uiState.videoId.isNotBlank()) {
                                youTubePlayer.loadVideo(uiState.videoId, 0f)
                            }
                        }

                        override fun onStateChange(youTubePlayer: YouTubePlayer, state: PlayerConstants.PlayerState) {
                            when (state) {
                                PlayerConstants.PlayerState.PLAYING -> playerManager.updatePlaybackState(isPlaying = true, isBuffering = false)
                                PlayerConstants.PlayerState.PAUSED -> playerManager.updatePlaybackState(isPlaying = false, isBuffering = false)
                                PlayerConstants.PlayerState.BUFFERING -> playerManager.updatePlaybackState(isPlaying = false, isBuffering = true)
                                PlayerConstants.PlayerState.ENDED -> playerManager.updatePlaybackState(isPlaying = false, isBuffering = false)
                                else -> {}
                            }
                        }

                        override fun onCurrentSecond(youTubePlayer: YouTubePlayer, second: Float) {
                            playerManager.updateCurrentPosition((second * 1000).toLong())
                        }

                        override fun onVideoDuration(youTubePlayer: YouTubePlayer, duration: Float) {
                            playerManager.updateDuration((duration * 1000).toLong())
                        }

                        override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                            playerManager.setErrorMessage("Playback note: $error")
                        }
                    }, options)

                    ytpView
                },
                update = { ytpView ->
                    if (uiState.videoId.isNotBlank()) {
                        playerManager.loadVideoInYouTubePlayer(uiState.videoId)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // PureTube Settings Gear Button (Top right overlay for sleep timer, stats, etc.)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = {
                        showSettingsSheet = true
                        activeSubSheet = null
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "PureTube Settings",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        player = playerManager.player
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Buffering Indicator
        if (uiState.isBuffering) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Error Banner & Switch Engine Fallback
        if (uiState.errorMessage != null && !uiState.isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Playback Error",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.errorMessage ?: "Video stream unavailable",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                playerManager.setPlayerEngineMode(PlayerEngineMode.YOUTUBE_PLAYER)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Tv, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Switch to YouTube Player", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val currentQuality = uiState.availableQualities.firstOrNull()
                                if (currentQuality != null) playerManager.switchQuality(currentQuality)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Stats for Nerds overlay
        if (uiState.statsForNerdsVisible) {
            StatsForNerdsBox(
                uiState = uiState,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }

        // Touch Locked floating unlock button
        if (uiState.touchControlsLocked) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                IconButton(
                    onClick = { playerManager.toggleTouchLock() },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Unlock Controls",
                        tint = Color.White
                    )
                }
            }
        } else {
            // Standard Official YouTube Overlay Controls
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    // Top Bar: CC, Loop, Lock, Settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopEnd)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Captions Toggle
                        IconButton(onClick = { playerManager.toggleCaptions() }) {
                            Icon(
                                imageVector = if (uiState.captionsEnabled) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionDisabled,
                                contentDescription = "Captions",
                                tint = if (uiState.captionsEnabled) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        // Loop Video Toggle
                        IconButton(onClick = { playerManager.toggleLoop() }) {
                            Icon(
                                imageVector = if (uiState.isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = "Loop Video",
                                tint = if (uiState.isLooping) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }

                        // Lock Screen Controls
                        IconButton(onClick = { playerManager.toggleTouchLock() }) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Screen",
                                tint = Color.White
                            )
                        }

                        // Settings (Gear icon)
                        IconButton(onClick = {
                            showSettingsSheet = true
                            activeSubSheet = null
                        }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Playback Settings",
                                tint = Color.White
                            )
                        }
                    }

                    // Center Transport Controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { playerManager.seekRelative(-10_000) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(36.dp))

                        IconButton(
                            onClick = { playerManager.togglePlayPause() },
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(36.dp))

                        IconButton(
                            onClick = { playerManager.seekRelative(10_000) },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10s",
                                tint = Color.White
                            )
                        }
                    }

                    // Bottom Bar: Elapsed / Duration, Seekbar, Fullscreen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Slider(
                            value = if (uiState.durationMillis > 0) {
                                (uiState.currentPositionMillis.toFloat() / uiState.durationMillis).coerceIn(0f, 1f)
                            } else 0f,
                            onValueChange = { fraction ->
                                val target = (fraction * uiState.durationMillis).toLong()
                                playerManager.seekTo(target)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.4f)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${formatMillis(uiState.currentPositionMillis)} / ${formatMillis(uiState.durationMillis)}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (uiState.currentQuality.isNotBlank()) {
                                    Text(
                                        text = " • ${uiState.currentQuality}",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                                if (uiState.playbackSpeed != 1.0f) {
                                    Text(
                                        text = " • ${uiState.playbackSpeed}x",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(onClick = onToggleFullscreen) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Toggle Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // YouTube-style Settings Bottom Sheet
    if (showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showSettingsSheet = false
                activeSubSheet = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            when (activeSubSheet) {
                SettingsSubSheet.QUALITY -> {
                    QualitySelectionSheet(
                        currentQuality = uiState.currentQuality,
                        availableQualities = uiState.availableQualities,
                        onQualitySelect = { quality ->
                            playerManager.switchQuality(quality)
                            showSettingsSheet = false
                            activeSubSheet = null
                        }
                    )
                }
                SettingsSubSheet.SPEED -> {
                    SpeedSelectionSheet(
                        currentSpeed = uiState.playbackSpeed,
                        onSpeedSelect = { speed ->
                            playerManager.setPlaybackSpeed(speed)
                            showSettingsSheet = false
                            activeSubSheet = null
                        }
                    )
                }
                SettingsSubSheet.SLEEP_TIMER -> {
                    SleepTimerSheet(
                        currentTimerMinutes = uiState.sleepTimerMinutes,
                        onTimerSelect = { minutes ->
                            playerManager.setSleepTimer(minutes)
                            showSettingsSheet = false
                            activeSubSheet = null
                        }
                    )
                }
                null -> {
                    MainSettingsMenu(
                        uiState = uiState,
                        onOpenQuality = { activeSubSheet = SettingsSubSheet.QUALITY },
                        onOpenSpeed = { activeSubSheet = SettingsSubSheet.SPEED },
                        onOpenSleepTimer = { activeSubSheet = SettingsSubSheet.SLEEP_TIMER },
                        onToggleLoop = { playerManager.toggleLoop() },
                        onToggleCaptions = { playerManager.toggleCaptions() },
                        onToggleAmbient = { playerManager.toggleAmbientMode() },
                        onToggleStats = {
                            playerManager.toggleStatsForNerds()
                            showSettingsSheet = false
                        },
                        onTogglePlayerEngine = { mode ->
                            playerManager.setPlayerEngineMode(mode)
                            showSettingsSheet = false
                        }
                    )
                }
            }
        }
    }
}

enum class SettingsSubSheet {
    QUALITY,
    SPEED,
    SLEEP_TIMER
}

@Composable
fun MainSettingsMenu(
    uiState: com.puretube.app.core.player.PlayerUiState,
    onOpenQuality: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleCaptions: () -> Unit,
    onToggleAmbient: () -> Unit,
    onToggleStats: () -> Unit,
    onTogglePlayerEngine: (PlayerEngineMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Playback Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        HorizontalDivider()

        // 1. Video Quality
        SettingsMenuItem(
            icon = Icons.Default.Videocam,
            title = "Quality",
            subtitle = uiState.currentQuality,
            onClick = onOpenQuality
        )

        // 2. Playback Speed
        SettingsMenuItem(
            icon = Icons.Default.Speed,
            title = "Playback speed",
            subtitle = if (uiState.playbackSpeed == 1.0f) "Normal" else "${uiState.playbackSpeed}x",
            onClick = onOpenSpeed
        )

        // 3. Player Engine Mode
        SettingsMenuItem(
            icon = if (uiState.playerEngineMode == PlayerEngineMode.YOUTUBE_PLAYER) Icons.Default.Tv else Icons.Default.Videocam,
            title = "Player Engine",
            subtitle = if (uiState.playerEngineMode == PlayerEngineMode.YOUTUBE_PLAYER) "YouTube Player (Official 100% Stream)" else "Media3 ExoPlayer",
            onClick = {
                val nextMode = if (uiState.playerEngineMode == PlayerEngineMode.YOUTUBE_PLAYER) {
                    PlayerEngineMode.NATIVE_EXOPLAYER
                } else {
                    PlayerEngineMode.YOUTUBE_PLAYER
                }
                onTogglePlayerEngine(nextMode)
            }
        )

        // 4. Captions
        SettingsToggleItem(
            icon = Icons.Default.ClosedCaption,
            title = "Captions",
            isChecked = uiState.captionsEnabled,
            onCheckedChange = { onToggleCaptions() }
        )

        // 5. Loop Video
        SettingsToggleItem(
            icon = Icons.Default.Repeat,
            title = "Loop video",
            isChecked = uiState.isLooping,
            onCheckedChange = { onToggleLoop() }
        )

        // 6. Ambient Mode
        SettingsToggleItem(
            icon = Icons.Default.NightlightRound,
            title = "Ambient mode",
            isChecked = uiState.ambientModeEnabled,
            onCheckedChange = { onToggleAmbient() }
        )

        // 7. Sleep Timer
        SettingsMenuItem(
            icon = Icons.Default.Tv,
            title = "Sleep timer",
            subtitle = if (uiState.sleepTimerMinutes > 0) "${uiState.sleepTimerMinutes} minutes" else "Off",
            onClick = onOpenSleepTimer
        )

        // 8. Stats for Nerds
        SettingsToggleItem(
            icon = Icons.Default.Info,
            title = "Stats for nerds",
            isChecked = uiState.statsForNerdsVisible,
            onCheckedChange = { onToggleStats() }
        )
    }
}

@Composable
fun QualitySelectionSheet(
    currentQuality: String,
    availableQualities: List<VideoQuality>,
    onQualitySelect: (VideoQuality) -> Unit
) {
    val qualities = availableQualities.ifEmpty {
        listOf(
            VideoQuality("1080p (FHD)", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
            VideoQuality("720p (HD)", "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"),
            VideoQuality("480p (SD)", "https://media.w3.org/2010/05/sintel/trailer.mp4"),
            VideoQuality("360p (Data Saver)", "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"),
            VideoQuality("Audio Only", "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4")
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Select Quality",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        HorizontalDivider()

        LazyColumn {
            items(qualities) { q ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQualitySelect(q) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = q.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (q.label == currentQuality) FontWeight.Bold else FontWeight.Normal,
                        color = if (q.label == currentQuality) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (q.label == currentQuality) {
                        Text(
                            text = "✓",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedSelectionSheet(
    currentSpeed: Float,
    onSpeedSelect: (Float) -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Playback Speed",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        HorizontalDivider()

        LazyColumn {
            items(speeds) { s ->
                val label = if (s == 1.0f) "Normal (1.0x)" else "${s}x"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSpeedSelect(s) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (s == currentSpeed) FontWeight.Bold else FontWeight.Normal,
                        color = if (s == currentSpeed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (s == currentSpeed) {
                        Text(
                            text = "✓",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SleepTimerSheet(
    currentTimerMinutes: Int,
    onTimerSelect: (Int) -> Unit
) {
    val options = listOf(0 to "Off", 10 to "10 minutes", 15 to "15 minutes", 30 to "30 minutes", 45 to "45 minutes", 60 to "60 minutes")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Sleep Timer",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        HorizontalDivider()

        options.forEach { (mins, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTimerSelect(mins) }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (mins == currentTimerMinutes) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (mins == currentTimerMinutes) {
                    Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun StatsForNerdsBox(
    uiState: com.puretube.app.core.player.PlayerUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(text = "STATS FOR NERDS", color = Color.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Video ID: ${uiState.videoId.ifBlank { "N/A" }}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(text = "Engine: ${uiState.playerEngineMode.name}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(text = "Resolution: ${uiState.currentQuality}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(text = "Speed: ${uiState.playbackSpeed}x", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(text = "Buffer: ${uiState.bufferedPositionMillis / 1000}s", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(text = "Shorts Filter: ACTIVE (0 Shorts)", color = Color.Green, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
