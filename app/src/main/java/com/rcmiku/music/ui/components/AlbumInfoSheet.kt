package com.rcmiku.music.ui.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rcmiku.music.R
import com.rcmiku.music.constants.ncmCookieKey
import com.rcmiku.music.ui.icons.Album
import com.rcmiku.music.utils.CoverImageSize
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.music.utils.toCoverImageUrl
import com.rcmiku.ncmapi.api.account.AccountApi
import com.rcmiku.ncmapi.model.Song
import com.rcmiku.ncmapi.model.SongAlbum
import kotlinx.coroutines.launch

/**
 * 专辑信息弹窗：展示当前歌曲所属专辑，支持收藏/取消收藏，点击进入专辑页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumInfoSheet(
    song: Song,
    onClick: (SongAlbum) -> Unit,
    openBottomSheet: Boolean,
    onDismiss: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ncmCookie by rememberPreference(ncmCookieKey, "")
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var subbed by remember { mutableStateOf(false) }
    var toggling by remember { mutableStateOf(false) }

    LaunchedEffect(openBottomSheet, song.id) {
        if (openBottomSheet) {
            bottomSheetState.show()
            subbed = if (ncmCookie.isNotEmpty()) {
                AccountApi.albumSubList().getOrNull()?.any { it.id == song.al.id } ?: false
            } else {
                false
            }
        } else {
            bottomSheetState.hide()
        }
    }

    if (openBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onClick(song.al)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.al.picUrl.toCoverImageUrl(CoverImageSize.LIST),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .size(64.dp)
                        )
                        Column(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = song.al.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.ar.joinToString("/") { it.name },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Album,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                if (subbed) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                toggling = true
                                AccountApi.albumSub(song.al.id, sub = false)
                                    .onSuccess {
                                        subbed = false
                                        Toast.makeText(
                                            context,
                                            R.string.uncollected,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .onFailure {
                                        Toast.makeText(
                                            context,
                                            R.string.operate_failed,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                toggling = false
                            }
                        },
                        enabled = !toggling
                    ) {
                        Text(text = stringResource(R.string.collected))
                    }
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                toggling = true
                                AccountApi.albumSub(song.al.id, sub = true)
                                    .onSuccess {
                                        subbed = true
                                        Toast.makeText(
                                            context,
                                            R.string.collected_success,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .onFailure {
                                        Toast.makeText(
                                            context,
                                            R.string.operate_failed,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                toggling = false
                            }
                        },
                        enabled = !toggling
                    ) {
                        Text(text = stringResource(R.string.collect))
                    }
                }

                Text(
                    text = stringResource(R.string.album_click_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
