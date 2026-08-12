package com.example

import com.example.data.network.M3uParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class M3uParserTest {

    @Test
    fun testM3uParsing() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="espn" tvg-name="ESPN HD" tvg-logo="https://example.com/logo.png" group-title="Sports", ESPN HD Live
            https://example.com/live/espn.m3u8
            #EXTINF:-1 group-title="News", World News 24/7
            https://example.com/live/news.m3u8
        """.trimIndent()

        val channels = M3uParser.parseM3uContent(sampleM3u)

        assertEquals(2, channels.size)
        
        val espn = channels[0]
        assertEquals("ESPN HD", espn.name)
        assertEquals("https://example.com/live/espn.m3u8", espn.url)
        assertEquals("Sports", espn.category)
        assertEquals("https://example.com/logo.png", espn.logoUrl)

        val news = channels[1]
        assertEquals("World News 24/7", news.name)
        assertEquals("https://example.com/live/news.m3u8", news.url)
        assertEquals("News", news.category)
    }
}
