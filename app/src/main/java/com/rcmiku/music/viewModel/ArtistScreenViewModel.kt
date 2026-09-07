package com.rcmiku.music.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.rcmiku.music.paging.ArtistAlbumPagingSource
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.api.artist.ArtistApi
import com.rcmiku.ncmapi.model.ArtistHeadInfoResponse
import com.rcmiku.ncmapi.model.ArtistTopSong
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistScreenViewModel @Inject constructor(savedStateHandle: SavedStateHandle) : ViewModel() {
    private val artistId = savedStateHandle.get<Long>("artistId")

    private val _artistHeadInfo =
        MutableStateFlow<ArtistHeadInfoResponse?>(null)
    val artistHeadInfo: StateFlow<ArtistHeadInfoResponse?> =
        _artistHeadInfo.asStateFlow()
    private val _artistTopSong =
        MutableStateFlow<ArtistTopSong?>(null)
    val artistTopSong: StateFlow<ArtistTopSong?> =
        _artistTopSong.asStateFlow()

    /** 关注状态：null=未登录/未知，true=已关注，false=未关注 */
    private val _subscribed = MutableStateFlow<Boolean?>(null)
    val subscribed: StateFlow<Boolean?> = _subscribed.asStateFlow()

    private val _subToggling = MutableStateFlow(false)
    val subToggling: StateFlow<Boolean> = _subToggling.asStateFlow()

    init {
        viewModelScope.launch {
            artistId?.let {
                _artistHeadInfo.value = ArtistApi.artistHeadInfo(it).getOrNull()
                _artistTopSong.value = ArtistApi.artistTopSong(it).getOrNull()
            }
        }
        viewModelScope.launch {
            artistId?.let { id ->
                _subscribed.value =
                    AccountApi.artistSublist().getOrNull()?.any { it.id == id }
            }
        }
    }

    fun toggleSub() {
        val id = artistId ?: return
        val current = _subscribed.value ?: return
        if (_subToggling.value) return
        viewModelScope.launch {
            _subToggling.value = true
            AccountApi.artistSub(id, sub = !current)
                .onSuccess { _subscribed.value = !current }
            _subToggling.value = false
        }
    }

    val artistAlbumList = artistId?.let { id ->
        Pager(
            config = PagingConfig(
                pageSize = 100,
                prefetchDistance = 50,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { ArtistAlbumPagingSource(id) }
        ).flow.cachedIn(viewModelScope)
    } ?: flowOf()

}