package com.rcmiku.music.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rcmiku.music.utils.dataStore
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.api.account.UserPlaylistType
import com.rcmiku.ncmapi.model.FavoriteSongResponse
import com.rcmiku.ncmapi.model.Playlist
import com.rcmiku.ncmapi.model.UserInfoBatch
import com.rcmiku.ncmapi.utils.json
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryScreenViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {
    private val cachedUserInfoKey = stringPreferencesKey("cachedUserInfo")
    private val cachedFavoriteSongKey = stringPreferencesKey("cachedFavoriteSong")
    private val cachedUserPlaylistsKey = stringPreferencesKey("cachedUserPlaylists")
    private val _userInfo = MutableStateFlow<UserInfoBatch?>(null)
    val userInfo: StateFlow<UserInfoBatch?> = _userInfo.asStateFlow()

    private val _favoriteSong = MutableStateFlow<FavoriteSongResponse?>(null)
    val favoriteSong: StateFlow<FavoriteSongResponse?> = _favoriteSong.asStateFlow()

    private val _userPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val userPlaylists: StateFlow<List<Playlist>> = _userPlaylists.asStateFlow()

    init {
        viewModelScope.launch {
            context.dataStore.data.first().let { preferences ->
                preferences[cachedUserInfoKey]?.let { cached -> runCatching { json.decodeFromString<UserInfoBatch>(cached) }.onSuccess { _userInfo.value = it } }
                preferences[cachedFavoriteSongKey]?.let { cached -> runCatching { json.decodeFromString<FavoriteSongResponse>(cached) }.onSuccess { _favoriteSong.value = it } }
                preferences[cachedUserPlaylistsKey]?.let { cached -> runCatching { json.decodeFromString<List<Playlist>>(cached) }.onSuccess { _userPlaylists.value = it } }
            }
            observeUserIdChanges()
        }
    }

    fun fetchUserInfo() {
        viewModelScope.launch {
            AccountApi.accountInfo().onSuccess {
                _userInfo.value = it
                context.dataStore.edit { preferences ->
                    preferences[cachedUserInfoKey] = json.encodeToString(it)
                    // 持久化 userId，供"添加到歌单"、喜欢等功能使用
                    preferences[com.rcmiku.music.constants.userIdKye] = it.account.profile.userId
                }
            }
        }
    }

    private fun fetchFavoriteSong(userId: Long) {
        viewModelScope.launch {
            AccountApi.favoriteSong(userId).onSuccess {
                _favoriteSong.value = it
                context.dataStore.edit { preferences -> preferences[cachedFavoriteSongKey] = json.encodeToString(it) }
            }
        }
    }

    private fun fetchUserPlaylists(userId: Long) {
        viewModelScope.launch {
            AccountApi.userPlaylist(
                userId = userId,
                userPlaylistType = UserPlaylistType.CREATE
            ).onSuccess { response ->
                val playlists = response.data?.playlist.orEmpty()
                _userPlaylists.value = playlists
                context.dataStore.edit { preferences -> preferences[cachedUserPlaylistsKey] = json.encodeToString(playlists) }
            }
        }
    }

    fun clear() {
        _userInfo.value = null
        _favoriteSong.value = null
        _userPlaylists.value = emptyList()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeUserIdChanges() {
        viewModelScope.launch {
            userInfo
                .mapLatest { it?.account?.profile?.userId }
                .distinctUntilChanged()
                .collectLatest { userId ->
                    if (userId == null) {
                        _favoriteSong.value = null
                        _userPlaylists.value = emptyList()
                    } else {
                        fetchFavoriteSong(userId)
                        fetchUserPlaylists(userId)
                    }
                }
        }
    }
}
