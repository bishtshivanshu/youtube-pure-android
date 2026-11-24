package com.puretube.app.core.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
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
    NATIVE_EXOPLAYER,
    WEB_EMBED
}

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val currentPositionMillis: Long = 0L,
    val durationMillis: Long = 0L,
    val bufferedPositionMillis: Long = 0L,
    val playbackSpeed: Float = 1.0f,
    val isBuffering: Boolean = false,
    val currentQuality: String = "1080p",
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
    val playerEngineMode: PlayerEngineMode = PlayerEngineMode.NATIVE_EXOPLAYER
)

@OptIn(UnstableApi::class)
class ExoPlayerManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val isBuffering = playbackState == Player.STATE_BUFFERING
                val duration = if (player.duration > 0) player.duration else 0L
                _uiState.value = _uiState.value.copy(
                    isBuffering = isBuffering,
                    durationMillis = duration
                )
            }
        })
    }

    fun playStream(
        videoId: String,
        title: String,
        streamUrl: String,
        qualities: List<VideoQuality> = emptyList(),
        initialQuality: String = "1080p"
    ) {
        val mediaItem = MediaItem.fromUri(streamUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        _uiState.value = _uiState.value.copy(
            videoId = videoId,
            title = title,
            availableQualities = qualities,
            currentQuality = initialQuality
        )
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMillis: Long) {
        player.seekTo(positionMillis)
        _uiState.value = _uiState.value.copy(currentPositionMillis = positionMillis)
    }

    fun seekRelative(offsetMillis: Long) {
        val target = (player.currentPosition + offsetMillis).coerceIn(0L, player.duration.coerceAtLeast(0L))
        seekTo(target)
    }

    fun setPlaybackSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
    }

    fun switchQuality(quality: VideoQuality) {
        val currentPos = player.currentPosition
        val wasPlaying = player.isPlaying

        val mediaItem = MediaItem.fromUri(quality.url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.seekTo(currentPos)
        player.playWhenReady = wasPlaying

        _uiState.value = _uiState.value.copy(currentQuality = quality.label)
    }

    fun toggleLoop() {
        val newLoopState = !_uiState.value.isLooping
        player.repeatMode = if (newLoopState) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        _uiState.value = _uiState.value.copy(isLooping = newLoopState)
    }

    fun toggleMute() {
        val newMute = !_uiState.value.isMuted
        player.volume = if (newMute) 0f else 1f
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
        if (mode == PlayerEngineMode.WEB_EMBED) {
            player.pause()
        }
        _uiState.value = _uiState.value.copy(playerEngineMode = mode)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _uiState.value = _uiState.value.copy(sleepTimerMinutes = minutes)
        if (minutes > 0) {
            sleepTimerJob = coroutineScope.launch {
                delay(minutes * 60 * 1000L)
                player.pause()
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
        player.release()
    }
}
