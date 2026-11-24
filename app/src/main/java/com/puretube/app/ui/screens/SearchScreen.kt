package com.puretube.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puretube.app.data.repository.YouTubeRepository
import com.puretube.app.domain.model.VideoItem
import com.puretube.app.ui.components.VideoCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    repository: YouTubeRepository,
    onVideoClick: (VideoItem) -> Unit,
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeSearchQuery by remember { mutableStateOf("Android development") }
    var durationFilterLongOnly by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val searchResults by repository.searchVideos(activeSearchQuery).collectAsState(initial = emptyList())

    val filteredResults = remember(searchResults, durationFilterLongOnly) {
        if (durationFilterLongOnly) {
            searchResults.filter { it.durationSeconds >= 240 } // >= 4 minutes
        } else {
            searchResults
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                // Sleek, perfectly proportioned YouTube Search Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button to return to home feed
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Pill-shaped Search Box with clean interior typography
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search YouTube (No Shorts)...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 14.sp
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = {
                                        if (searchQuery.isNotBlank()) {
                                            activeSearchQuery = searchQuery
                                            focusManager.clearFocus()
                                        }
                                    })
                                )
                            }

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Explicit Search Icon Button for easy one-tap search execution
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(40.dp)
                            .clickable {
                                if (searchQuery.isNotBlank()) {
                                    activeSearchQuery = searchQuery
                                    focusManager.clearFocus()
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Quick Filter & Category Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = durationFilterLongOnly,
                        onClick = { durationFilterLongOnly = !durationFilterLongOnly },
                        label = {
                            Text(
                                text = "Duration > 4m",
                                fontSize = 12.sp,
                                fontWeight = if (durationFilterLongOnly) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                val quickTopics = listOf("All", "Kotlin", "Android 16", "Compose", "Podcasts", "Technology", "Music", "Science")
                items(quickTopics) { topic ->
                    val isSelected = activeSearchQuery.equals(topic, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val target = if (topic == "All") "Android development" else topic
                            searchQuery = target
                            activeSearchQuery = target
                        },
                        label = {
                            Text(
                                text = topic,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                            selectedLabelColor = MaterialTheme.colorScheme.background
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Results List
            if (filteredResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Searching \"$activeSearchQuery\" (Zero Shorts)...",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = filteredResults, key = { it.id }) { video ->
                        VideoCard(
                            video = video,
                            onClick = { onVideoClick(video) }
                        )
                    }
                }
            }
        }
    }
}
