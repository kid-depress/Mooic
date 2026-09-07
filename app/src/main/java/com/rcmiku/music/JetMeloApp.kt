package com.rcmiku.music

import android.app.Application
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.rcmiku.music.constants.apiBaseUrlKey
import com.rcmiku.music.constants.ncmCookieKey
import com.rcmiku.music.constants.unblockBaseUrlKey
import com.rcmiku.music.constants.userIdKye
import com.rcmiku.music.playback.PlayerController
import com.rcmiku.music.utils.SongListUtil
import com.rcmiku.music.utils.UserAgentUtil
import com.rcmiku.music.utils.dataStore
import com.rcmiku.ncmapi.api.API_BASE_URL
import com.rcmiku.ncmapi.api.UNBLOCK_BASE_URL
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.utils.CookieKeys
import com.rcmiku.ncmapi.utils.CookieProvider
import com.rcmiku.ncmapi.utils.FileProvider
import com.rcmiku.ncmapi.utils.UserAgentProvider
import com.rcmiku.ncmapi.utils.json
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit

@HiltAndroidApp
class JetMeloApp : Application(), SingletonImageLoader.Factory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @androidx.annotation.OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        PlayerController.init(this)
        FileProvider.init(cacheDir.resolve("ncm"))
        SongListUtil.init(filesDir.resolve("playlist"))
        UserAgentProvider.init(UserAgentUtil.DEFAULT_USER_AGENT)
        applicationScope.launch {
            UserAgentProvider.init(UserAgentUtil.DEFAULT_USER_AGENT)
            dataStore.data
                .map { it[ncmCookieKey] }
                .distinctUntilChanged()
                .collect { ncmCookie ->
                    val cookieMap = ncmCookie
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull() }
                    if (cookieMap?.containsKey(CookieKeys.MUSIC_U) == true) {
                        CookieProvider.init(cookieMap)
                        ensureUserIdPersisted()
                    } else {
                        CookieProvider.clear()
                    }
                }
        }
        applicationScope.launch {
            dataStore.data
                .map { prefs ->
                    prefs[apiBaseUrlKey] to prefs[unblockBaseUrlKey]
                }
                .distinctUntilChanged()
                .collect { (apiUrl, unblockUrl) ->
                    if (!apiUrl.isNullOrEmpty()) API_BASE_URL = apiUrl
                    if (!unblockUrl.isNullOrEmpty()) UNBLOCK_BASE_URL = unblockUrl
                }
        }
    }

    /**
     * 已登录但 userId 缺失（历史版本登录时未持久化）时，补拉用户信息写入。
     * 供"添加到歌单"、喜欢等依赖 userId 的功能使用。
     */
    private suspend fun ensureUserIdPersisted() {
        val storedUserId = dataStore.data.first()[userIdKye] ?: 0L
        if (storedUserId > 0) return
        runCatching {
            val userId = AccountApi.accountInfo().getOrNull()?.account?.profile?.userId ?: return
            if (userId > 0) {
                dataStore.edit { it[userIdKye] = userId }
            }
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        super.onTerminate()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizeBytes(64 * 1024 * 1024L)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .maxSizeBytes(512 * 1024 * 1024L)
                    .directory(cacheDir.resolve("coil"))
                    .build()
            }
            .build()
    }

}
