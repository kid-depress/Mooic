package com.rcmiku.music.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcmiku.ncmapi.api.comment.CommentApi
import com.rcmiku.ncmapi.model.CommentResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommentViewModel @Inject constructor() : ViewModel() {

    private val _comments = MutableStateFlow<CommentResponse?>(null)
    val comments: StateFlow<CommentResponse?> = _comments.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun fetchComments(songId: Long) {
        viewModelScope.launch {
            _loading.value = true
            _comments.value = CommentApi.songComments(songId).getOrNull()
            _loading.value = false
        }
    }
}
