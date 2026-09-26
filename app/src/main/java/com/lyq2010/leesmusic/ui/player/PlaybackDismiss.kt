package com.lyq2010.leesmusic.ui.player

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Vertical dismiss leaves horizontal seeking and volume gestures to their controls. */
@Composable
internal fun PlaybackDismiss(onDismiss: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    var offset by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableIntStateOf(1) }
    val threshold = with(LocalDensity.current) { 96.dp.toPx() }
    val fling = with(LocalDensity.current) { 1200.dp.toPx() }
    val latestDismiss by rememberUpdatedState(onDismiss)
    Box(Modifier.fillMaxSize().testTag("playback-dismiss")
        .onSizeChanged { height = it.height }
        .graphicsLayer { translationY = offset }
        .draggable(rememberDraggableState { offset = (offset + it).coerceIn(0f, height.toFloat()) },
            Orientation.Vertical, onDragStopped = { velocity ->
                val dismiss = offset >= threshold || (offset > threshold / 4 && velocity >= fling)
                animate(offset, if (dismiss) height.toFloat() else 0f) { value, _ -> offset = value }
                if (dismiss) latestDismiss()
            }), content = content)
}
