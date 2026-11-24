package com.puretube.app.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.puretube.app.domain.model.VideoQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class PlayerEngineMode {
    YOUTUBE_PLAYER,
    NATIVE_EXOPLAYER
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPositionMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val bufferedPositionMillis: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isBuffering: Boolean = false,
    val currentQuality: String = "1080p (FHD)",
    val availableQualities: List<VideoQuality> = emptyList(),
    val title: String = "",
    val videoId: String = "",
    val isLooping: Boolean = false,
    val isMuted: Boolean = false,
    val captionsEnabled: Boolean = false,
    val ambientModeEnabled: Boolean = true,
    val statsForNerdsVisible: Boolean = false,
    val touchControlsLocked: Boolean = false,
    val sleepTimerMinutes: Int = 0,
    val playerEngineMode: PlayerEngineMode = PlayerEngineMode.YOUTUBE_PLAYER,
    val errorMessage: String? = null
)

@OptIn(UnstableApi::class)
class ExoPlayerManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Linux; Android 16; Pixel 9 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(20000)

    private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var activeYouTubePlayer: YouTubePlayer? = null
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (_uiState.value.playerEngineMode == PlayerEngineMode.NATIVE_EXOPLAYER) {
                    _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                    if (isPlaying) {
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (_uiState.value.playerEngineMode == PlayerEngineMode.NATIVE_EXOPLAYER) {
                    val isBuffering = playbackState == Player.STATE_BUFFERING
                    val duration = if (player.duration > 0) player.duration else 0L
                    _uiState.value = _uiState.value.copy(
                        isBuffering = isBuffering,
                        durationMillis = duration,
                        errorMessage = if (playbackState == Player.STATE_READY) null else _uiState.value.errorMessage
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (_uiState.value.playerEngineMode == PlayerEngineMode.NATIVE_EXOPLAYER) {
                    _uiState.value = _uiState.value.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = error.localizedMessage ?: "Playback error: ${error.errorCodeName}"
                    )
                    // Automatically recover with fallback stream
                    val fallbackUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                    val currentUri = player.currentMediaItem?.localConfiguration?.uri?.toString()
                    if (currentUri != fallbackUrl) {
                        val mediaItem = MediaItem.fromUri(fallbackUrl)
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.playWhenReady = true
                    }
                }
            }
        })
    }

    fun attachYouTubePlayer(youTubePlayer: YouTubePlayer) {
        this.activeYouTubePlayer = youTubePlayer
        val currentId = _uiState.value.videoId
        if (currentId.isNotBlank()) {
            youTubePlayer.loadVideo(currentId, 0f)
        }
    }

    fun detachYouTubePlayer() {
        this.activeYouTubePlayer = null
    }

    fun loadVideoInYouTubePlayer(videoId: String) {
        if (_uiState.value.videoId != videoId) {
            _uiState.value = _uiState.value.copy(
                videoId = videoId,
                errorMessage = null,
                isBuffering = true
            )
            activeYouTubePlayer?.loadVideo(videoId, 0f)
        }
    }

    fun updatePlaybackState(isPlaying: Boolean, isBuffering: Boolean) {
        _uiState.value = _uiState.value.copy(
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            errorMessage = if (isPlaying) null else _uiState.value.errorMessage
        )
    }

    fun updateCurrentPosition(positionMillis: Long) {
        _uiState.value = _uiState.value.copy(currentPositionMillis = positionMillis)
    }

    fun updateDuration(durationMillis: Long) {
        _uiState.value = _uiState.value.copy(durationMillis = durationMillis)
    }

    fun setErrorMessage(msg: String?) {
        _uiState.value = _uiState.value.copy(errorMessage = msg, isBuffering = false)
    }

    fun playStream(
        videoId: String,
        title: String,
        streamUrl: String,
        qualities: List<VideoQuality> = emptyList(),
        initialQuality: String = "1080p (FHD)"
    ) {
        _uiState.value = _uiState.value.copy(
            videoId = videoId,
            title = title,
            availableQualities = qualities,
            currentQuality = initialQuality,
            errorMessage = null,
            isBuffering = true
        )

        if (_uiState.value.playerEngineMode == PlayerEngineMode.YOUTUBE_PLAYER) {
            activeYouTubePlayer?.loadVideo(videoId, 0f)
        } else {
            val mediaItem = MediaItem.fromUri(streamUrl)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
        }
    }

    fun togglePlayPause() {
        if (_uiState.value.playerEngineMode == PlayerEngineMode.YOUTUBE_PLAYER) {
            if (_uiState.value.isPlaying) {
                activeYouTubePlayer?.pause()
                _uiState.value = _uiState.value.copy(isPlaying = false)
            } else {
                activeYouTubePlayer?.play()
                _uiState.value = _uiState.value.copy(isPlaying = true)
            }
        } else {
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMillis: Long) {
        if (_uiState.value.playerEngineMode == PlayerEngineMode.YOUTUBE_PLAYER) {
            activeYouTubePlayer?.seekTo(positionMillis / 1000f)
            _uiState.value = _uiState.value.copy(currentPositionMillis = positionMillis)
        } else {
            player.seekTo(positionMillis)
            _uiState.value = _uiState.value.copy(currentPositionMillis = positionMillis)
        }
    }

    fun seekRelative(offsetMillis: Long) {
        val target = (_uiState.value.currentPositionMillis + offsetMillis).coerceIn(0L, _uiState.value.durationMillis.coerceAtLeast(0L))
        seekTo(target)
    }

    fun setPlaybackSpeed(speed: Float) {
        if (_uiState.value.playerEngineMode == PlayerEngineMode.YOUTUBE_PLAYER) {
            val rate = when {
                speed <= 0.25f -> PlayerConstants.PlaybackRate.RATE_0_25
                speed <= 0.5f -> PlayerConstants.PlaybackRate.RATE_0_5
                speed <= 0.75f -> PlayerConstants.PlaybackRate.RATE_0_75
                speed <= 1.0f -> PlayerConstants.PlaybackRate.RATE_1
                speed <= 1.25f -> PlayerConstants.PlaybackRate.RATE_1_25
                speed <= 1.5f -> PlayerConstants.PlaybackRate.RATE_1_5
                speed <= 1.75f -> PlayerConstants.PlaybackRate.RATE_1_75
                else -> PlayerConstants.PlaybackRate.RATE_2
            }
            activeYouTubePlayer?.setPlaybackRate(rate)
        } else {
            player.playbackParameters = PlaybackParameters(speed)
        }
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun switchQuality(quality: VideoQuality) {
        _uiState.value = _uiState.value.copy(currentQuality = quality.label, errorMessage = null)
        if (_uiState.value.playerEngineMode == PlayerEngineMode.NATIVE_EXOPLAYER) {
            val currentPos = player.currentPosition
            val wasPlaying = player.isPlaying
            val mediaItem = MediaItem.fromUri(quality.url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.seekTo(currentPos)
            player.playWhenReady = wasPlaying
        }
    }

    fun toggleLoop() {
        val newLoopState = !_uiState.value.isLooping
        player.repeatMode = if (newLoopState) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _uiState.value = _uiState.value.copy(isLooping = newLoopState)
    }

    fun toggleMute() {
        val newMute = !_uiState.value.isMuted
        if (_uiState.value.playerEngineMode == PlayerEngineMode.YOUTUBE_PLAYER) {
            if (newMute) activeYouTubePlayer?.mute() else activeYouTubePlayer?.unMute()
        } else {
            player.volume = if (newMute) 0f else 1f
        }
        _uiState.value = _uiState.value.copy(isMuted = newMute)
    }

    fun toggleCaptions() {
        _uiState.value = _uiState.value.copy(captionsEnabled = !_uiState.value.captionsEnabled)
    }

    fun toggleAmbientMode() {
        _uiState.value = _uiState.value.copy(ambientModeEnabled = !_uiState.value.ambientModeEnabled)
    }

    fun toggleStatsForNerds() {
        _uiState.value = _uiState.value.copy(statsForNerdsVisible = !_uiState.value.statsForNerdsVisible)
    }

    fun toggleTouchLock() {
        _uiState.value = _uiState.value.copy(touchControlsLocked = !_uiState.value.touchControlsLocked)
    }

    fun setPlayerEngineMode(mode: PlayerEngineMode) {
        if (mode == PlayerEngineMode.YOUTUBE_PLAYER) {
            player.pause()
            val id = _uiState.value.videoId
            if (id.isNotBlank()) {
                activeYouTubePlayer?.loadVideo(id, (_uiState.value.currentPositionMillis / 1000f))
            }
        } else {
            activeYouTubePlayer?.pause()
            val qual = _uiState.value.availableQualities.firstOrNull()?.url ?: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
            val mediaItem = MediaItem.fromUri(qual)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.seekTo(_uiState.value.currentPositionMillis)
            player.playWhenReady = true
        }
        _uiState.value = _uiState.value.copy(playerEngineMode = mode)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(sleepTimerMinutes = minutes)
        if (minutes > 0) {
            sleepTimerJob = coroutineScope.launch {
                delay(minutes * 60 * 1000L)
                if (_uiState.value.playerEngineMode == PlayerEngineMode.YOUTUBE_PLAYER) {
                    activeYouTubePlayer?.pause()
                } else {
                    player.pause()
                }
                _uiState.value = _uiState.value.copy(sleepTimerMinutes = 0, isPlaying = false)
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            while (isActive) {
                val current = player.currentPosition.coerceAtLeast(0L)
                val duration = player.duration.coerceAtLeast(0L)
                val buffered = player.bufferedPosition.coerceAtLeast(0L)
                _uiState.value = _uiState.value.copy(
                    currentPositionMillis = current,
                    durationMillis = duration,
                    bufferedPositionMillis = buffered
                )
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracker()
        sleepTimerJob?.cancel()
        activeYouTubePlayer = null
        player.release()
    }
}
