package com.lyq2010.leesmusic.ui.player

import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** A completed horizontal drag changes one track; taps remain with the bar's controls. */
internal fun Modifier.trackSwipe(canPrevious: Boolean, canNext: Boolean,
    onPrevious: () -> Unit, onNext: () -> Unit): Modifier = composed {
    var offset by remember { mutableFloatStateOf(0f) }
    val previous by rememberUpdatedState(onPrevious)
    val next by rememberUpdatedState(onNext)
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    this.clipToBounds().graphicsLayer { translationX = offset }
        .draggable(rememberDraggableState { delta ->
            val proposed = offset + delta
            offset = proposed.coerceIn(if (canNext) -threshold * 1.5f else -threshold / 4,
                if (canPrevious) threshold * 1.5f else threshold / 4)
        }, Orientation.Horizontal, onDragStopped = {
            val direction = when {
                offset <= -threshold && canNext -> -1
                offset >= threshold && canPrevious -> 1
                else -> 0
            }
            animate(offset, 0f) { value, _ -> offset = value }
            if (direction < 0) next() else if (direction > 0) previous()
        })
}
