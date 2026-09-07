package com.rcmiku.music.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.rcmiku.music.R
import com.rcmiku.music.constants.ncmCookieKey
import com.rcmiku.music.ui.components.TopBar
import com.rcmiku.music.ui.icons.Favorite
import com.rcmiku.music.ui.icons.Login
import com.rcmiku.music.ui.icons.VipFill
import com.rcmiku.music.ui.navigation.PlaylistNav
import com.rcmiku.music.ui.navigation.Screen
import com.rcmiku.music.utils.CoverImageSize
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.music.utils.toCoverImageUrl
import com.rcmiku.music.viewModel.LibraryScreenViewModel
import com.rcmiku.ncmapi.model.Playlist
import com.rcmiku.ncmapi.model.UserInfoBatch

@Composable
fun LibraryScreen(
    navController: NavHostController,
    libraryScreenViewModel: LibraryScreenViewModel = hiltViewModel()
) {
    val ncmCookie by rememberPreference(ncmCookieKey, "")
    val userInfoBatchState by libraryScreenViewModel.userInfo.collectAsState()
    val favoriteSongState by libraryScreenViewModel.favoriteSong.collectAsState()
    val userPlaylists by libraryScreenViewModel.userPlaylists.collectAsState()

    LaunchedEffect(ncmCookie) {
        if (ncmCookie.isNotEmpty()) {
            libraryScreenViewModel.fetchUserInfo()
        } else {
            libraryScreenViewModel.clear()
        }
    }

    val userId = userInfoBatchState?.account?.profile?.userId ?: 0L
    val favoritePlaylistId = favoriteSongState?.data?.id
    val favoritePlaylist = userPlaylists.firstOrNull { it.id == favoritePlaylistId }
    val normalPlaylists = userPlaylists.filterNot { it.id == favoritePlaylistId }
    val createdPlaylists = normalPlaylists.filter { it.creator?.userId == userId }
    val collectedPlaylists = normalPlaylists.filter { it.creator?.userId != userId }

    Scaffold(
        topBar = {
            TopBar(navController = navController, titleRes = R.string.mine)
        }
    ) { padding ->
        if (ncmCookie.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(24.dp)
                        .clickable { navController.navigate(Screen.Login.route) },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            imageVector = Login,
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = stringResource(R.string.click_to_login),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        } else {
            LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 12.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            userInfoBatchState?.let {
                item {
                    LibraryUserCard(userInfo = it)
                }
            }

            item {
                FmEntryCard(
                    onClick = { navController.navigate(Screen.Fm.route) }
                )
            }

            favoritePlaylist?.let { playlist ->
                item {
                    FavoritePlaylistCard(
                        playlist = playlist,
                        onClick = {
                            navController.navigate(
                                PlaylistNav(
                                    playlistId = playlist.id,
                                    noCache = true
                                )
                            )
                        }
                    )
                }
            }

            if (createdPlaylists.isNotEmpty()) {
                item {
                    PlaylistGroupCard(
                        title = stringResource(R.string.create_playlist),
                        playlists = createdPlaylists,
                        onPlaylistClick = { playlist ->
                            navController.navigate(
                                PlaylistNav(
                                    playlistId = playlist.id,
                                    limit = playlist.trackCount
                                )
                            )
                        }
                    )
                }
            }

            if (collectedPlaylists.isNotEmpty()) {
                item {
                    PlaylistGroupCard(
                        title = stringResource(R.string.collect_playlist),
                        playlists = collectedPlaylists,
                        onPlaylistClick = { playlist ->
                            navController.navigate(
                                PlaylistNav(
                                    playlistId = playlist.id,
                                    limit = playlist.trackCount
                                )
                            )
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
        }
    }
}

@Composable
private fun LibraryUserCard(userInfo: UserInfoBatch) {
    val profile = userInfo.account.profile
    val secondaryText = profile.signature.takeIf { it.isNotBlank() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 38.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = profile.nickname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                secondaryText?.let {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier.size(76.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .clip(CircleShape)
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    .size(76.dp)
            )

            if (profile.vipType != 0) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(6.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = VipFill,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FmEntryCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FM",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Column(
                    modifier = Modifier.padding(start = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.personal_fm),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.fm_entry_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritePlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp)
        ) {
            RowCardContent(
                title = playlist.name,
                subtitle = stringResource(R.string.track_count, playlist.trackCount),
                coverUrl = playlist.picUrl
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 38.dp, top = 38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .padding(5.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun PlaylistGroupCard(
    title: String,
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            playlists.forEachIndexed { index, playlist ->
                LibraryPlaylistRow(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist) }
                )
                if (index != playlists.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 3.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryPlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit
) {
    val creatorName = playlist.creator?.nickname.orEmpty()
    val subtitle = buildString {
        append(stringResource(R.string.track_count, playlist.trackCount))
        if (creatorName.isNotEmpty()) {
            append(" by ")
            append(creatorName)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(vertical = 1.dp)
    ) {
        RowCardContent(
            title = playlist.name,
            subtitle = subtitle,
            coverUrl = playlist.picUrl
        )
    }
}

@Composable
private fun RowCardContent(
    title: String,
    subtitle: String,
    coverUrl: String
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = coverUrl.toCoverImageUrl(CoverImageSize.LIST),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .clip(MaterialTheme.shapes.medium)
                .size(52.dp)
        )

        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
