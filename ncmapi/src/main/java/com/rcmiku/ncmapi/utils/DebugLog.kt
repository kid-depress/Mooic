package com.rcmiku.ncmapi.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {
    private const val MAX_ENTRIES = 500
    private val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val entries = ArrayDeque<String>()

    @Synchronized fun append(message: String) {
        if (entries.size >= MAX_ENTRIES) entries.removeFirst()
        entries.addLast("${format.format(Date())} $message")
    }

    @Synchronized fun export(): String = entries.joinToString("\n")
}
