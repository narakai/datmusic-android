/*
 * Copyright (C) 2021, Alashov Berkeli
 * All rights reserved.
 */
package tm.alashow.common.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * It is what it is. Makes given [content] zoomable, optionally snaps back [snapBack] is true.
 */
@Composable
fun Zoomable(
    maxScale: Float = 4f,
    minScale: Float = 0.7f,
    snapBack: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val panX = remember { Animatable(0f) }
    val panY = remember { Animatable(0f) }

    fun limitScale(k: Float): Float =
        if ((scale.value <= maxScale && k > 1f) || (scale.value >= minScale && k < 1f)) scale.value * k else scale.value

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures(false) { centroid, pan, zoomChange, rotationChange ->
                    coroutineScope.launch {
                        scale.snapTo(limitScale(zoomChange))
                        rotation.snapTo(rotation.value + rotationChange)
                        panX.snapTo(panX.value + pan.x)
                        panY.snapTo(panY.value + pan.y)
                        if (snapBack) {
                            delay(24)
                            launch { scale.animateTo(1f) }
                            launch { rotation.animateTo(0f) }
                            launch { panX.animateTo(0f) }
                            launch { panY.animateTo(0f) }
                        }
                    }
                }
            }
            .graphicsLayer(
                scaleX = scale.value,
                scaleY = scale.value,
                translationX = panX.value,
                translationY = panY.value,
                rotationZ = rotation.value
            )
    ) {
        content()
    }
}
