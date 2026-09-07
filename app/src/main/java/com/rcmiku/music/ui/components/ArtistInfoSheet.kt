package com.rcmiku.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rcmiku.music.R
import com.rcmiku.music.ui.icons.Artist
import com.rcmiku.music.utils.getItemShape
import com.rcmiku.ncmapi.model.Artist
import com.rcmiku.ncmapi.model.Song

/**
 * 歌手弹窗：列出当前歌曲的歌手，点击进入歌手主页（主页提供关注功能）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistInfoSheet(
    song: Song,
    onClick: (Artist) -> Unit,
    openBottomSheet: Boolean,
    onDismiss: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            sheetState = bottomSheetState
        ) {
            LazyColumn(
                Modifier.padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(song.ar) { index, artist ->
                    val shape = getItemShape(
                        prevItem = song.ar.getOrNull(index - 1),
                        nextItem = song.ar.getOrNull(index + 1),
                        corner = 16.dp,
                        subCorner = 4.dp,
                    )
                    Card(
                        shape = shape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    onDismiss()
                                    onClick(artist)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Artist,
                                contentDescription = null,
                                Modifier.padding(horizontal = 12.dp)
                            )
                            Text(
                                text = artist.name,
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
}
