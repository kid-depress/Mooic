package com.rcmiku.music.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rcmiku.music.constants.MiniPlayerHeight
import com.rcmiku.music.ui.screen.AlbumScreen
import com.rcmiku.music.ui.screen.AlbumSublistScreen
import com.rcmiku.music.ui.screen.ArtistScreen
import com.rcmiku.music.ui.screen.CloudSongScreen
import com.rcmiku.music.ui.screen.ExploreScreen
import com.rcmiku.music.ui.screen.FMScreen
import com.rcmiku.music.ui.screen.HomeScreen
import com.rcmiku.music.ui.screen.LibraryScreen
import com.rcmiku.music.ui.screen.ListScreen
import com.rcmiku.music.ui.screen.LoginScreen
import com.rcmiku.music.ui.screen.PlaylistScreen
import com.rcmiku.music.ui.screen.ProgramRadioScreen
import com.rcmiku.music.ui.screen.RecordScreen
import com.rcmiku.music.ui.screen.SearchScreen
import com.rcmiku.music.ui.screen.SettingsScreen
import com.rcmiku.music.ui.screen.UserPlaylistScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    bottomPadding: Dp,
    showMiniPlayer: Boolean
) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            Modifier
                .windowInsetsPadding(WindowInsets(bottom = bottomPadding + if (showMiniPlayer) MiniPlayerHeight else 0.dp)),
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable
                )
            }
            composable(Screen.Explore.route) {
                ExploreScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    navController = navController,
                )
            }
            composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
            composable(Screen.Login.route) { LoginScreen(navController = navController) }
            composable(Screen.Search.route) { SearchScreen(navController = navController) }
            composable(Screen.Fm.route) { FMScreen(navController = navController) }
            composable<PlaylistNav> {
                PlaylistScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable
                )
            }
            composable<AlbumNav> {
                AlbumScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable
                )
            }
            composable(Screen.TopList.route) {
                ListScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable
                )
            }
            composable(Screen.AlbumSublist.route) {
                AlbumSublistScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable
                )
            }
            composable<UserPlayListNav> {
                UserPlaylistScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable
                )
            }
            composable<RecordNav> {
                RecordScreen(navController = navController)
            }
            composable<CloudSongNav> {
                CloudSongScreen(navController = navController)
            }
            composable<ArtistNav> {
                ArtistScreen(navController = navController)
            }
            composable<RadioNav> {
                ProgramRadioScreen(
                    navController = navController,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@composable
                )
            }
        }
    }
}
