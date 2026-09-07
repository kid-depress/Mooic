package com.rcmiku.music.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.extensions.playMediaAt
import com.rcmiku.music.extensions.playMediaAtId
import com.rcmiku.music.extensions.setPlaylist
import com.rcmiku.music.ui.components.SongListItem
import com.rcmiku.music.viewModel.FMScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FMScreen(
    navController: NavHostController,
    fmScreenViewModel: FMScreenViewModel = hiltViewModel()
) {
    val fmSongs by fmScreenViewModel.fmSongs.collectAsState()
    val loading by fmScreenViewModel.loading.collectAsState()
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId?.toLongOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.personal_fm),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { fmScreenViewModel.fetchFm() }) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding())
        ) {
            item {
                FmHeader(
                    songCount = fmSongs?.size ?: 0,
                    onPlayAll = {
                        fmSongs?.let { songs ->
                            if (songs.isNotEmpty()) {
                                mediaController?.setPlaylist(
                                    songs,
                                    sourceName = "fm"
                                )
                                mediaController?.playMediaAt()
                            }
                        }
                    }
                )
            }

            when {
                loading && fmSongs == null -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                fmSongs == null -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.load_failed),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    itemsIndexed(fmSongs!!) { index, song ->
                        SongListItem(
                            song = song,
                            isPlaying = isPlaying,
                            showLikedIcon = false,
                            isActive = currentMediaId == song.id,
                            songIndex = index + 1,
                            modifier = Modifier.clickable {
                                mediaController?.setPlaylist(
                                    fmSongs ?: emptyList(),
                                    sourceName = "fm"
                                )
                                mediaController?.playMediaAtId(song.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FmHeader(
    songCount: Int,
    onPlayAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "FM",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(Modifier.size(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.personal_fm),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.fm_hint, songCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onPlayAll,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(text = stringResource(R.string.play))
            }
        }
    }
}
