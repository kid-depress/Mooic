package com.rcmiku.music.playback

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.CacheSpan
import androidx.media3.datasource.cache.SimpleCache
import com.rcmiku.music.constants.audioCacheMaxSizeKey
import com.rcmiku.music.utils.dataStore
import com.rcmiku.music.utils.get
import java.util.TreeSet

@UnstableApi
object AudioCache {
    const val DEFAULT_MAX_SIZE_BYTES = 512L * 1024 * 1024
    private val lock = Any()

    @Volatile
    private var cache: SimpleCache? = null
    private var evictor: AdjustableCacheEvictor? = null

    fun get(context: Context): SimpleCache {
        cache?.let { return it }

        return synchronized(lock) {
            cache ?: run {
                val maxSize = context.dataStore.get(
                    audioCacheMaxSizeKey,
                    DEFAULT_MAX_SIZE_BYTES,
                )
                val cacheEvictor = AdjustableCacheEvictor(maxSize)
                SimpleCache(
                    context.cacheDir.resolve("audio"),
                    cacheEvictor,
                    StandaloneDatabaseProvider(context)
                ).also {
                    evictor = cacheEvictor
                    cache = it
                }
            }
        }
    }

    fun cacheSpace(context: Context): Long = get(context).cacheSpace

    fun setMaxSize(context: Context, maxSizeBytes: Long) {
        require(maxSizeBytes > 0) { "Cache size must be greater than zero" }
        val currentCache = get(context)
        synchronized(lock) {
            evictor?.updateMaxBytes(currentCache, maxSizeBytes)
        }
    }

    fun clear(context: Context) {
        val currentCache = get(context)
        currentCache.keys.toList().forEach(currentCache::removeResource)
    }
}

/** LRU cache evictor whose limit can be updated without recreating the player cache. */
@UnstableApi
private class AdjustableCacheEvictor(initialMaxBytes: Long) : CacheEvictor {
    private val leastRecentlyUsed = TreeSet<CacheSpan>(compareBy<CacheSpan> {
        it.lastTouchTimestamp
    }.thenBy { it.key }.thenBy { it.position })

    private var maxBytes = initialMaxBytes
    private var currentSize = 0L

    override fun requiresCacheSpanTouches(): Boolean = true

    override fun onCacheInitialized() = Unit

    @Synchronized
    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {
        if (length != C.LENGTH_UNSET.toLong()) {
            evictCache(cache, length)
        }
    }

    @Synchronized
    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        if (leastRecentlyUsed.add(span)) {
            currentSize += span.length
        }
        evictCache(cache, 0)
    }

    @Synchronized
    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        if (leastRecentlyUsed.remove(span)) {
            currentSize -= span.length
        }
    }

    @Synchronized
    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        onSpanRemoved(cache, oldSpan)
        onSpanAdded(cache, newSpan)
    }

    @Synchronized
    fun updateMaxBytes(cache: Cache, maxBytes: Long) {
        this.maxBytes = maxBytes
        evictCache(cache, 0)
    }

    private fun evictCache(cache: Cache, requiredSpace: Long) {
        while (currentSize + requiredSpace > maxBytes && leastRecentlyUsed.isNotEmpty()) {
            cache.removeSpan(leastRecentlyUsed.first())
        }
    }
}
