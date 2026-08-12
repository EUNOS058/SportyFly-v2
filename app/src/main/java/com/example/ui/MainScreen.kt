package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AddChannelDialog
import com.example.ui.components.ChannelListSection
import com.example.ui.components.VideoPlayerView
import com.example.ui.player.PlayerViewModel
import com.example.ui.theme.SportyAccent
import com.example.ui.theme.SportyDarkBackground
import com.example.ui.theme.SportyPrimary
import com.example.ui.theme.SportySurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val player = remember { viewModel.getOrCreatePlayer(context) }

    val channels by viewModel.channels.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val resizeMode by viewModel.resizeMode.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isPlaylistLoading by viewModel.isPlaylistLoading.collectAsState()
    val playlistMessage by viewModel.playlistMessage.collectAsState()

    var isFullscreen by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show toast or snackbar when playlist import completes
    LaunchedEffect(playlistMessage) {
        playlistMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearPlaylistMessage()
            showAddDialog = false
        }
    }

    if (isFullscreen) {
        // Fullscreen Player Mode
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoPlayerView(
                player = player,
                selectedChannel = selectedChannel,
                playbackState = playbackState,
                resizeMode = resizeMode,
                isMuted = isMuted,
                isFullscreen = true,
                onPlayTap = { viewModel.playStream() },
                onPauseTap = { viewModel.pauseStream() },
                onResumeTap = { viewModel.resumeStream() },
                onRetryTap = { viewModel.retryPlayback() },
                onToggleMute = { viewModel.toggleMute() },
                onToggleResizeMode = { viewModel.toggleResizeMode() },
                onToggleFullscreen = { isFullscreen = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        // Standard Portrait Mode
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SportyPrimary.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LiveTv,
                                        contentDescription = "SportyFly Logo",
                                        tint = SportyPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "SportyFly Live",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = SportyAccent
                                    ) {
                                        Text(
                                            text = "TV",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Safe Non-ANR IPTV Engine",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("add_channel_appbar_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Playlist or Channel",
                                tint = SportyPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SportySurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = SportyDarkBackground
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Video Player Container (Fixed 16:9 ratio)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    VideoPlayerView(
                        player = player,
                        selectedChannel = selectedChannel,
                        playbackState = playbackState,
                        resizeMode = resizeMode,
                        isMuted = isMuted,
                        isFullscreen = false,
                        onPlayTap = { viewModel.playStream() },
                        onPauseTap = { viewModel.pauseStream() },
                        onResumeTap = { viewModel.resumeStream() },
                        onRetryTap = { viewModel.retryPlayback() },
                        onToggleMute = { viewModel.toggleMute() },
                        onToggleResizeMode = { viewModel.toggleResizeMode() },
                        onToggleFullscreen = { isFullscreen = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Channel Directory & Controls Section
                ChannelListSection(
                    channels = channels,
                    favoriteIds = favoriteIds,
                    selectedChannel = selectedChannel,
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onCategorySelect = { viewModel.setSelectedCategory(it) },
                    onChannelSelect = { viewModel.selectChannel(it) },
                    onPlayTap = { viewModel.playStream(it) },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }

    if (showAddDialog) {
        AddChannelDialog(
            isPlaylistLoading = isPlaylistLoading,
            onDismissRequest = { showAddDialog = false },
            onAddSingleStream = { name, url, category ->
                viewModel.addCustomChannel(name, url, category)
                showAddDialog = false
            },
            onImportM3uPlaylist = { url ->
                viewModel.importM3uPlaylist(url)
            }
        )
    }
}
