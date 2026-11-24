package com.puretube.app.data.repository

import com.puretube.app.core.security.SecureStorageManager
import com.puretube.app.core.shorts.ShortsFilterEngine
import com.puretube.app.data.api.YouTubeApiService
import com.puretube.app.data.db.PureTubeDatabase
import com.puretube.app.data.db.SavedVideoEntity
import com.puretube.app.data.db.WatchHistoryEntity
import com.puretube.app.domain.model.ChannelItem
import com.puretube.app.domain.model.StreamInfo
import com.puretube.app.domain.model.VideoItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

interface YouTubeRepository {
    fun getHomeFeed(): Flow<List<VideoItem>>
    fun getSubscriptionsFeed(): Flow<List<VideoItem>>
    fun searchVideos(query: String): Flow<List<VideoItem>>
    fun getStreamInfo(videoId: String): Flow<StreamInfo>
    fun getWatchHistory(): Flow<List<WatchHistoryEntity>>
    suspend fun recordWatchHistory(video: VideoItem, positionMillis: Long = 0L)
    fun getSavedVideos(): Flow<List<SavedVideoEntity>>
    suspend fun toggleSaveVideo(video: VideoItem, isLiked: Boolean = true)
    fun isVideoSaved(videoId: String): Flow<Boolean>
}

class YouTubeRepositoryImpl(
    private val apiService: YouTubeApiService,
    private val database: PureTubeDatabase,
    private val secureStorage: SecureStorageManager,
    private val shortsFilterEngine: ShortsFilterEngine
) : YouTubeRepository {

    override fun getHomeFeed(): Flow<List<VideoItem>> = flow {
        val feed = apiService.fetchHomeFeed()
        emit(shortsFilterEngine.sanitizeList(feed))
    }

    override fun getSubscriptionsFeed(): Flow<List<VideoItem>> = flow {
        // Fetches subscription channels uploads and filters out all shorts
        val feed = apiService.fetchHomeFeed() // In full InnerTube/OAuth this points to FEsubscriptions
        emit(shortsFilterEngine.sanitizeList(feed))
    }

    override fun searchVideos(query: String): Flow<List<VideoItem>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
        } else {
            val results = apiService.searchVideos(query)
            emit(shortsFilterEngine.sanitizeList(results))
        }
    }

    override fun getStreamInfo(videoId: String): Flow<StreamInfo> = flow {
        emit(apiService.resolveStreamInfo(videoId))
    }

    override fun getWatchHistory(): Flow<List<WatchHistoryEntity>> {
        return database.watchHistoryDao().getAllHistory()
    }

    override suspend fun recordWatchHistory(video: VideoItem, positionMillis: Long) {
        val entity = WatchHistoryEntity(
            videoId = video.id,
            title = video.title,
            channelTitle = video.channelTitle,
            thumbnailUrl = video.thumbnailUrl,
            durationFormatted = video.durationFormatted,
            playbackPositionMillis = positionMillis,
            lastWatchedTimestamp = System.currentTimeMillis()
        )
        database.watchHistoryDao().insertOrUpdate(entity)
    }

    override fun getSavedVideos(): Flow<List<SavedVideoEntity>> {
        return database.savedVideoDao().getAllSavedVideos()
    }

    override suspend fun toggleSaveVideo(video: VideoItem, isLiked: Boolean) {
        val dao = database.savedVideoDao()
        val entity = SavedVideoEntity(
            videoId = video.id,
            title = video.title,
            channelTitle = video.channelTitle,
            thumbnailUrl = video.thumbnailUrl,
            durationFormatted = video.durationFormatted,
            savedTimestamp = System.currentTimeMillis(),
            isLiked = isLiked
        )
        dao.saveVideo(entity)
    }

    override fun isVideoSaved(videoId: String): Flow<Boolean> {
        return database.savedVideoDao().isSaved(videoId)
    }
}
