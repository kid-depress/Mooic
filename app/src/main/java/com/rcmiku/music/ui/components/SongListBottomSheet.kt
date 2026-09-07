package com.rcmiku.music.ui.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rcmiku.music.R
import com.rcmiku.music.constants.userIdKye
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.api.account.PlayManipulateType
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.model.UserPlaylistV1Response
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongListBottomSheet(
    song: Song?,
    openBottomSheet: Boolean,
    onDismiss: () -> Unit,
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val userId by rememberPreference(userIdKye, 0)
    var playlist by remember { mutableStateOf<UserPlaylistV1Response?>(null) }
    var removeSong by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val selectedPlaylistId = remember { mutableStateOf<Long?>(null) }
    val selectedRemovePlaylistId = remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(openBottomSheet) {
        if (openBottomSheet) {
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
        }
    }

    LaunchedEffect(song?.id, userId) {
        val songId = song?.id
        if (songId != null && userId != 0L) {
            playlist = AccountApi.userPlaylistV1(
                userId = userId,
                trackIds = listOf(songId)
            ).getOrNull()
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
                playlist?.playlist?.let { items ->
                    items(items) {
                        val isSelected = selectedPlaylistId.value == it.id
                        Card(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (!it.containsTracks)
                                        selectedPlaylistId.value = it.id
                                    else {
                                        selectedRemovePlaylistId.value = it.id
                                        removeSong = true
                                    }

                                },
                            colors = if (isSelected) CardDefaults.cardColors()
                                .copy(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.cardColors()
                                .copy(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            PlaylistV1ListItem(playlist = it, trailingContent = {
                                if (it.containsTracks)
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(text = stringResource(R.string.collected))
                                    }
                            })
                        }
                    }

                    item {
                        Row(
                            Modifier
                                .padding(horizontal = 12.dp)
                                .padding(top = 12.dp)
                                .fillMaxWidth(), horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    onDismiss()
                                },
                            ) {
                                Text(text = stringResource(R.string.cancel))
                            }

                            Spacer(Modifier.width(12.dp))

                            Button(
                                onClick = {
                                    scope.launch {
                                        val selectedId = selectedPlaylistId.value
                                        val songId = song?.id
                                        if (selectedId != null && songId != null) {
                                            AccountApi.playlistManipulate(
                                                playlistId = selectedId,
                                                songIds = listOf(songId)
                                            ).onSuccess {
                                                Toast.makeText(
                                                    context,
                                                    context.getText(R.string.add_success),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }.onFailure {
                                                Toast.makeText(
                                                    context,
                                                    context.getText(R.string.add_fail),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            onDismiss()
                                        }
                                    }
                                },
                                enabled = selectedPlaylistId.value != null,
                            ) {
                                Text(text = stringResource(R.string.confirm))
                            }
                        }

                        if (removeSong) {
                            Dialog(
                                onConfirmation = {
                                    removeSong = false
                                    scope.launch {
                                        val selectedId = selectedRemovePlaylistId.value
                                        val songId = song?.id
                                        if (selectedId != null && songId != null) {
                                            AccountApi.playlistManipulate(
                                                playlistId = selectedId,
                                                songIds = listOf(songId),
                                                manipulateType = PlayManipulateType.DEL
                                            )
                                            onDismiss()
                                        }
                                    }
                                },
                                onDismissRequest = {
                                    removeSong = false
                                },
                                dialogTitle = stringResource(R.string.remove_from_songList),
                            )
                        }
                    }
                }
            }
        }
    }
}