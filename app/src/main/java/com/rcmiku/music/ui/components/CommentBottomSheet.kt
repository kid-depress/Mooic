package com.rcmiku.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.rcmiku.music.R
import com.rcmiku.music.ui.icons.ThumbUp
import com.rcmiku.music.viewModel.CommentViewModel
import com.rcmiku.ncmapi.model.Comment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    songId: Long?,
    songName: String?,
    openBottomSheet: Boolean,
    onDismiss: () -> Unit,
    commentViewModel: CommentViewModel = hiltViewModel()
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val comments by commentViewModel.comments.collectAsState()
    val loading by commentViewModel.loading.collectAsState()

    LaunchedEffect(openBottomSheet, songId) {
        if (openBottomSheet) {
            songId?.let { commentViewModel.fetchComments(it) }
            bottomSheetState.show()
        } else {
            bottomSheetState.hide()
        }
    }

    if (openBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = bottomSheetState,
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            Column(Modifier.fillMaxHeight()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = songName.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.comment_count, comments?.total?.toInt() ?: 0),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when {
                    loading && comments == null -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }

                    comments == null -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.comment_load_failed),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> {
                        val data = comments ?: return@Column
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (data.hotComments.isNotEmpty()) {
                                item(key = "hot_title") {
                                    SectionTitle(stringResource(R.string.comment_hot))
                                }
                                items(data.hotComments, key = { "hot_${it.user.userId}_${it.time}" }) { comment ->
                                    CommentItem(comment)
                                }
                            }
                            if (data.comments.isNotEmpty()) {
                                item(key = "latest_title") {
                                    SectionTitle(stringResource(R.string.comment_latest))
                                }
                                items(data.comments, key = { "latest_${it.user.userId}_${it.time}" }) { comment ->
                                    CommentItem(comment)
                                }
                            }
                            if (data.hotComments.isEmpty() && data.comments.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.comment_empty),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun CommentItem(comment: Comment) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = comment.user.avatarUrl.ifBlank { null },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = comment.user.nickname,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatCommentTime(comment.time),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = ThumbUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = formatCount(comment.likedCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        comment.beReplied.firstOrNull()?.let { replied ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = "@${replied.user.nickname}: ${replied.content}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = comment.content,
            style = MaterialTheme.typography.bodyMedium
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun formatCommentTime(time: Long): String {
    if (time <= 0) return ""
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(time))
}

private fun formatCount(count: Long): String = when {
    count >= 10000 -> String.format(Locale.getDefault(), "%.1f万", count / 10000.0)
    else -> count.toString()
}
