package com.puretube.app

import android.app.Application
import androidx.room.Room
import com.puretube.app.core.player.ExoPlayerManager
import com.puretube.app.core.security.AuthInterceptor
import com.puretube.app.core.security.GoogleAuthManager
import com.puretube.app.core.security.SecureStorageManager
import com.puretube.app.core.shorts.ShortsFilterEngine
import com.puretube.app.data.api.YouTubeApiService
import com.puretube.app.data.db.PureTubeDatabase
import com.puretube.app.data.repository.YouTubeRepository
import com.puretube.app.data.repository.YouTubeRepositoryImpl
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PureTubeApplication : Application() {

    lateinit var secureStorage: SecureStorageManager
        private set
    lateinit var googleAuthManager: GoogleAuthManager
        private set
    lateinit var database: PureTubeDatabase
        private set
    lateinit var shortsFilterEngine: ShortsFilterEngine
        private set
    lateinit var youtubeRepository: YouTubeRepository
        private set
    lateinit var playerManager: ExoPlayerManager
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Hardware-backed secure storage
        secureStorage = SecureStorageManager(this)

        // 2. Google Identity & Credential Manager
        googleAuthManager = GoogleAuthManager(this, secureStorage)

        // 3. Shorts Elimination Engine
        shortsFilterEngine = ShortsFilterEngine(
            maxShortDurationSeconds = 65L,
            strictShortsBlackout = true
        )

        // 4. Secure OkHttpClient with TLS 1.3 and AuthInterceptor
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(secureStorage))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        // 5. Room Database
        database = Room.databaseBuilder(
            this,
            PureTubeDatabase::class.java,
            "puretube.db"
        ).fallbackToDestructiveMigration().build()

        // 6. Data API Service & Repository
        val apiService = YouTubeApiService(okHttpClient, shortsFilterEngine)
        youtubeRepository = YouTubeRepositoryImpl(
            apiService = apiService,
            database = database,
            secureStorage = secureStorage,
            shortsFilterEngine = shortsFilterEngine
        )

        // 7. Media3 ExoPlayer Engine
        playerManager = ExoPlayerManager(this)
    }

    override fun onTerminate() {
        playerManager.release()
        super.onTerminate()
    }
}
