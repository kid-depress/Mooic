package com.rcmiku.music.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.rcmiku.music.LocalPlayerController
import com.rcmiku.music.LocalPlayerState
import com.rcmiku.music.R
import com.rcmiku.music.extensions.addSong
import com.rcmiku.music.ui.components.AlbumListItem
import com.rcmiku.music.ui.components.ArtistListItem
import com.rcmiku.music.ui.components.PlaylistListItem
import com.rcmiku.music.ui.components.SongListItem
import com.rcmiku.music.ui.components.SongMenuBottomSheet
import com.rcmiku.music.ui.components.VoiceListItem
import com.rcmiku.music.ui.icons.ArrowInsert
import com.rcmiku.music.ui.icons.History
import com.rcmiku.music.ui.icons.Remove
import com.rcmiku.music.ui.icons.Search
import com.rcmiku.music.ui.navigation.AlbumNav
import com.rcmiku.music.ui.navigation.ArtistNav
import com.rcmiku.music.ui.navigation.PlaylistNav
import com.rcmiku.music.ui.navigation.RadioNav
import com.rcmiku.music.viewModel.SearchViewModel
import com.rcmiku.ncmapi.api.search.SearchType
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.model.toAlbumList
import com.rcmiku.ncmapi.model.toPlaylist
import com.rcmiku.ncmapi.model.toSearchArtist
import com.rcmiku.ncmapi.model.toSearchUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavHostController,
    searchViewModel: SearchViewModel = hiltViewModel()
) {

    var searchValue by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val searchType by searchViewModel.searchType.collectAsState()
    val searchResults by searchViewModel.searchResults.collectAsState()
    val mediaController = LocalPlayerController.current.controller
    val playerState = LocalPlayerState.current
    val isPlaying = playerState?.isPlaying == true
    val currentMediaId = playerState?.currentMediaItem?.mediaId?.toLongOrNull()
    val searchHistoryState by searchViewModel.searchHistory.collectAsState(initial = emptyList())
    val suggestKeywordResponseState by searchViewModel.suggestKeywordResponse.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current
    var state by rememberSaveable { mutableIntStateOf(0) }
    val tab = listOf(
        SearchType.Song to stringResource(R.string.song),
        SearchType.Playlist to stringResource(R.string.playlists),
        SearchType.VoiceList to stringResource(R.string.voice_list),
        SearchType.Artist to stringResource(R.string.artist),
        SearchType.Album to stringResource(R.string.album)
    )
    var openBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectSong by remember { mutableStateOf<Song?>(null) }

    // SearchBar 顶部 = 状态栏 + 自带 8dp 内边距;这里抵消 4dp,让搜索框更贴近状态栏
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val searchBarTopInset = (statusBarTop - 4.dp).coerceAtLeast(0.dp)
    // 搜索框实际底边 = searchBarTopInset + 8dp(内部) + 56dp(输入框),Tab 栏再留 8dp 间距
    val searchResultsTop = searchBarTopInset + 72.dp

    Box(
        Modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true }) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f },
            windowInsets = WindowInsets(0.dp, searchBarTopInset, 0.dp, 0.dp),
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchValue,
                    onQueryChange = {
                        searchValue = it
                        if (it.isNotEmpty())
                            searchViewModel.fetchSearchKeyword(it)
                    },
                    onSearch = {
                        expanded = false
                        searchViewModel.updateSearchValue(it)
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = it
                    },
                    placeholder = { Text(stringResource(R.string.search)) },
                    leadingIcon = {
                        IconButton(onClick = {
                            if (!expanded)
                                navController.navigateUp()
                            else
                                expanded = false
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = null
                            )
                        }
                    }
                )
            },
            expanded = expanded,
            onExpandedChange = {
                expanded = it
            },
            content = {
                LazyColumn {
                    suggestKeywordResponseState?.data?.suggests?.let {
                        items(it) { item ->
                            SuggestItem(history = false, query = item.keyword, onInsert = {
                                searchValue = item.keyword
                                focusManager.clearFocus()
                            }, onClick = {
                                searchValue = item.keyword
                                expanded = false
                                searchViewModel.updateSearchValue(item.keyword)
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }, onDelete = {

                            })
                        }
                    }

                    if (searchHistoryState.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "搜索记录",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                TextButton(
                                    onClick = {
                                        searchViewModel.clearSearchHistory()
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Remove,
                                        contentDescription = null
                                    )
                                    Text(text = "清空")
                                }
                            }
                        }
                    }

                    items(searchHistoryState) {
                        SuggestItem(history = true, query = it, onInsert = {
                            searchValue = it
                            focusManager.clearFocus()
                        }, onClick = {
                            searchValue = it
                            expanded = false
                            searchViewModel.updateSearchValue(it)
                            focusManager.moveFocus(FocusDirection.Right)
                            keyboardController?.hide()
                        }, onDelete = {
                            searchViewModel.deleteSearchQuery(it)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        })
                    }
                }
            }
        )

        Column(Modifier.padding(top = searchResultsTop)) {
            SecondaryTabRow(
                selectedTabIndex = state,
                indicator = {
                    FancyIndicator(
                        MaterialTheme.colorScheme.primary,
                        Modifier.tabIndicatorOffset(state)
                    )
                }
            ) {
                tab.forEachIndexed { index, item ->
                    Tab(
                        modifier = Modifier.clip(MaterialTheme.shapes.small),
                        selected = state == index,
                        onClick = {
                            state = index
                            searchViewModel.updateSearchType(item.first)
                        },
                        text = { Text(item.second) })
                }
            }

            LazyColumn(
                modifier = Modifier.semantics { traversalIndex = 1f }
            ) {

                when (searchType) {
                    SearchType.Album -> {
                        items(searchResults.size) { index ->
                            searchResults.getOrNull(index)?.let { resource ->
                                resource.toAlbumList()?.let {
                                    AlbumListItem(album = it, modifier = Modifier.clickable {
                                        navController.navigate(AlbumNav(albumId = it.id))
                                    })
                                }
                            }
                        }
                    }

                    SearchType.Artist -> {
                        items(searchResults.size) { index ->
                            searchResults.getOrNull(index)?.let { resource ->
                                resource.toSearchArtist()?.let {
                                    ArtistListItem(artist = it, modifier = Modifier.clickable {
                                        navController.navigate(ArtistNav(artistId = it.id))
                                    })
                                }
                            }
                        }
                    }

                    SearchType.Playlist -> {
                        items(searchResults.size) { index ->
                            searchResults.getOrNull(index)?.let { resource ->
                                resource.toPlaylist()?.let {
                                    PlaylistListItem(playlist = it, Modifier.clickable {
                                        navController.navigate(
                                            PlaylistNav(
                                                playlistId = it.id,
                                                limit = it.trackCount
                                            )
                                        )
                                    })
                                }
                            }
                        }
                    }

                    SearchType.Song -> {
                        items(searchResults.size) { index ->
                            searchResults.getOrNull(index)?.let { resource ->
                                resource.song?.let { song ->
                                    SongListItem(
                                        isPlaying = isPlaying,
                                        isActive = currentMediaId == song.id,
                                        song = song,
                                        songIndex = index + 1,
                                        modifier = Modifier.clickable {
                                            mediaController?.addSong(song)
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
                                        })
                                }
                            }
                        }

                    }

                    SearchType.VoiceList -> {
                        items(searchResults.size) { index ->
                            searchResults.getOrNull(index)?.let { resource ->
                                resource.baseInfo?.let { voice ->
                                    VoiceListItem(voice = voice, modifier = Modifier.clickable {
                                        navController.navigate(
                                            RadioNav(radioId = voice.id)
                                        )
                                    }
                                    )
                                }
                            }
                        }
                    }

                    SearchType.User -> {
                        items(searchResults.size) { index ->
                            searchResults.getOrNull(index)?.let { resource ->
                                resource.toSearchUser()?.let { user ->
                                    ArtistListItem(
                                        artist = com.rcmiku.ncmapi.model.SearchArtist(
                                            id = user.id,
                                            name = user.nickname,
                                            picUrl = user.avatarUrl,
                                            briefDesc = user.signature
                                        ),
                                        modifier = Modifier.clickable {
                                            navController.navigate(ArtistNav(artistId = user.id))
                                        }
                                    )
                                }
                            }
                        }
                    }

                    SearchType.Radio -> {
                        items(searchResults.size) { index ->
                            searchResults.getOrNull(index)?.let { resource ->
                                resource.baseInfo?.let { voice ->
                                    VoiceListItem(voice = voice, modifier = Modifier.clickable {
                                        navController.navigate(RadioNav(radioId = voice.id))
                                    })
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.navigationBarsPadding())
                }
            }
        }
    }

    SongMenuBottomSheet(
        navController = navController,
        song = selectSong,
        onDismiss = { openBottomSheet = false },
        openBottomSheet = openBottomSheet
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuggestItem(
    history: Boolean,
    modifier: Modifier = Modifier,
    query: String,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onInsert: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .combinedClickable(onClick = {
                onClick()
            }, onLongClick = {
                onDelete()
            })
    ) {
        Icon(
            imageVector = if (history) History else Search,
            contentDescription = null,
            modifier = Modifier
                .fillMaxHeight()
                .padding(12.dp)
        )
        Text(
            text = query,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onInsert,
        ) {
            Icon(
                imageVector = ArrowInsert,
                contentDescription = null
            )
        }
    }
}
