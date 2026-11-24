package com.puretube.app.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val durationFormatted: String,
    val playbackPositionMillis: Long = 0L,
    val lastWatchedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_videos")
data class SavedVideoEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelTitle: String,
    val thumbnailUrl: String,
    val durationFormatted: String,
    val savedTimestamp: Long = System.currentTimeMillis(),
    val isLiked: Boolean = true
)

@Dao
interface WatchHistoryDao {
    @Query("SELECT * FROM watch_history ORDER BY lastWatchedTimestamp DESC")
    fun getAllHistory(): Flow<List<WatchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE videoId = :videoId")
    suspend fun delete(videoId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
}

@Dao
interface SavedVideoDao {
    @Query("SELECT * FROM saved_videos ORDER BY savedTimestamp DESC")
    fun getAllSavedVideos(): Flow<List<SavedVideoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveVideo(video: SavedVideoEntity)

    @Query("DELETE FROM saved_videos WHERE videoId = :videoId")
    suspend fun removeVideo(videoId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_videos WHERE videoId = :videoId)")
    fun isSaved(videoId: String): Flow<Boolean>
}

@Database(
    entities = [WatchHistoryEntity::class, SavedVideoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PureTubeDatabase : RoomDatabase() {
    abstract fun watchHistoryDao(): WatchHistoryDao
    abstract fun savedVideoDao(): SavedVideoDao
}
