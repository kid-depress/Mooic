package com.rcmiku.music.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.activity.compose.BackHandler
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.MediaMetadata
import androidx.navigation.NavHostController
import com.rcmiku.music.constants.DURATION_EXIT
import com.rcmiku.music.constants.DURATION_EXIT_SHORT
import com.rcmiku.music.ui.components.Lyric
import com.rcmiku.music.ui.components.MiniPlayer
import com.rcmiku.music.ui.components.Player
import com.rcmiku.music.ui.components.PlayerQueue



@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerTransformTablet(
    onClick: () -> Unit = {},
    onBackPressed: () -> Unit = {},
    mediaMetadata: MediaMetadata,
    position: Long,
    duration: Long,
    onPositionUpdate: (Long) -> Unit,
    navController: NavHostController,
) {

    var show by remember {
        mutableIntStateOf(MINI_PLAYER)
    }

    BackHandler(enabled = show != MINI_PLAYER) {
        show = MINI_PLAYER
    }

    BackHandler(enabled = show != MINI_PLAYER) {
        show = MINI_PLAYER
    }

    SharedTransitionLayout(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        AnimatedContent(targetState = show, transitionSpec = {
            fadeIn(
                tweenEnter(delayMillis = DURATION_EXIT_SHORT)
            ) togetherWith fadeOut(
                tweenExit(durationMillis = DURATION_EXIT_SHORT)
            )
        }) {
            when (it) {
                FULL_PLAYER -> {
                    val _this = this
                    Row(
                        modifier = Modifier.fillMaxSize() 
                    ) {
                        Player(
                            navController = navController,
                            mediaMetadata = mediaMetadata,
                            duration = duration,
                            position = position,
                            imageModifier = Modifier.sharedElement(
                                state = rememberSharedContentState(
                                    key = mediaMetadata.artist.toString()
                                ),
                                animatedVisibilityScope = _this,
                                placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize,
                                boundsTransform = AlbumArtBoundsTransform,
                            ),
                            modifier = Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(
                                    key = "container"
                                ),
                                animatedVisibilityScope = _this,
                                placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize,
                                boundsTransform = AlbumArtBoundsTransform,
                                enter = fadeIn(
                                    tweenEnter(delayMillis = DURATION_EXIT_SHORT)
                                ),
                                exit = fadeOut(
                                    tweenExit(durationMillis = DURATION_EXIT_SHORT)
                                )
                            ).weight(4f),
                            onBackPressed = {
                                show = MINI_PLAYER
                                onBackPressed()
                            },
                            onContainerClick = {
                                show = PLAY_QUEUE
                            },
                            onPositionUpdate = { position ->
                                onPositionUpdate(position)
                            },
                            isTablet = true
                        )
                        Lyric(
                            position = position,
                            mediaMetadata = mediaMetadata,
                            imageModifier = Modifier.sharedElement(
                                state = rememberSharedContentState(
                                    key = mediaMetadata.artist.toString()
                                ),
                                animatedVisibilityScope = _this,
                                placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize,
                                boundsTransform = AlbumArtBoundsTransform,
                            ),
                            modifier = Modifier.sharedBounds(
                                sharedContentState = rememberSharedContentState(
                                    key = "container_baka"
                                ),
                                animatedVisibilityScope = _this,
                                placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize,
                                boundsTransform = AlbumArtBoundsTransform,
                                enter = fadeIn(
                                    tweenEnter(delayMillis = DURATION_EXIT_SHORT)
                                ),
                                exit = fadeOut(
                                    tweenExit(durationMillis = DURATION_EXIT_SHORT)
                                )
                            ).weight(6f), 
                            onBackPressed = { show = FULL_PLAYER },
                            isTablet = true
                        )
                    }
                }

                PLAY_QUEUE -> {
                    PlayerQueue(
                        mediaMetadata = mediaMetadata,
                        imageModifier = Modifier.sharedElement(
                            state = rememberSharedContentState(
                                key = mediaMetadata.artist.toString()
                            ),
                            animatedVisibilityScope = this,
                            placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize,
                            boundsTransform = AlbumArtBoundsTransform,
                        ),
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = "container"
                            ),
                            animatedVisibilityScope = this,
                            placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize,
                            boundsTransform = AlbumArtBoundsTransform,
                            enter = fadeIn(
                                tweenEnter(delayMillis = DURATION_EXIT_SHORT)
                            ),
                            exit = fadeOut(
                                tweenExit(durationMillis = DURATION_EXIT_SHORT)
                            )
                        ),
                        onBackPressed = { show = FULL_PLAYER },
                    )
                }

                else -> {
                    MiniPlayer(
                        mediaMetadata = mediaMetadata,
                        duration = duration,
                        position = position,
                        onClick = {
                            show = FULL_PLAYER
                            onClick()
                        },
                        onQueueClick = {
                            show = PLAY_QUEUE
                            onClick()
                        },
                        imageModifier = Modifier.sharedElement(
                            state = rememberSharedContentState(
                                key = mediaMetadata.artist.toString()
                            ),
                            animatedVisibilityScope = this,
                            placeHolderSize = { contentSize: IntSize, animatedSize: IntSize ->
                                IntSize(contentSize.width, animatedSize.height)
                            },
                            boundsTransform = AlbumArtBoundsTransform,
                        ),
                        modifier = Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = "container"
                            ),
                            animatedVisibilityScope = this,
                            placeHolderSize = SharedTransitionScope.PlaceHolderSize.contentSize,
                            boundsTransform = AlbumArtBoundsTransform,
                            enter = fadeIn(
                                tweenEnter(delayMillis = DURATION_EXIT)
                            ),
                            exit = fadeOut(
                                tweenExit(durationMillis = DURATION_EXIT_SHORT)
                            )
                        ),
                    )
                }
            }
        }
    }
}
