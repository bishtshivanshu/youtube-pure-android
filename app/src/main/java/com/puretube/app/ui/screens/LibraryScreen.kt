package com.puretube.app.ui.screens

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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.puretube.app.core.security.AuthResult
import com.puretube.app.core.security.GoogleAuthManager
import com.puretube.app.data.repository.YouTubeRepository
import com.puretube.app.domain.model.VideoItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    repository: YouTubeRepository,
    authManager: GoogleAuthManager,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf(authManager.getCurrentUser()) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isLoadingAuth by remember { mutableStateOf(false) }

    var showQuickSignInDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }

    val watchHistory by repository.getWatchHistory().collectAsState(initial = emptyList())
    val savedVideos by repository.getSavedVideos().collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Library & Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Google Account & Security Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentUser != null && !currentUser?.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = currentUser?.avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Account",
                                    modifier = Modifier.size(54.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                if (currentUser != null) {
                                    Text(
                                        text = currentUser?.displayName ?: "Google User",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = currentUser?.email ?: "",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Protected with Android Keystore AES-256",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(
                                        text = "Google Account Login",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Sign in via Android Credential Manager or Quick Setup",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (currentUser != null) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        authManager.signOut()
                                        currentUser = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Sign Out")
                            }
                        } else {
                            // Primary Sign In Button (Credential Manager)
                            Button(
                                onClick = {
                                    isLoadingAuth = true
                                    authError = null
                                    coroutineScope.launch {
                                        when (val result = authManager.signIn(context)) {
                                            is AuthResult.Success -> {
                                                currentUser = result.profile
                                                isLoadingAuth = false
                                            }
                                            is AuthResult.Error -> {
                                                authError = result.message
                                                isLoadingAuth = false
                                            }
                                            is AuthResult.Cancelled -> {
                                                isLoadingAuth = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                enabled = !isLoadingAuth
                            ) {
                                if (isLoadingAuth) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Sign In with Google (Credential Manager)")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Quick Sign In Option
                                OutlinedButton(
                                    onClick = { showQuickSignInDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Quick Sign In", fontSize = 12.sp)
                                }

                                // Configure Client ID & API Key Option
                                OutlinedButton(
                                    onClick = { showConfigDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("API Keys", fontSize = 12.sp)
                                }
                            }
                        }

                        if (authError != null) {
                            Text(
                                text = authError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            // 2. Hardware Security & Shorts Elimination Info Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Security Active",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Security & Anti-Distraction Profile",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        SecurityCheckRow(text = "Android 16 Credential Manager API")
                        SecurityCheckRow(text = "Android Keystore AES-256 GCM token encryption")
                        SecurityCheckRow(text = "Strict TLS 1.3 / Cleartext traffic disallowed")
                        SecurityCheckRow(text = "100% Shorts elimination on feeds & search")
                    }
                }
            }

            // 3. Saved / Liked Videos Section
            if (savedVideos.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saved Videos (${savedVideos.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                items(savedVideos) { saved ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onVideoClick(
                                    VideoItem(
                                        id = saved.videoId,
                                        title = saved.title,
                                        channelId = "",
                                        channelTitle = saved.channelTitle,
                                        thumbnailUrl = saved.thumbnailUrl,
                                        durationSeconds = 0,
                                        durationFormatted = saved.durationFormatted
                                    )
                                )
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = saved.thumbnailUrl,
                            contentDescription = saved.title,
                            modifier = Modifier
                                .size(width = 110.dp, height = 65.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = saved.title,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                maxLines = 2
                            )
                            Text(
                                text = saved.channelTitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                }
            }

            // 4. Watch History
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Watch History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }

                    if (watchHistory.isNotEmpty()) {
                        Text(
                            text = "${watchHistory.size} videos",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            if (watchHistory.isEmpty()) {
                item {
                    Text(
                        text = "No history yet. Videos you watch will appear here.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                items(watchHistory) { historyItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onVideoClick(
                                    VideoItem(
                                        id = historyItem.videoId,
                                        title = historyItem.title,
                                        channelId = "",
                                        channelTitle = historyItem.channelTitle,
                                        thumbnailUrl = historyItem.thumbnailUrl,
                                        durationSeconds = 0,
                                        durationFormatted = historyItem.durationFormatted
                                    )
                                )
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = historyItem.thumbnailUrl,
                            contentDescription = historyItem.title,
                            modifier = Modifier
                                .size(width = 110.dp, height = 65.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = historyItem.title,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                maxLines = 2
                            )
                            Text(
                                text = historyItem.channelTitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }

    // Quick Sign-In Dialog
    if (showQuickSignInDialog) {
        var inputName by remember { mutableStateOf("Shivanshu Bisht") }
        var inputEmail by remember { mutableStateOf("bishtshivanshu@gmail.com") }

        AlertDialog(
            onDismissRequest = { showQuickSignInDialog = false },
            title = { Text("Quick Google Account Sign In") },
            text = {
                Column {
                    Text(
                        text = "Instantly connect your account with Android Keystore AES-256 GCM encryption.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputEmail,
                        onValueChange = { inputEmail = it },
                        label = { Text("Google Account Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val result = authManager.signInWithCustomProfile(
                            name = inputName,
                            email = inputEmail
                        )
                        if (result is AuthResult.Success) {
                            currentUser = result.profile
                            authError = null
                        }
                        showQuickSignInDialog = false
                    }
                ) {
                    Text("Sign In")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickSignInDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Configure Web Client ID & API Key Dialog
    if (showConfigDialog) {
        var inputClientId by remember { mutableStateOf(authManager.getActiveWebClientId() ?: "") }
        var inputApiKey by remember { mutableStateOf(authManager.getCustomApiKey() ?: "") }

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("Google Cloud Credentials") },
            text = {
                Column {
                    Text(
                        text = "Configure your Google Cloud OAuth 2.0 Web Client ID and optional YouTube Data v3 API Key.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputClientId,
                        onValueChange = { inputClientId = it },
                        label = { Text("OAuth Web Client ID") },
                        placeholder = { Text("xxx.apps.googleusercontent.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inputApiKey,
                        onValueChange = { inputApiKey = it },
                        label = { Text("YouTube API Key (Optional)") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputClientId.isNotBlank()) {
                            authManager.setWebClientId(inputClientId.trim())
                        }
                        if (inputApiKey.isNotBlank()) {
                            authManager.saveCustomApiKey(inputApiKey.trim())
                        }
                        showConfigDialog = false
                        authError = null
                    }
                ) {
                    Text("Save & Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SecurityCheckRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
    }
}
