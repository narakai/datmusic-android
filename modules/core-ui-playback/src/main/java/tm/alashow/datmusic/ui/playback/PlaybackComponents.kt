/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.datmusic.ui.playback

import android.support.v4.media.MediaMetadataCompat
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import com.google.accompanist.pager.*
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.collectLatest
import tm.alashow.common.compose.LocalPlaybackConnection
import tm.alashow.datmusic.domain.entities.Audio
import tm.alashow.datmusic.playback.PLAYBACK_PROGRESS_INTERVAL
import tm.alashow.datmusic.playback.PlaybackConnection
import tm.alashow.datmusic.playback.models.toAudio

@OptIn(ExperimentalPagerApi::class)
@Composable
internal fun PlaybackPager(
    nowPlaying: MediaMetadataCompat,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState(),
    playbackConnection: PlaybackConnection = LocalPlaybackConnection.current,
    content: @Composable (Audio, Int, Modifier) -> Unit,
) {
    val playbackQueue by playbackConnection.playbackQueue.collectAsState()
    val playbackCurrentIndex = playbackQueue.currentIndex
    var lastRequestedPage by remember(playbackQueue, nowPlaying) { mutableStateOf<Int?>(playbackCurrentIndex) }

    if (!playbackQueue.isValid) {
        content(nowPlaying.toAudio(), playbackCurrentIndex, Modifier)
        return
    }
    LaunchedEffect(playbackCurrentIndex, pagerState) {
        if (playbackCurrentIndex != pagerState.currentPage) {
            pagerState.scrollToPage(playbackCurrentIndex)
        }
        snapshotFlow { pagerState.currentPage }.collectLatest { page ->
            if (lastRequestedPage != page) {
                lastRequestedPage = page
                playbackConnection.transportControls?.skipToQueueItem(page.toLong())
            }
        }
    }
    HorizontalPager(
        count = playbackQueue.ids.size,
        modifier = modifier,
        state = pagerState
    ) { page ->
        val currentAudio = playbackQueue.audios.getOrNull(page) ?: Audio()

        val pagerMod = Modifier.graphicsLayer {
            val pageOffset = calculateCurrentOffsetForPage(page).absoluteValue
            lerp(
                start = 0.85f,
                stop = 1f,
                fraction = 1f - pageOffset.coerceIn(0f, 1f)
            ).also { scale ->
                scaleX = scale
                scaleY = scale
            }

            alpha = lerp(
                start = 0.5f,
                stop = 1f,
                fraction = 1f - pageOffset.coerceIn(0f, 1f)
            )
        }
        content(currentAudio, page, pagerMod)
    }
}

@Composable
internal fun animatePlaybackProgress(
    targetValue: Float,
) = animateFloatAsState(
    targetValue = targetValue,
    animationSpec = tween(
        durationMillis = PLAYBACK_PROGRESS_INTERVAL.toInt(),
        easing = FastOutSlowInEasing
    ),
)
