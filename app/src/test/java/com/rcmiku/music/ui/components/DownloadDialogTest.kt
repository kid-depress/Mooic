package com.rcmiku.music.ui.components

import com.rcmiku.ncmapi.api.player.SongLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadDialogTest {

    @Test
    fun `文件名中的非法字符被替换`() {
        assertEquals("歌_曲_名_", sanitizeFileName("歌/曲:名*"))
        assertEquals("a_b_c", sanitizeFileName("a\\b?c"))
        assertEquals("unknown", sanitizeFileName("   "))
    }

    @Test
    fun `无损与母带使用 flac 扩展名`() {
        assertEquals("flac", SongLevel.LOSSLESS.extension())
        assertEquals("flac", SongLevel.HIRES.extension())
    }

    @Test
    fun `有损码率使用 mp3 扩展名`() {
        assertEquals("mp3", SongLevel.STANDARD.extension())
        assertEquals("mp3", SongLevel.HIGHER.extension())
        assertEquals("mp3", SongLevel.EXHIGH.extension())
    }
}
