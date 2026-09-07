package com.rcmiku.music.ui.components

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.constants.MediaSessionConstants
import com.rcmiku.music.data.favoriteSongIdsDatastore
import com.rcmiku.music.ui.icons.Album
import com.rcmiku.music.ui.icons.Artist
import com.rcmiku.music.ui.icons.ChevronDown
import com.rcmiku.music.ui.icons.Comment
import com.rcmiku.music.ui.icons.Favorite
import com.rcmiku.music.ui.icons.FavoriteFill
import com.rcmiku.music.ui.icons.PauseFill
import com.rcmiku.music.ui.icons.Repeat
import com.rcmiku.music.ui.icons.RepeatOne
import com.rcmiku.music.ui.icons.Shuffle
import com.rcmiku.music.ui.icons.SkipNextFill
import com.rcmiku.music.ui.icons.SkipPreviousFill
import com.rcmiku.music.ui.navigation.AlbumNav
import com.rcmiku.music.ui.navigation.ArtistNav
import com.rcmiku.music.utils.getItemShape
import com.rcmiku.music.utils.makeTimeString
import com.rcmiku.ncmapi.model.Artist
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.model.SongAlbum
import com.rcmiku.ncmapi.utils.json
import kotlinx.coroutines.flow.map
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Player(
    mediaMetadata: MediaMetadata,
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onClick: () -> Unit = {},
    onContainerClick: () -> Unit = {},
    onPositionUpdate: (Long) -> Unit,
    navController: NavHostController
) {

    BackHandler {
        onBackPressed()
    }

    var sliderPosition by rememberSaveable {
        mutableStateOf<Long?>(null)
    }

    val playerState = LocalPlayerState.current
    val mediaController = LocalPlayerController.current.controller
    val isPlaying = playerState?.isPlaying == true
    val repeatMode = playerState?.repeatMode ?: 0
    val repeatIcon = when (repeatMode) {
        0 -> Repeat
        1 -> RepeatOne
        else -> Repeat
    }
    val context = LocalContext.current.applicationContext
    val mediaId = playerState?.currentMediaItem?.mediaId
    val songIds by context.favoriteSongIdsDatastore.data.map { it.songIdsList }
        .collectAsState(emptyList())
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var openPlayerBottomSheet by rememberSaveable { mutableStateOf(false) }
    var openCommentSheet by rememberSaveable { mutableStateOf(false) }
    val shuffleMode = playerState?.shuffleModeEnabled == true

    LaunchedEffect(mediaId) {
        playerState?.currentMediaItem?.mediaMetadata?.extras?.getString("song")?.let {
            currentSong = json.decodeFromString<Song>(it)
        }
    }

    val screenHeight = LocalConfiguration.current.screenHeightDp

    Surface(
        modifier = modifier
            .pointerInput(Unit) {
                var dragDistance = Offset.Zero
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDistance += dragAmount
                    },
                    onDragEnd = {
                        val threshold = 100.dp.toPx()
                        val dx = dragDistance.x
                        val dy = dragDistance.y
                        when {
                            abs(dy) > abs(dx) && abs(dy) > threshold -> {
                                if (dy < 0) mediaController?.seekToNext()
                                else mediaController?.seekToPrevious()
                            }

                            abs(dx) > abs(dy) && abs(dx) > threshold -> {
                                if (dx < 0) onClick()
                                else openPlayerBottomSheet = true
                            }
                        }
                        dragDistance = Offset.Zero
                    }
                )
            }
            .clickable {
                onContainerClick()
            }
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = if (screenHeight.dp < 700.dp) Arrangement.spacedBy(8.dp) else Arrangement.spacedBy(
                24.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = ChevronDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onClick() }
            ) {
                AsyncImage(
                    model = mediaMetadata.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = imageModifier
                        .clip(MaterialTheme.shapes.small)
                        .fillMaxWidth()
                )
            }

            Column(
                modifier = Modifier
                    .weight(6f)
                    .fillMaxWidth(),
                verticalArrangement = if (screenHeight.dp < 700.dp) Arrangement.spacedBy(0.dp) else Arrangement.spacedBy(
                    24.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            mediaMetadata.title?.let {
                                Text(
                                    text = it.toString(),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .basicMarquee()
                                        .clickable { openBottomSheet = true }
                                )
                            }
                            mediaMetadata.artist?.let {
                                Text(
                                    text = it.toString(),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .basicMarquee()
                                        .clickable { openBottomSheet = true }
                                )
                            }
                        }
                        FilledIconButton(
                            onClick = {
                                mediaController?.sendCustomCommand(
                                    MediaSessionConstants.CommandToggleLike,
                                    Bundle.EMPTY
                                )
                            }
                        ) {
                            Icon(
                                if (songIds.contains(mediaId?.toLong())) FavoriteFill else Favorite,
                                contentDescription = null
                            )
                        }
                        FilledIconButton(onClick = { openCommentSheet = true }) {
                            Icon(imageVector = Comment, contentDescription = null)
                        }
                        FilledIconButton(onClick = { openPlayerBottomSheet = true }) {
                            Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = null)
                        }
                    }

                    if (screenHeight.dp > 700.dp)
                        Spacer(Modifier.height(24.dp))

                    val interactionSource = remember { MutableInteractionSource() }
                    Slider(
                        value = (sliderPosition ?: position).toFloat(),
                        valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                        onValueChange = { sliderPosition = it.toLong() },
                        onValueChangeFinished = {
                            sliderPosition?.let {
                                mediaController?.seekTo(it)
                                onPositionUpdate(it)
                            }
                            sliderPosition = null
                        },
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                thumbTrackGapSize = 2.dp,
                                modifier = Modifier.height(4.dp)
                            )
                        },
                        thumb = {
                            SliderDefaults.Thumb(
                                interactionSource = interactionSource,
                                thumbSize = DpSize(4.dp, 20.dp)
                            )
                        }
                    )

                    Row {
                        Text(
                            text = makeTimeString(sliderPosition ?: position),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = makeTimeString(duration),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (screenHeight.dp > 700.dp)
                    Spacer(Modifier.height(24.dp))

                val buttonSize = Modifier.size(48.dp)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            mediaController?.sendCustomCommand(
                                MediaSessionConstants.CommandToggleShuffle,
                                Bundle.EMPTY
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Shuffle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.alpha(if (!shuffleMode) 0.4f else 1f)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { mediaController?.seekToPrevious() },
                        modifier = buttonSize
                    ) {
                        Icon(
                            SkipPreviousFill,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    FilledTonalIconButton(
                        modifier = Modifier.size(72.dp),
                        onClick = { if (!isPlaying) mediaController?.play() else mediaController?.pause() }
                    ) {
                        Icon(
                            if (isPlaying) PauseFill else Icons.Filled.PlayArrow,
                            modifier = Modifier.size(48.dp),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    FilledTonalIconButton(
                        onClick = { mediaController?.seekToNext() },
                        modifier = buttonSize
                    ) {
                        Icon(
                            SkipNextFill,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            val newMode = when (repeatMode) {
                                0 -> 2
                                1 -> 0
                                2 -> 1
                                else -> 0
                            }
                            mediaController?.repeatMode = newMode
                        }
                    ) {
                        Icon(
                            imageVector = repeatIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.alpha(if (repeatMode == 0) 0.4f else 1f)
                        )
                    }
                }
            }
        }


        currentSong?.let { song ->
            ArtistInfoSheet(
                song = song,
                onClick = { artist ->
                    navController.navigate(ArtistNav(artistId = artist.id))
                    onBackPressed()
                },
                onDismiss = {
                    openBottomSheet = false
                },
                openBottomSheet = openBottomSheet
            )
        }

        PlayerMenuBottomSheet(
            currentSong = currentSong,
            onDismiss = { openPlayerBottomSheet = false },
            openBottomSheet = openPlayerBottomSheet,
            onArtistClick = { artist ->
                navController.navigate(ArtistNav(artistId = artist.id))
                onBackPressed()
            },
            onAlbumClick = { album ->
                navController.navigate(AlbumNav(albumId = album.id))
                onBackPressed()
            }
        )

        CommentBottomSheet(
            songId = currentSong?.id ?: mediaId?.substringBefore('?')?.toLongOrNull(),
            songName = mediaMetadata.title?.toString(),
            onDismiss = { openCommentSheet = false },
            openBottomSheet = openCommentSheet
        )
    }
}

