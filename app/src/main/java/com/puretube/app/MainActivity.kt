package com.puretube.app

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.puretube.app.core.shorts.SanitizedUrlResult
import com.puretube.app.domain.model.VideoItem
import com.puretube.app.ui.navigation.Screen
import com.puretube.app.ui.screens.HomeScreen
import com.puretube.app.ui.screens.LibraryScreen
import com.puretube.app.ui.screens.PlayerScreen
import com.puretube.app.ui.screens.SearchScreen
import com.puretube.app.ui.screens.SubscriptionsScreen
import com.puretube.app.ui.theme.PureTubeTheme

class MainActivity : ComponentActivity() {

    private lateinit var app: PureTubeApplication
    private var activeVideo by mutableStateOf<VideoItem?>(null)
    private var isFullscreen by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mandatory modern Edge-to-Edge display in Android 16
        enableEdgeToEdge()

        app = application as PureTubeApplication

        // Handle incoming deep links (e.g. youtube.com/watch?v=... or youtube.com/shorts/...)
        handleIncomingIntent(intent)

        setContent {
            PureTubeTheme {
                PureTubeMainContent()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val data = intent?.data ?: return
        val url = data.toString()
        val sanitized = app.shortsFilterEngine.sanitizeUrl(url)

        val targetVideoId = when (sanitized) {
            is SanitizedUrlResult.ConvertedShort -> sanitized.videoId
            is SanitizedUrlResult.StandardUrl -> {
                if (url.contains("watch?v=")) {
                    url.substringAfter("watch?v=").substringBefore("&")
                } else if (url.contains("youtu.be/")) {
                    url.substringAfter("youtu.be/").substringBefore("?")
                } else null
            }
        }

        if (targetVideoId != null) {
            activeVideo = VideoItem(
                id = targetVideoId,
                title = "Loaded Video",
                channelId = "",
                channelTitle = "YouTube Channel",
                thumbnailUrl = "https://i.ytimg.com/vi/$targetVideoId/hqdefault.jpg",
                durationSeconds = 180,
                durationFormatted = "3:00"
            )
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Enter Picture-in-Picture automatically when leaving app during playback
        if (app.playerManager.player.isPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    @Composable
    private fun PureTubeMainContent() {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val showBottomBar = activeVideo == null && !isFullscreen

        BackHandler(enabled = activeVideo != null) {
            if (isFullscreen) {
                toggleOrientationFullscreen()
            } else {
                activeVideo = null
            }
        }

        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Screen.bottomNavItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isFullscreen) androidx.compose.foundation.layout.PaddingValues() else innerPadding)
            ) {
                // Main Navigation Tabs
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            repository = app.youtubeRepository,
                            onVideoClick = { video -> activeVideo = video },
                            onSearchClick = {
                                navController.navigate(Screen.Search.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    composable(Screen.Subscriptions.route) {
                        SubscriptionsScreen(
                            repository = app.youtubeRepository,
                            onVideoClick = { video -> activeVideo = video }
                        )
                    }

                    composable(Screen.Search.route) {
                        SearchScreen(
                            repository = app.youtubeRepository,
                            onVideoClick = { video -> activeVideo = video },
                            onBackClick = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }

                    composable(Screen.Library.route) {
                        LibraryScreen(
                            repository = app.youtubeRepository,
                            authManager = app.googleAuthManager,
                            onVideoClick = { video -> activeVideo = video }
                        )
                    }
                }

                // Active Video Player Overlay
                activeVideo?.let { currentVideo ->
                    PlayerScreen(
                        video = currentVideo,
                        playerManager = app.playerManager,
                        repository = app.youtubeRepository,
                        onRelatedVideoClick = { nextVideo -> activeVideo = nextVideo },
                        onToggleFullscreen = {
                            toggleOrientationFullscreen()
                        }
                    )
                }
            }
        }
    }

    private fun toggleOrientationFullscreen() {
        isFullscreen = !isFullscreen
        requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
}
