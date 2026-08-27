package com.rcmiku.music.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener
import androidx.media3.session.CommandButton
import androidx.media3.session.CommandButton.ICON_UNDEFINED
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.rcmiku.music.MainActivity
import com.rcmiku.music.R
import com.rcmiku.music.constants.MediaSessionConstants
import com.rcmiku.music.constants.audioQualityKey
import com.rcmiku.music.constants.userIdKye
import com.rcmiku.music.constants.use40DpIconKey
import com.rcmiku.music.constants.userIdKye
import com.rcmiku.music.data.favoriteSongIdsDatastore
import com.rcmiku.music.extensions.updateMediaItemUri
import com.rcmiku.music.utils.FavoriteSongIdsUtil
import com.rcmiku.music.utils.dataStore
import com.rcmiku.music.utils.enumPreference
import com.rcmiku.music.utils.preference
import com.rcmiku.music.utils.reportLikeFailure
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.api.player.SongLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import com.rcmiku.ncmapi.utils.CookieProvider

@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var favoriteSongIds: List<Long> by mutableStateOf(emptyList())
    private val use40DpIcon by preference(this, use40DpIconKey, false)
    private val userId by preference(this, userIdKye, 0L)
    private val audioQuality by enumPreference(this, audioQualityKey, SongLevel.STANDARD)
    private var scrobbleJob: Job? = null
    private var scrobbleState: ScrobbleState? = null

    private val favoriteButton: CommandButton
        get() = CommandButton.Builder(ICON_UNDEFINED)
            .setCustomIconResId(if (use40DpIcon) R.drawable.ic_favorite_40_dp else R.drawable.ic_favorite)
            .setDisplayName("like")
            .setSessionCommand(MediaSessionConstants.CommandToggleLike)
            .build()

    private val favoriteButtonOn: CommandButton
        get() = CommandButton.Builder(ICON_UNDEFINED)
            .setCustomIconResId(if (use40DpIcon) R.drawable.ic_favorite_fill_40_dp else R.drawable.ic_favorite_fill)
            .setDisplayName("like_on")
            .setSessionCommand(MediaSessionConstants.CommandToggleLike)
            .build()

    private val shuffleButton: CommandButton
        get() = CommandButton.Builder(ICON_UNDEFINED)
            .setCustomIconResId(if (use40DpIcon) R.drawable.ic_shuffle_40_dp else R.drawable.ic_shuffle)
            .setDisplayName("shuffle")
            .setSessionCommand(MediaSessionConstants.CommandToggleShuffle)
            .build()

    private val shuffleButtonOn: CommandButton
        get() = CommandButton.Builder(ICON_UNDEFINED)
            .setCustomIconResId(if (use40DpIcon) R.drawable.ic_shuffle_on_40_dp else R.drawable.ic_shuffle_on)
            .setDisplayName("shuffle_on")
            .setSessionCommand(MediaSessionConstants.CommandToggleShuffle)
            .build()

    private val scope = CoroutineScope(Dispatchers.Main) + SupervisorJob()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { 2000 },
                DefaultMediaNotificationProvider.DEFAULT_CHANNEL_ID,
                DefaultMediaNotificationProvider.DEFAULT_CHANNEL_NAME_RESOURCE_ID
            ).apply {
                setSmallIcon(R.drawable.ic_music_note)
            }
        )
        val audioOnlyRenderersFactory =
            RenderersFactory {
                    handler: Handler,
                    _: VideoRendererEventListener,
                    audioListener: AudioRendererEventListener,
                    _: TextOutput,
                    _: MetadataOutput,
                ->
                arrayOf<Renderer>(
                    MediaCodecAudioRenderer(
                        this,
                        MediaCodecSelector.DEFAULT,
                        handler,
                        audioListener
                    )
                )
            }

        val player = ExoPlayer.Builder(this, audioOnlyRenderersFactory)
            .apply {
                val resolvingDataSourceFactory: ResolvingDataSource.Factory = ResolvingDataSource.Factory(
                    DefaultHttpDataSource.Factory()
                ) { dataSpec ->
                    runBlocking {
                        dataSpec.withUri(
                            updateMediaItemUri(
                                dataSpec.uri.path.orEmpty(),
                                dataSpec.uri.query,
                                audioQuality
                            )
                                ?: throw PlaybackException(
                                    null,
                                    null,
                                    PlaybackException.ERROR_CODE_REMOTE_ERROR
                                )
                        )
                    }
                }
                val cacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(AudioCache.get(this@PlaybackService))
                    .setUpstreamDataSourceFactory(resolvingDataSourceFactory)
                    .setCacheKeyFactory { dataSpec ->
                        "${dataSpec.key ?: dataSpec.uri}#${audioQuality.value}#unblock-v2"
                    }
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            }.build()

        val audioOffloadPreferences =
            TrackSelectionParameters.AudioOffloadPreferences.Builder()
                .setAudioOffloadMode(TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                .setIsGaplessSupportRequired(true)
                .build()
        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .setAudioOffloadPreferences(audioOffloadPreferences)
                .build()
        player.repeatMode = REPEAT_MODE_ALL
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            ).setCallback(MediaSessionCallback())
            .setCustomLayout(ImmutableList.of(favoriteButton, shuffleButton)).build()
        observeIconPreference()
        observeFavoriteSongIds()
        observeScrobble(player)
    }

    override fun onDestroy() {
        scrobbleJob?.cancel()
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    @OptIn(UnstableApi::class)
    override fun onTaskRemoved(rootIntent: Intent?) {
        pauseAllPlayersAndStopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    fun updateCustomLayout() {
        mediaSession?.setCustomLayout(
            ImmutableList.of(
                if (favoriteSongIds.contains(mediaSession?.player?.currentMediaItem?.mediaId?.toLong())) favoriteButtonOn else favoriteButton,
                if (mediaSession?.player?.shuffleModeEnabled != false) shuffleButtonOn else shuffleButton
            )
        )
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        updateCustomLayout()
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    private fun toggleLike(like: Boolean, songId: Long) {
        scope.launch {
            AccountApi.songLike(like, songId, userId).onSuccess {
                if (like) {
                    FavoriteSongIdsUtil.addSongId(applicationContext, songId)
                } else {
                    FavoriteSongIdsUtil.removeSongId(applicationContext, songId)
                }
            }.onFailure { error ->
                reportLikeFailure(applicationContext, like, songId, userId, error)
            }
        }
    }

    private data class ScrobbleState(
        val mediaId: String,
        val mediaItemIndex: Int,
        var playedSeconds: Int = 0,
        var reported: Boolean = false
    )

    private fun observeScrobble(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                resetScrobble(player)
                if (player.isPlaying) startScrobbleTicker(player)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startScrobbleTicker(player)
                } else {
                    stopScrobbleTicker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    stopScrobbleTicker()
                }
            }
        })
    }

    private fun resetScrobble(player: Player) {
        scrobbleState = player.currentMediaItem?.let {
            ScrobbleState(
                mediaId = it.mediaId,
                mediaItemIndex = player.currentMediaItemIndex
            )
        }
    }

    private fun startScrobbleTicker(player: Player) {
        val mediaItem = player.currentMediaItem ?: return
        if (!CookieProvider.isLoggedIn()) return
        if (mediaItem.mediaId.toLongOrNull() == null) return

        val currentState = scrobbleState
        if (currentState?.mediaId == mediaItem.mediaId &&
            currentState.mediaItemIndex == player.currentMediaItemIndex &&
            currentState.reported
        ) {
            return
        }

        if (scrobbleJob?.isActive == true) return

        scrobbleJob = scope.launch {
            while (isActive) {
                delay(1000)
                tickScrobble(player)
            }
        }
    }

    private fun stopScrobbleTicker() {
        scrobbleJob?.cancel()
        scrobbleJob = null
    }

    private fun tickScrobble(player: Player) {
        if (!CookieProvider.isLoggedIn()) {
            stopScrobbleTicker()
            return
        }

        val mediaItem = player.currentMediaItem ?: run {
            stopScrobbleTicker()
            return
        }
        val songId = mediaItem.mediaId.toLongOrNull() ?: run {
            stopScrobbleTicker()
            return
        }
        val currentState = scrobbleState
            ?.takeIf {
                it.mediaId == mediaItem.mediaId &&
                    it.mediaItemIndex == player.currentMediaItemIndex
            }
            ?: ScrobbleState(
                mediaId = mediaItem.mediaId,
                mediaItemIndex = player.currentMediaItemIndex
            ).also { scrobbleState = it }

        if (currentState.reported) {
            stopScrobbleTicker()
            return
        }

        currentState.playedSeconds += 1
        val totalSeconds = resolveTotalSeconds(player, mediaItem)
        val thresholdSeconds = totalSeconds?.let {
            if (it < 60) maxOf(5, it / 2) else 30
        } ?: 30

        if (currentState.playedSeconds >= thresholdSeconds) {
            currentState.reported = true
            submitScrobble(mediaItem, songId, currentState.playedSeconds, totalSeconds)
            stopScrobbleTicker()
        }
    }

    private fun resolveTotalSeconds(player: Player, mediaItem: MediaItem): Int? {
        val playerDuration = player.duration
            .takeIf { it != C.TIME_UNSET && it > 0 }
        val metadataDuration = mediaItem.mediaMetadata.extras
            ?.getLong(MediaSessionConstants.EXTRA_DURATION_MS)
            ?.takeIf { it > 0 }

        return (playerDuration ?: metadataDuration)
            ?.div(1000L)
            ?.toInt()
    }

    private fun submitScrobble(
        mediaItem: MediaItem,
        songId: Long,
        playedSeconds: Int,
        totalSeconds: Int?
    ) {
        if (!CookieProvider.isLoggedIn()) return

        val metadata = mediaItem.mediaMetadata
        val extras = metadata.extras
        val sourceId = extras
            ?.getLong(MediaSessionConstants.EXTRA_SOURCE_ID)
            ?.takeIf { it > 0 }
        val sourceName = extras
            ?.getString(MediaSessionConstants.EXTRA_SOURCE_NAME)
            ?.takeIf { it.isNotBlank() }
        val currentAudioQuality = audioQuality
        val reportedSeconds = totalSeconds?.let {
            minOf(playedSeconds, it)
        } ?: playedSeconds

        scope.launch(Dispatchers.IO) {
            AccountApi.scrobble(
                songId = songId,
                time = reportedSeconds,
                total = totalSeconds,
                sourceId = sourceId,
                sourceName = sourceName,
                songName = metadata.title?.toString(),
                artistName = metadata.artist?.toString(),
                songLevel = currentAudioQuality
            )
        }
    }

    @kotlin.OptIn(FlowPreview::class)
    private fun observeIconPreference() {
        scope.launch {
            applicationContext.dataStore.data.debounce(1000)
                .map { it[use40DpIconKey] ?: false }.distinctUntilChanged().collect {
                    updateCustomLayout()
                }
        }
    }

    @kotlin.OptIn(FlowPreview::class)
    private fun observeFavoriteSongIds() {
        scope.launch {
            applicationContext.favoriteSongIdsDatastore.data.debounce(1000).distinctUntilChanged()
                .collect { favoriteSongs ->
                    favoriteSongIds = favoriteSongs.songIdsList
                    updateCustomLayout()
                }
        }
    }

    private inner class MediaSessionCallback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(
                    MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                        .add(MediaSessionConstants.CommandToggleLike)
                        .add(MediaSessionConstants.CommandToggleShuffle)
                        .build()
                )
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == MediaSessionConstants.ACTION_TOGGLE_SHUFFLE) {
                session.player.shuffleModeEnabled = !session.player.shuffleModeEnabled
                updateCustomLayout()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            if (customCommand.customAction == MediaSessionConstants.ACTION_TOGGLE_LIKE) {
                session.player.currentMediaItem?.mediaId?.toLongOrNull()?.let {
                    toggleLike(it !in favoriteSongIds, it)
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }
}
