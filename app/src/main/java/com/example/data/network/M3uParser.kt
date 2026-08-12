package com.example.data.network

import com.example.data.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.StringReader
import java.util.UUID
import java.util.concurrent.TimeUnit

object M3uParser {

    // OkHttpClient strictly configured with maximum 4-second timeouts as required
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /**
     * Fetches M3U playlist content from a URL asynchronously on Dispatchers.IO.
     * Enforces strict 4-second network timeout.
     */
    suspend fun fetchPlaylistFromUrl(playlistUrl: String): Result<List<Channel>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(playlistUrl)
                .addHeader("User-Agent", "SportyFly-IPTV/1.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP Error ${response.code}: ${response.message}"))
                }
                val bodyText = response.body?.string() ?: ""
                val channels = parseM3uContent(bodyText)
                Result.success(channels)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parses raw M3U text content safely.
     */
    fun parseM3uContent(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(StringReader(content))
        var line: String?

        var currentTitle = ""
        var currentLogo: String? = null
        var currentGroup = "Sports & Live TV"

        while (reader.readLine().also { line = it } != null) {
            val trimmed = line?.trim() ?: continue
            if (trimmed.isEmpty()) continue

            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                // Parse channel parameters
                currentTitle = parseAttribute(trimmed, "tvg-name")
                    .ifEmpty { parseTitleFromExtInf(trimmed) }
                
                val logo = parseAttribute(trimmed, "tvg-logo")
                if (logo.isNotEmpty()) currentLogo = logo
                
                val group = parseAttribute(trimmed, "group-title")
                if (group.isNotEmpty()) currentGroup = group

            } else if (!trimmed.startsWith("#")) {
                // This is the stream URL
                if (trimmed.startsWith("http://", ignoreCase = true) || 
                    trimmed.startsWith("https://", ignoreCase = true) ||
                    trimmed.endsWith(".m3u8", ignoreCase = true)) {
                    
                    val name = if (currentTitle.isNotEmpty()) currentTitle else "Channel ${channels.size + 1}"
                    channels.add(
                        Channel(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            url = trimmed,
                            category = currentGroup,
                            logoUrl = currentLogo
                        )
                    )
                }
                // Reset metadata for next entry
                currentTitle = ""
                currentLogo = null
                currentGroup = "Sports & Live TV"
            }
        }

        return channels
    }

    private fun parseTitleFromExtInf(extInfLine: String): String {
        val commaIndex = extInfLine.lastIndexOf(',')
        return if (commaIndex != -1 && commaIndex < extInfLine.length - 1) {
            extInfLine.substring(commaIndex + 1).trim()
        } else {
            "Live Stream"
        }
    }

    private fun parseAttribute(line: String, attrName: String): String {
        val pattern = """$attrName="([^"]*)"""".toRegex(RegexOption.IGNORE_CASE)
        val match = pattern.find(line)
        return match?.groupValues?.get(1) ?: ""
    }

    /**
     * Tests stream connectivity on Dispatchers.IO with 4s max timeout.
     */
    suspend fun checkStreamStatus(streamUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(streamUrl)
                .head() // Try HEAD request first
                .addHeader("User-Agent", "SportyFly-IPTV/1.0")
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}
