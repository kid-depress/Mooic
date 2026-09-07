package com.rcmiku.music.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.ncmapi.api.fm.PersonalFmApi
import com.rcmiku.ncmapi.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FMScreenViewModel @Inject constructor() : ViewModel() {

    private val _fmSongs = MutableStateFlow<List<Song>?>(null)
    val fmSongs: StateFlow<List<Song>?> = _fmSongs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        fetchFm()
    }

    fun fetchFm() {
        if (_loading.value) return
        _loading.value = true
        viewModelScope.launch {
            PersonalFmApi.personalFm()
                .onSuccess { _fmSongs.value = it }
                .onFailure { /* 失败时保留上一批歌曲 */ }
            _loading.value = false
        }
    }
}
