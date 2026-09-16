package com.rcmiku.music.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rcmiku.music.R

val AudioCacheSizeOptions = listOf(128L, 256L, 512L, 1024L, 2048L)
    .map { it * 1024 * 1024 }

@Composable
fun CacheManagementDialog(
    cacheSpaceBytes: Long,
    currentMaxBytes: Long,
    onMaxSizeSelected: (Long) -> Unit,
    onClearCache: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.extraLarge) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = stringResource(R.string.cache_management),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                Text(
                    text = stringResource(
                        R.string.audio_cache_usage,
                        formatCacheSize(cacheSpaceBytes),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
                Text(
                    text = stringResource(R.string.cache_limit),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp),
                )
                Column(modifier = Modifier.selectableGroup()) {
                    AudioCacheSizeOptions.forEach { sizeBytes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = currentMaxBytes == sizeBytes,
                                    onClick = { onMaxSizeSelected(sizeBytes) },
                                    role = Role.RadioButton,
                                )
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = currentMaxBytes == sizeBytes,
                                onClick = null,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(formatCacheSize(sizeBytes))
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onClearCache) {
                        Text(stringResource(R.string.clear_cache))
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
        }
    }
}

fun formatCacheSize(bytes: Long): String {
    val megabytes = bytes / (1024.0 * 1024.0)
    return if (megabytes >= 1024) {
        val gigabytes = megabytes / 1024.0
        if (gigabytes % 1.0 == 0.0) "${gigabytes.toInt()} GB" else "%.1f GB".format(gigabytes)
    } else {
        if (megabytes < 10) "%.1f MB".format(megabytes) else "${megabytes.toLong()} MB"
    }
}
