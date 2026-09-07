package com.rcmiku.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.extensions.playMediaAtId
import com.rcmiku.music.extensions.setPlaylist
import com.rcmiku.music.ui.components.AlbumListItem
import com.rcmiku.music.ui.components.NavigationTitle
import com.rcmiku.music.ui.components.SongListItem
import com.rcmiku.music.ui.components.SongMenuBottomSheet
import com.rcmiku.music.ui.navigation.AlbumNav
import com.rcmiku.music.utils.CoverImageSize
import com.rcmiku.music.utils.toCoverImageUrl
import com.rcmiku.music.viewModel.ArtistScreenViewModel
import com.rcmiku.ncmapi.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    navController: NavHostController,
    artistScreenViewModel: ArtistScreenViewModel = hiltViewModel()
) {
    val artistHeadInfoState by artistScreenViewModel.artistHeadInfo.collectAsState()
    val artistTopSongState by artistScreenViewModel.artistTopSong.collectAsState()
    val subscribed by artistScreenViewModel.subscribed.collectAsState()
    val subToggling by artistScreenViewModel.subToggling.collectAsState()
    val artistAlbumList = artistScreenViewModel.artistAlbumList.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val showPlaylistTitle by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    var state by rememberSaveable { mutableIntStateOf(1) }
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId?.toLongOrNull()
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectSong by remember { mutableStateOf<Song?>(null) }

    val titles = listOf(
        stringResource(R.string.home),
        stringResource(R.string.song),
        stringResource(R.string.album)
    )

    LazyColumn(state = listState) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomStart
            ) {
                AsyncImage(
                    model = artistHeadInfoState?.data?.artist?.cover.toCoverImageUrl(CoverImageSize.HERO),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.aspectRatio(4f / 3f)
                )
                Row(
                    modifier = Modifier.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    artistHeadInfoState?.data?.artist?.name?.let {
                        Box(
                            Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(color = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = it,
                                Modifier.padding(4.dp),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    subscribed?.let { isSubbed ->
                        Spacer(Modifier.width(8.dp))
                        if (isSubbed) {
                            OutlinedButton(
                                onClick = { artistScreenViewModel.toggleSub() },
                                enabled = !subToggling
                            ) {
                                Text(text = stringResource(R.string.followed))
                            }
                        } else {
                            Button(
                                onClick = { artistScreenViewModel.toggleSub() },
                                enabled = !subToggling,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text(text = stringResource(R.string.follow))
                            }
                        }
                    }
                }
            }
        }
        item {
            Column {
                SecondaryTabRow(selectedTabIndex = state) {
                    titles.forEachIndexed { index, title ->
                        Tab(
                            selected = state == index,
                            onClick = { state = index },
                            text = {
                                Text(
                                    text = title,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }

        when (state) {
            0 -> {
                item {
                    NavigationTitle(
                        title = stringResource(R.string.artist_info),
                        modifier = Modifier.animateItem()
                    )
                    artistHeadInfoState?.data?.artist?.briefDesc?.trimIndent()?.let {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            if (it.isNotBlank())
                                Text(
                                    text = it,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            else
                                Text(
                                    text = stringResource(R.string.no_brief),
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                        }
                    }
                }
            }

            1 -> {
                artistTopSongState?.songs?.let {
                    itemsIndexed(it) { index, song ->
                        SongListItem(
                            song = song,
                            isPlaying = isPlaying,
                            isActive = currentMediaId == song.id,
                            songIndex = index + 1,
                            modifier = Modifier.clickable {
                                mediaController?.setPlaylist(it)
                                mediaController?.playMediaAtId(song.id)
                            },
                            trailingContent = {
                                IconButton(onClick = {
                                    selectSong = song
                                    openBottomSheet = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.more)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            2 -> {
                items(artistAlbumList.itemCount) { index ->
                    artistAlbumList[index]?.let {
                        AlbumListItem(album = it, modifier = Modifier.clickable {
                            navController.navigate(AlbumNav(albumId = it.id))
                        })
                    }
                }
            }
        }

        item {
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    TopAppBar(
        title = {
            Text(
                text = if (showPlaylistTitle) artistHeadInfoState?.data?.artist?.name ?: "" else "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    navController.navigateUp()
                },
                colors = if (showPlaylistTitle) IconButtonDefaults.iconButtonColors() else IconButtonDefaults.filledIconButtonColors()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                )
            }
        },
        modifier = Modifier.background(
            Brush.verticalGradient(
                listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    Color.Transparent
                )
            )
        ),
        colors = if (showPlaylistTitle) TopAppBarDefaults.topAppBarColors() else TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )

    SongMenuBottomSheet(
        navController = navController,
        song = selectSong,
        onDismiss = { openBottomSheet = false },
        openBottomSheet = openBottomSheet
    )
}
