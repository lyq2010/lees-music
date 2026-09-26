package com.lyq2010.leesmusic.ui.player

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity

/** Vertical dismiss leaves horizontal seeking and volume gestures to their controls. */
@Composable
internal fun PlaybackDismiss(onDismiss: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    var offset by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableIntStateOf(1) }
    val threshold = with(LocalDensity.current) { 96.dp.toPx() }
    val fling = with(LocalDensity.current) { 1200.dp.toPx() }
    val latestDismiss by rememberUpdatedState(onDismiss)
    var closing by remember { mutableStateOf(false) }
    suspend fun settle(velocity: Float) {
        if (closing) return
        val dismiss = offset >= threshold || (offset > threshold / 4 && velocity >= fling)
        closing = dismiss
        animate(offset, if (dismiss) height.toFloat() else 0f, animationSpec = tween(220)) { value, _ -> offset = value }
        if (dismiss) latestDismiss()
    }
    val nestedScroll = remember(threshold, fling) { object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (offset <= 0f || closing || source != NestedScrollSource.UserInput) return Offset.Zero
            val before = offset
            offset = (offset + available.y).coerceIn(0f, height.toFloat())
            return Offset(0f, offset - before)
        }
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (available.y <= 0f || closing || source != NestedScrollSource.UserInput) return Offset.Zero
            val before = offset
            offset = (offset + available.y).coerceAtMost(height.toFloat())
            return Offset(0f, offset - before)
        }
        override suspend fun onPreFling(available: Velocity): Velocity {
            if (offset <= 0f) return Velocity.Zero
            settle(available.y)
            return Velocity(0f, available.y)
        }
    } }
    Box(Modifier.fillMaxSize().testTag("playback-dismiss")
        .onSizeChanged { height = it.height }
        .graphicsLayer { translationY = offset }
        .nestedScroll(nestedScroll)
        .draggable(rememberDraggableState { offset = (offset + it).coerceIn(0f, height.toFloat()) },
            Orientation.Vertical, enabled = !closing, onDragStopped = { settle(it) }), content = content)
}
