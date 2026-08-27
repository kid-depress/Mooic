package com.rcmiku.music.ui.screen

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.decode.ImageSource
import com.rcmiku.music.R
import com.rcmiku.music.constants.SettingItemCorner
import com.rcmiku.music.constants.SettingItemHeight
import com.rcmiku.music.constants.SettingItemSubCorner
import com.rcmiku.music.constants.apiBaseUrlKey
import com.rcmiku.music.constants.audioQualityKey
import com.rcmiku.music.constants.autoSkipNextOnErrorKey
import com.rcmiku.music.constants.dynamicThemeColorKey
import com.rcmiku.music.constants.ncmCookieKey
import com.rcmiku.music.constants.theme
import com.rcmiku.music.constants.themeSeedColorKey
import com.rcmiku.music.constants.unblockBaseUrlKey
import com.rcmiku.music.constants.use40DpIconKey
import com.rcmiku.music.constants.watchMode
import com.rcmiku.music.ui.components.Dialog
import com.rcmiku.music.ui.components.SongQualityDialog
import com.rcmiku.music.ui.components.ThemeSeedDialog
import com.rcmiku.music.ui.components.UrlEditDialog
import com.rcmiku.music.ui.icons.Dns
import com.rcmiku.music.ui.icons.Github
import com.rcmiku.music.ui.icons.GraphicEq
import com.rcmiku.music.ui.icons.Login
import com.rcmiku.music.ui.icons.Logout
import com.rcmiku.music.ui.icons.PlayPause
import com.rcmiku.music.ui.icons.SkipNext
import com.rcmiku.music.ui.icons.UserRound
import com.rcmiku.music.ui.icons.VipUser
import com.rcmiku.music.ui.icons.Watch
import com.rcmiku.music.ui.navigation.Screen
import com.rcmiku.music.ui.theme.AppThemeSeed
import com.rcmiku.music.utils.getItemShape
import com.rcmiku.music.utils.rememberEnumPreference
import com.rcmiku.music.utils.rememberPreference
import com.rcmiku.ncmapi.api.player.SongLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    var use40DpIcon by rememberPreference(use40DpIconKey, false)
    var audioQuality by rememberEnumPreference(audioQualityKey, defaultValue = SongLevel.STANDARD)
    var useDynamicThemeColor by rememberPreference(dynamicThemeColorKey, false)
    var themeSeed by rememberEnumPreference(themeSeedColorKey, defaultValue = AppThemeSeed.PURPLE)
    var autoSkipNextOnError by rememberPreference(autoSkipNextOnErrorKey, false)
    var theme by rememberPreference(theme, 2)
    var watchMode by rememberPreference(watchMode, isWatch(context))
    var ncmCookie by rememberPreference(ncmCookieKey, "")
    var apiBaseUrl by rememberPreference(apiBaseUrlKey, "https://ncm-api.prod.gbclstudio.cn")
    var unblockBaseUrl by rememberPreference(unblockBaseUrlKey, "https://unlock.depresskid.top")

    var showQualityDialog by remember { mutableStateOf(false) }
    var showThemeSeedDialog by remember { mutableStateOf(false) }
    var showApiUrlDialog by remember { mutableStateOf(false) }
    var showUnblockUrlDialog by remember { mutableStateOf(false) }
    var logout by rememberSaveable { mutableStateOf(false) }

    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val appearanceTitle = "\u5916\u89c2\u8bbe\u7f6e"
    val appearanceSubtitle = when {
        useDynamicThemeColor && dynamicColorAvailable -> "\u58c1\u7eb8\u52a8\u6001\u53d6\u8272"
        useDynamicThemeColor -> "\u58c1\u7eb8\u52a8\u6001\u53d6\u8272 (\u5f53\u524d\u4e0d\u53ef\u7528)"
        else -> "\u4e3b\u9898\u8272: ${themeSeed.label}"
    }

    val baseSettingItems = listOf(
        SettingItemData(
            title = appearanceTitle,
            subtitle = appearanceSubtitle,
            imageVector = GraphicEq,
            onClick = { showThemeSeedDialog = true }
        ),
        SettingItemData(
            title = stringResource(if (ncmCookie.isNotEmpty()) R.string.logout else R.string.login),
            imageVector = if (ncmCookie.isNotEmpty()) Logout else Login,
            onClick = {
                if (ncmCookie.isNotEmpty()) {
                    logout = true
                } else {
                    navController.navigate(Screen.Login.route)
                }
            }
        ),
        SettingItemData(
            title = stringResource(R.string.command_button),
            subtitle = stringResource(R.string.command_button_subtitle),
            imageVector = PlayPause,
            trailingContent = {
                Switch(
                    checked = use40DpIcon,
                    onCheckedChange = { use40DpIcon = it }
                )
                Spacer(Modifier.width(12.dp))
            },
            onClick = { use40DpIcon = !use40DpIcon }
        ),
        SettingItemData(
            title = stringResource(R.string.watch_mode),
            subtitle = stringResource(R.string.watch_mode_describe),
            imageVector = Watch,
            trailingContent = {
                Switch(
                    checked = watchMode,
                    onCheckedChange = { watchMode = it }
                )
                Spacer(Modifier.width(12.dp))
            }
        ),
        SettingItemData(
            title = stringResource(R.string.audio_quality),
            subtitle = when (audioQuality) {
                SongLevel.STANDARD -> stringResource(R.string.standard)
                SongLevel.HIGHER -> stringResource(R.string.higer)
                SongLevel.EXHIGH -> stringResource(R.string.exhigh)
                SongLevel.LOSSLESS -> stringResource(R.string.lossless)
                SongLevel.HIRES -> stringResource(R.string.hi_res)
            },
            imageVector = GraphicEq,
            onClick = { showQualityDialog = true }
        ),
        SettingItemData(
            title = stringResource(R.string.auto_skip),
            imageVector = SkipNext,
            trailingContent = {
                Switch(
                    checked = autoSkipNextOnError,
                    onCheckedChange = { autoSkipNextOnError = it }
                )
                Spacer(Modifier.width(12.dp))
            },
            onClick = { autoSkipNextOnError = !autoSkipNextOnError }
        ),
        SettingItemData(
            title = stringResource(R.string.api_server),
            subtitle = apiBaseUrl,
            imageVector = Dns,
            onClick = { showApiUrlDialog = true }
        ),
        SettingItemData(
            title = stringResource(R.string.unblock_server),
            subtitle = unblockBaseUrl,
            imageVector = Dns,
            onClick = { showUnblockUrlDialog = true }
        )
    )

    val settingsItems = listOf(
        SettingItemData(
            title = "kid-depress",
            imageVector = UserRound,
            onClick = { uriHandler.openUri("https://github.com/kid-depress") }
        ),
        SettingItemData(
            title = stringResource(R.string.contact_author),
            imageVector = VipUser,
            onClick = { uriHandler.openUri("https://qm.qq.com/q/CNTuNPHdrc") }
        ),
        SettingItemData(
            title = stringResource(R.string.source_code),
            imageVector = Github,
            onClick = { uriHandler.openUri("https://github.com/rcmiku/JetMelo") }
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = padding
        ) {
            item {
                Text(
                    stringResource(R.string.basic_settings),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            itemsIndexed(baseSettingItems) { index, item ->
                val shape = getItemShape(
                    prevItem = baseSettingItems.getOrNull(index - 1),
                    nextItem = baseSettingItems.getOrNull(index + 1),
                    corner = SettingItemCorner,
                    subCorner = SettingItemSubCorner,
                )

                SettingCard(
                    title = item.title,
                    description = item.subtitle,
                    shape = shape,
                    imageVector = item.imageVector,
                    onClick = item.onClick,
                    trailingContent = item.trailingContent
                )
            }

            item {
                Text(
                    stringResource(R.string.about),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            itemsIndexed(settingsItems) { index, item ->
                val shape = getItemShape(
                    prevItem = settingsItems.getOrNull(index - 1),
                    nextItem = settingsItems.getOrNull(index + 1),
                    corner = SettingItemCorner,
                    subCorner = SettingItemSubCorner,
                )
                SettingCard(
                    title = item.title,
                    description = item.subtitle,
                    imageVector = item.imageVector,
                    shape = shape,
                    onClick = item.onClick,
                    trailingContent = item.trailingContent,
                )
            }
        }
    }

    if (showQualityDialog) {
        SongQualityDialog(
            currentLevel = audioQuality,
            onDismiss = { showQualityDialog = false },
            onQualitySelected = { audioQuality = it }
        )
    }

    if (showThemeSeedDialog) {
        ThemeSeedDialog(
            currentSeed = themeSeed,
            dynamicColorAvailable = dynamicColorAvailable,
            dynamicColorEnabled = useDynamicThemeColor,
            theme = theme,
            onDismiss = { showThemeSeedDialog = false },
            onDynamicColorChange = { useDynamicThemeColor = it },
            onThemeChange = { theme = it },
            onSeedSelected = { themeSeed = it }
        )
    }

    if (logout) {
        Dialog(
            onConfirmation = {
                ncmCookie = ""
                logout = false
            },
            onDismissRequest = {
                logout = false
            },
            dialogTitle = stringResource(R.string.logout),
        )
    }

    if (showApiUrlDialog) {
        UrlEditDialog(
            title = stringResource(R.string.api_server),
            currentUrl = apiBaseUrl,
            defaultUrl = "https://ncm-api.prod.gbclstudio.cn",
            onDismiss = { showApiUrlDialog = false },
            onConfirm = { apiBaseUrl = it }
        )
    }

    if (showUnblockUrlDialog) {
        UrlEditDialog(
            title = stringResource(R.string.unblock_server),
            currentUrl = unblockBaseUrl,
            defaultUrl = "https://unlock.depresskid.top",
            onDismiss = { showUnblockUrlDialog = false },
            onConfirm = { unblockBaseUrl = it }
        )
    }
}

@Composable
fun SettingCard(
    title: String,
    description: String? = null,
    imageVector: ImageVector,
    shape: Shape,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        SettingItem(
            imageVector = imageVector,
            title = title,
            description = description,
            onClick = onClick,
            trailingContent = trailingContent
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingItem(
    imageVector: ImageVector,
    title: String,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SettingItemHeight)
            .combinedClickable(onClick = { onClick?.invoke() }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            imageVector = imageVector,
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.surfaceTint),
            modifier = Modifier.padding(start = 12.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        trailingContent?.invoke()
    }
}

data class SettingItemData(
    val title: String,
    val subtitle: String? = null,
    val imageVector: ImageVector,
    val onClick: (() -> Unit)? = null,
    val trailingContent: @Composable (() -> Unit)? = null
)

fun isWatch(context: Context): Boolean {
    val packageManager = context.packageManager
    if (packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)) return true

    val displayMetrics = context.resources.displayMetrics
    val dpi = displayMetrics.densityDpi
    val smallestWidthDp = context.resources.configuration.smallestScreenWidthDp
    return dpi >= 240 && smallestWidthDp in 1..360
}
