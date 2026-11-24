package com.puretube.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puretube.app.data.repository.YouTubeRepository
import com.puretube.app.domain.model.VideoItem
import com.puretube.app.ui.components.VideoCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: YouTubeRepository,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val categories = remember {
        listOf("All", "Music", "Podcasts", "Technology", "Gaming", "News", "Science", "Nature", "Coding")
    }
    var selectedCategory by remember { mutableStateOf("All") }
    val allVideos by repository.getHomeFeed().collectAsState(initial = emptyList())

    // Category filter logic
    val filteredVideos = remember(selectedCategory, allVideos) {
        if (selectedCategory == "All") {
            allVideos
        } else {
            allVideos.filter {
                it.title.contains(selectedCategory, ignoreCase = true) ||
                it.channelTitle.contains(selectedCategory, ignoreCase = true) ||
                it.description.contains(selectedCategory, ignoreCase = true)
            }.ifEmpty { allVideos } // Fallback to all videos if no strict category matches
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleFilled,
                            contentDescription = "PureTube Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PureTube",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // High-contrast "Zero Shorts" pill badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(top = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Shorts blocked",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Zero Shorts",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Cast device searching...")
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Cast,
                            contentDescription = "Cast",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("No new notifications")
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Feed refreshed • 100% Shorts Filtered")
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Horizontal Category Filter Pills (YouTube style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEach { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                            selectedLabelColor = MaterialTheme.colorScheme.background
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Video List
            if (filteredVideos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = filteredVideos, key = { it.id }) { video ->
                        VideoCard(
                            video = video,
                            onClick = { onVideoClick(video) },
                            onSaveToLibrary = { savedVideo ->
                                coroutineScope.launch {
                                    repository.toggleSaveVideo(savedVideo, false)
                                    snackbarHostState.showSnackbar("Saved to Library")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
