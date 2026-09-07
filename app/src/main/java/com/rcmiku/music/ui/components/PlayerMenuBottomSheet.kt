package com.rcmiku.music.ui.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.constants.heartBeatModeKey
import com.rcmiku.music.ui.icons.Album
import com.rcmiku.music.ui.icons.Artist
import com.rcmiku.music.ui.icons.Download
import com.rcmiku.music.ui.icons.FavoriteFill
import com.rcmiku.music.ui.icons.SongListAdd
import com.rcmiku.music.ui.icons.Timelapse
import com.rcmiku.music.ui.icons.Timer
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.ncmapi.model.Artist
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.model.SongAlbum
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMenuBottomSheet(
    currentSong: Song? = null,
    openBottomSheet: Boolean,
    onDismiss: () -> Unit,
    onArtistClick: (Artist) -> Unit = {},
    onAlbumClick: (SongAlbum) -> Unit = {},
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var timePicker by rememberSaveable { mutableStateOf(false) }
    var openArtistInfoSheet by rememberSaveable { mutableStateOf(false) }
    var openAlbumInfoSheet by rememberSaveable { mutableStateOf(false) }
    var openDownloadDialog by rememberSaveable { mutableStateOf(false) }
    val playerState = LocalPlayerState.current
    val isSleepTimerSet = playerState?.isSleepTimerSet == true
    val context = LocalContext.current
    var cancelSleepTimer by rememberSaveable { mutableStateOf(false) }
    var openSongListBottomSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(openBottomSheet) {
        if (openBottomSheet) {
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
        }
    }

    if (openBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
        ) {
            LazyColumn(
                Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 8.dp,
                            bottomEnd = 8.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    if (isSleepTimerSet)
                                        cancelSleepTimer = true
                                    else
                                        timePicker = true
                                }, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSleepTimerSet) Timelapse else Timer,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(if (isSleepTimerSet) R.string.remaining_time else R.string.sleep_timer),
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (isSleepTimerSet) {
                                playerState?.remainingTime?.let {
                                    Text(
                                        text = it.toDuration(DurationUnit.SECONDS)
                                            .toComponents { hours, minutes, seconds, _ ->
                                                if (hours > 0) {
                                                    "%02dh:%02dm:%02ds".format(
                                                        hours,
                                                        minutes,
                                                        seconds
                                                    )
                                                } else {
                                                    "%02dm:%02ds".format(minutes, seconds)
                                                }
                                            },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    openSongListBottomSheet = true
                                    onDismiss()
                                }, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = SongListAdd,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(R.string.add_to_songList),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    openArtistInfoSheet = true
                                    onDismiss()
                                }, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Artist,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(R.string.view_artist),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    openAlbumInfoSheet = true
                                    onDismiss()
                                }, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Album,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(R.string.view_album),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    openDownloadDialog = true
                                    onDismiss()
                                }, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Download,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(R.string.download),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable(onClick = {
                                    currentSong?.id?.let {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "https://music.163.com/#/song?id=${it}"
                                            )
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                shareIntent,
                                                context.getString(R.string.share_link)
                                            )
                                        )
                                    }
                                }), verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(R.string.share),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                item {
                    var heartBeatMode by rememberPreference(heartBeatModeKey, false)
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 8.dp,
                            topEnd = 8.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable { heartBeatMode = !heartBeatMode },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = FavoriteFill,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(R.string.heart_beat_mode),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = heartBeatMode,
                                onCheckedChange = { heartBeatMode = it }
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                    }
                }


                item {
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (timePicker)
                TimePickerDialog(
                    onDismiss = {
                        timePicker = false
                    }, onTimeSet = {
                        playerState?.startTimer(it)
                        timePicker = false
                    }
                )

            if (cancelSleepTimer) {
                Dialog(
                    onConfirmation = {
                        playerState?.cancelTimer()
                        cancelSleepTimer = false
                    },
                    onDismissRequest = {
                        cancelSleepTimer = false
                    },
                    dialogTitle = stringResource(R.string.sleep_timer_cancel),
                )
            }
        }
    }

    SongListBottomSheet(song = currentSong, onDismiss = {
        openSongListBottomSheet = false
    }, openBottomSheet = openSongListBottomSheet)

    currentSong?.let { song ->
        ArtistInfoSheet(
            song = song,
            onClick = onArtistClick,
            onDismiss = { openArtistInfoSheet = false },
            openBottomSheet = openArtistInfoSheet
        )
        AlbumInfoSheet(
            song = song,
            onClick = onAlbumClick,
            onDismiss = { openAlbumInfoSheet = false },
            openBottomSheet = openAlbumInfoSheet
        )
        DownloadQualityDialog(
            song = song,
            show = openDownloadDialog,
            onDismiss = { openDownloadDialog = false }
        )
    }

}