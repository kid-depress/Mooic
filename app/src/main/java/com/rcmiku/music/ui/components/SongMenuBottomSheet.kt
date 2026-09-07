package com.rcmiku.music.ui.components

import android.content.Intent
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.R
import com.rcmiku.music.constants.userIdKye
import com.rcmiku.music.data.favoriteSongIdsDatastore
import com.rcmiku.music.extensions.addToPlaylist
import com.rcmiku.music.extensions.insertToPlaylist
import com.rcmiku.music.ui.icons.Album
import com.rcmiku.music.ui.icons.Artist
import com.rcmiku.music.ui.icons.Favorite
import com.rcmiku.music.ui.icons.FavoriteFill
import com.rcmiku.music.ui.icons.PlaylistAdd
import com.rcmiku.music.ui.icons.PlaylistInsert
import com.rcmiku.music.ui.icons.SongListAdd
import com.rcmiku.music.ui.navigation.AlbumNav
import com.rcmiku.music.ui.navigation.ArtistNav
import com.rcmiku.music.utils.FavoriteSongIdsUtil
import com.rcmiku.music.utils.CoverImageSize
import com.rcmiku.music.utils.makeTimeString
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.music.utils.reportLikeFailure
import com.rcmiku.music.utils.toCoverImageUrl
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.model.Song
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongMenuBottomSheet(
    song: Song?,
    openBottomSheet: Boolean,
    onDismiss: () -> Unit,
    navController: NavHostController,
) {
    var openArtistBottomSheet by rememberSaveable { mutableStateOf(false) }
    var openAlbumInfoSheet by rememberSaveable { mutableStateOf(false) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var openSongListBottomSheet by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val userId by rememberPreference(userIdKye, 0L)
    val mediaController = LocalPlayerController.current.controller
    val songIds by context.favoriteSongIdsDatastore.data.map { it.songIdsList }
        .collectAsState(emptyList())

    val scope = rememberCoroutineScope()

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
                    song?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .height(64.dp)
                                .fillMaxWidth()
                        ) {
                            AsyncImage(
                                model = it.al.picUrl.toCoverImageUrl(CoverImageSize.LIST),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(MaterialTheme.shapes.small)
                            )

                            Spacer(modifier = Modifier.width(6.dp))
                            Column(
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                Text(
                                    text = it.name,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.basicMarquee()
                                )
                                Text(
                                    text = it.ar.joinToString("/") { it.name },
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.basicMarquee(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = makeTimeString(it.dt),
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.basicMarquee(),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                }

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
                                    openArtistBottomSheet = true
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
                                    song?.let { mediaController?.insertToPlaylist(song = it) }
                                    onDismiss()
                                }, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = PlaylistInsert,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(R.string.insert_to_playlist),
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
                                    song?.let { mediaController?.addToPlaylist(song = it) }
                                    onDismiss()
                                }, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = PlaylistAdd,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(R.string.add_to_playlist),
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
                                    song?.id?.let { songId ->
                                        scope.launch {
                                            val like = songId !in songIds
                                            AccountApi.songLike(like, songId, userId).onSuccess {
                                                if (like)
                                                    FavoriteSongIdsUtil.addSongId(context, songId)
                                                else
                                                    FavoriteSongIdsUtil.removeSongId(
                                                        context,
                                                        songId
                                                    )
                                            }.onFailure { error ->
                                                reportLikeFailure(context, like, songId, userId, error)
                                            }
                                        }
                                    }
                                }, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (songIds.contains(song?.id)) FavoriteFill else Favorite,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = stringResource(if (songIds.contains(song?.id)) R.string.unlike else R.string.like),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                item {
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
                                .clickable(onClick = {
                                    song?.id?.let {
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
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    song?.let { song ->
        ArtistInfoSheet(
            song = song,
            onClick = { artist ->
                navController.navigate(ArtistNav(artistId = artist.id))
            },
            onDismiss = {
                openArtistBottomSheet = false
            },
            openBottomSheet = openArtistBottomSheet
        )
        AlbumInfoSheet(
            song = song,
            onClick = { album ->
                navController.navigate(AlbumNav(albumId = album.id))
            },
            onDismiss = {
                openAlbumInfoSheet = false
            },
            openBottomSheet = openAlbumInfoSheet
        )
        SongListBottomSheet(song = song, onDismiss = {
            openSongListBottomSheet = false
        }, openBottomSheet = openSongListBottomSheet)
    }
}
