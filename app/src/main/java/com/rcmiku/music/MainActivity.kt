package com.rcmiku.music

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.media3.common.util.UnstableApi
import com.rcmiku.music.extensions.init
import com.rcmiku.music.playback.PlayerController
import com.rcmiku.music.playback.PlayerState
import com.rcmiku.music.playback.state
import com.rcmiku.music.ui.screen.MainScreen
import com.rcmiku.music.ui.theme.JetMeloTheme
import com.rcmiku.music.utils.rememberPreference
import dagger.hilt.android.AndroidEntryPoint

@UnstableApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var playerController: PlayerController
    private var playerState by mutableStateOf<PlayerState?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        playerController = PlayerController
        setContent {
            playerController.controller?.run {
                if (playerState?.player !== this) {
                    playerState?.dispose()
                    init(applicationContext)
                    playerState = state(applicationContext)
                }
            }
            val _theme by rememberPreference(com.rcmiku.music.constants.theme, defaultValue = 2)
            var is_dark = if (_theme == 2) isSystemInDarkTheme() else _theme == 1
            JetMeloTheme(darkTheme = is_dark) {
                CompositionLocalProvider(
                    LocalPlayerController provides playerController,
                    LocalPlayerState provides playerState
                ) {
                    MainScreen(
                        context = this
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        playerState?.dispose()
        playerState = null
        super.onDestroy()
    }

}

val LocalPlayerController = staticCompositionLocalOf<PlayerController> {
    error("No PlayerController provided")
}

val LocalPlayerState = staticCompositionLocalOf<PlayerState?> {
    error("No PlayerState provided")
}
