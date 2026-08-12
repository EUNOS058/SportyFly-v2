package com.example.data.repository

import com.example.data.model.Channel
import com.example.data.network.M3uParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class ChannelRepository {

    // Default test streams requested & additional verified public streams
    val defaultChannels = listOf(
        Channel(
            id = "default_mux_test",
            name = "Mux Sports Live Test",
            url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            category = "Sports Test",
            description = "Default high-stability HLS multi-bitrate test stream",
            isFavorite = true
        ),
        Channel(
            id = "sintel_hls_test",
            name = "Sintel HD Stream Test",
            url = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            category = "Video Test",
            description = "Akamai CDN open HLS stream test"
        ),
        Channel(
            id = "big_buck_bunny_test",
            name = "Big Buck Bunny Live",
            url = "https://test-streams.mux.dev/pts_shift/master.m3u8",
            category = "Video Test",
            description = "Live stream loop test"
        ),
        Channel(
            id = "fmp4_hls_test",
            name = "fMP4 HLS Stream Test",
            url = "https://demo.unified-streaming.com/k8s/features/stable/video/mp4/fmp4.m3u8/master.m3u8",
            category = "Format Test",
            description = "Fragmented MP4 live broadcast test"
        )
    )

    private val _channels = MutableStateFlow(defaultChannels)
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(setOf("default_mux_test"))
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    fun toggleFavorite(channelId: String) {
        _favoriteIds.update { current ->
            if (current.contains(channelId)) current - channelId else current + channelId
        }
        _channels.update { list ->
            list.map { channel ->
                if (channel.id == channelId) {
                    channel.copy(isFavorite = !_favoriteIds.value.contains(channelId))
                } else channel
            }
        }
    }

    fun addCustomChannel(name: String, url: String, category: String = "Custom Channels") {
        val newChannel = Channel(
            id = "custom_${System.currentTimeMillis()}",
            name = name.ifBlank { "Custom Stream" },
            url = url.trim(),
            category = category,
            description = "User added stream"
        )
        _channels.update { listOf(newChannel) + it }
    }

    suspend fun importM3uPlaylist(playlistUrl: String): Result<Int> = withContext(Dispatchers.IO) {
        val result = M3uParser.fetchPlaylistFromUrl(playlistUrl)
        result.map { parsedChannels ->
            if (parsedChannels.isNotEmpty()) {
                _channels.update { existing ->
                    // Merge new channels while avoiding duplicate URLs
                    val existingUrls = existing.map { it.url }.toSet()
                    val filteredNew = parsedChannels.filter { it.url !in existingUrls }
                    filteredNew + existing
                }
            }
            parsedChannels.size
        }
    }

    fun removeChannel(channelId: String) {
        _channels.update { list -> list.filter { it.id != channelId } }
    }
}
