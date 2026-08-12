package com.example.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.data.model.Channel
import com.example.data.network.M3uParser
import com.example.data.repository.ChannelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ResizeMode {
    FIT, CROP, FILL
}

sealed class PlaybackUiState {
    object Idle : PlaybackUiState() // Placeholder UI before play is tapped
    object Loading : PlaybackUiState()
    object Playing : PlaybackUiState()
    object Paused : PlaybackUiState()
    data class Error(val message: String, val canRetry: Boolean = true) : PlaybackUiState()
}

class PlayerViewModel(
    val repository: ChannelRepository = ChannelRepository()
) : ViewModel() {

    private var exoPlayer: ExoPlayer? = null

    val channels = repository.channels
    val favoriteIds = repository.favoriteIds

    private val _selectedChannel = MutableStateFlow<Channel?>(repository.defaultChannels.firstOrNull())
    val selectedChannel: StateFlow<Channel?> = _selectedChannel.asStateFlow()

    private val _playbackState = MutableStateFlow<PlaybackUiState>(PlaybackUiState.Idle)
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    private val _resizeMode = MutableStateFlow(ResizeMode.FIT)
    val resizeMode: StateFlow<ResizeMode> = _resizeMode.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isPlaylistLoading = MutableStateFlow(false)
    val isPlaylistLoading: StateFlow<Boolean> = _isPlaylistLoading.asStateFlow()

    private val _playlistMessage = MutableStateFlow<String?>(null)
    val playlistMessage: StateFlow<String?> = _playlistMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    /**
     * Strictly instantiates ExoPlayer ONCE using context.
     * Enforces strict 4-second HTTP connect and read timeouts.
     */
    fun getOrCreatePlayer(context: Context): ExoPlayer {
        if (exoPlayer == null) {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(4000)
                .setReadTimeoutMs(4000)
                .setAllowCrossProtocolRedirects(true)

            val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

            val player = ExoPlayer.Builder(context.applicationContext)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()

            player.playWhenReady = false // CRITICAL: Do NOT auto-play on startup!

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> {
                            _playbackState.value = PlaybackUiState.Loading
                        }
                        Player.STATE_READY -> {
                            _playbackState.value = if (player.playWhenReady) {
                                PlaybackUiState.Playing
                            } else {
                                PlaybackUiState.Paused
                            }
                        }
                        Player.STATE_ENDED -> {
                            _playbackState.value = PlaybackUiState.Idle
                        }
                        Player.STATE_IDLE -> {
                            // If idle and not error, keep idle
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    val errorMessage = when (error.errorCode) {
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                            "Network connection timed out (4s limit exceeded)"
                        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                            "Stream URL unavailable or returned HTTP error"
                        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                            "Invalid M3U8 or stream media format"
                        else -> error.localizedMessage ?: "Stream playback failed"
                    }
                    _playbackState.value = PlaybackUiState.Error(errorMessage)
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        _playbackState.value = PlaybackUiState.Playing
                    } else if (player.playbackState == Player.STATE_READY) {
                        _playbackState.value = PlaybackUiState.Paused
                    }
                }
            })

            exoPlayer = player
        }
        return exoPlayer!!
    }

    /**
     * User explicitly taps Play button on the placeholder or stream.
     */
    fun playStream(channel: Channel? = selectedChannel.value) {
        val targetChannel = channel ?: return
        val player = exoPlayer ?: return

        _selectedChannel.value = targetChannel
        _playbackState.value = PlaybackUiState.Loading

        val mediaItem = MediaItem.fromUri(targetChannel.url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun pauseStream() {
        exoPlayer?.playWhenReady = false
        _playbackState.value = PlaybackUiState.Paused
    }

    fun resumeStream() {
        exoPlayer?.let { player ->
            if (player.playbackState == Player.STATE_IDLE) {
                playStream()
            } else {
                player.playWhenReady = true
                _playbackState.value = PlaybackUiState.Playing
            }
        }
    }

    fun selectChannel(channel: Channel) {
        _selectedChannel.value = channel
        _playbackState.value = PlaybackUiState.Idle // Reset to idle placeholder for user to tap Play
        exoPlayer?.stop()
    }

    fun toggleMute() {
        _isMuted.update { current ->
            val next = !current
            exoPlayer?.volume = if (next) 0f else 1f
            next
        }
    }

    fun toggleResizeMode() {
        _resizeMode.update { current ->
            when (current) {
                ResizeMode.FIT -> ResizeMode.CROP
                ResizeMode.CROP -> ResizeMode.FILL
                ResizeMode.FILL -> ResizeMode.FIT
            }
        }
    }

    fun toggleFavorite(channelId: String) {
        repository.toggleFavorite(channelId)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun addCustomChannel(name: String, url: String, category: String) {
        repository.addCustomChannel(name, url, category)
        val newlyAdded = repository.channels.value.firstOrNull()
        if (newlyAdded != null) {
            selectChannel(newlyAdded)
        }
    }

    fun importM3uPlaylist(url: String) {
        viewModelScope.launch {
            _isPlaylistLoading.value = true
            _playlistMessage.value = null
            
            val result = repository.importM3uPlaylist(url)
            result.onSuccess { count ->
                _playlistMessage.value = if (count > 0) {
                    "Successfully loaded $count channels!"
                } else {
                    "No valid channels found in playlist."
                }
            }.onFailure { err ->
                _playlistMessage.value = "Failed to load playlist: ${err.localizedMessage}"
            }
            _isPlaylistLoading.value = false
        }
    }

    fun clearPlaylistMessage() {
        _playlistMessage.value = null
    }

    fun retryPlayback() {
        selectedChannel.value?.let { playStream(it) }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer?.release()
        exoPlayer = null
    }
}
