package com.rcmiku.music.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.rcmiku.music.ui.theme.AppThemeSeed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSeedDialog(
    currentSeed: AppThemeSeed,
    dynamicColorAvailable: Boolean,
    dynamicColorEnabled: Boolean,
    theme: Int,
    onDismiss: () -> Unit,
    onThemeChange: (Int) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onSeedSelected: (AppThemeSeed) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = MaterialTheme.shapes.extraLarge) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "\u4e3b\u9898\u79cd\u5b50\u8272",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "\u5207\u6362\u5e94\u7528\u6574\u4f53\u914d\u8272",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "\u6839\u636e\u58c1\u7eb8\u52a8\u6001\u53d6\u8272",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (dynamicColorAvailable) {
                                    "\u5728 Android 12 \u53ca\u4ee5\u4e0a\u8ddf\u968f\u58c1\u7eb8\u989c\u8272"
                                } else {
                                    "\u5f53\u524d\u8bbe\u5907\u4e0d\u652f\u6301\u58c1\u7eb8\u52a8\u6001\u53d6\u8272"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = dynamicColorEnabled,
                            onCheckedChange = onDynamicColorChange,
                            enabled = dynamicColorAvailable
                        )
                    }
                    val context = LocalContext.current
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        var _theme by remember { mutableStateOf(theme) }

                        val themeOptions = listOf(
                            0 to "浅色主题",
                            1 to "深色主题",
                            2 to "依照系统"
                        )

                        var currentLabel = themeOptions.find { it.first == _theme }?.second ?: ""

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            OutlinedTextField(
                                value = currentLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("主题") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                themeOptions.forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            _theme = value
                                            onThemeChange(_theme)
                                            Toast.makeText(
                                                context,
                                                "已切换为${themeOptions.find { it.first == value }?.second}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (!dynamicColorEnabled) {
                    Spacer(Modifier.height(2.dp))
                }

                if (!dynamicColorEnabled) {
                    AppThemeSeed.entries.forEach { seed ->
                        val selected = seed == currentSeed
                        val containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSeedSelected(seed)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(seed.seedColor)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp)
                                ) {
                                    Text(
                                        text = seed.label,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = seed.seedColor.toHexString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = selected,
                                    onClick = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Color.toHexString(): String =
    String.format("#%06X", toArgb() and 0xFFFFFF)
