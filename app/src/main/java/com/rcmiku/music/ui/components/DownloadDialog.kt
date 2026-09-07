package com.rcmiku.music.ui.components

import android.app.DownloadManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.rcmiku.music.R
import com.rcmiku.ncmapi.api.player.PlayerApi
import com.rcmiku.ncmapi.api.player.SongLevel
import com.rcmiku.ncmapi.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val SongLevel.labelRes: Int
    get() = when (this) {
        SongLevel.STANDARD -> R.string.quality_standard
        SongLevel.HIGHER -> R.string.quality_higher
        SongLevel.EXHIGH -> R.string.quality_exhigh
        SongLevel.LOSSLESS -> R.string.quality_lossless
        SongLevel.HIRES -> R.string.quality_hires
    }

internal fun SongLevel.extension(): String =
    if (this == SongLevel.LOSSLESS || this == SongLevel.HIRES) "flac" else "mp3"

internal fun sanitizeFileName(name: String): String =
    name.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank { "unknown" }

/**
 * 应用级下载协程作用域：对话框关闭（Composable 移除）后下载仍继续
 */
private val appDownloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private fun toastOnMain(context: Context, resId: Int) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
    }
}

/**
 * 下载对话框：选择音质后立即关闭，后台通过系统 DownloadManager 下载到公共下载目录
 */
@Composable
fun DownloadQualityDialog(
    song: Song?,
    show: Boolean,
    onDismiss: () -> Unit
) {
    if (!show || song == null) return
    var selectedLevel by remember { mutableStateOf(SongLevel.EXHIGH) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.download)) },
        text = {
            Column {
                Text(
                    text = song.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.ar.joinToString("/") { it.name },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    SongLevel.entries.forEach { level ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedLevel = level }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLevel == level,
                                onClick = { selectedLevel = level }
                            )
                            Text(
                                text = stringResource(level.labelRes),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val targetSong = song
                    val targetLevel = selectedLevel
                    val appContext = context.applicationContext
                    // 立即关闭对话框，后台继续下载
                    onDismiss()
                    appDownloadScope.launch {
                        downloadSong(appContext, targetSong, targetLevel)
                            .onSuccess { toastOnMain(appContext, R.string.download_started) }
                            .onFailure { toastOnMain(appContext, R.string.download_failed) }
                    }
                }
            ) {
                Text(text = stringResource(R.string.download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        }
    )
}

private suspend fun downloadSong(
    context: Context,
    song: Song,
    level: SongLevel
): Result<Unit> {
    return PlayerApi.songPlayUrlV1(song.id.toString(), level).mapCatching { response ->
        val url = response.data?.firstOrNull()?.url
            ?: throw IllegalStateException("无可用播放地址")
        val artist = song.ar.joinToString("/") { it.name }
        val fileName = sanitizeFileName("${song.name} - $artist.${level.extension()}")
        val request = DownloadManager.Request(url.toUri())
            .setTitle(song.name)
            .setDescription(artist)
            .setMimeType(if (level.extension() == "flac") "audio/flac" else "audio/mpeg")
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        val finalRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Mooic/$fileName"
            )
        } else {
            request.setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )
        }
        val downloadManager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(finalRequest)
    }
}
