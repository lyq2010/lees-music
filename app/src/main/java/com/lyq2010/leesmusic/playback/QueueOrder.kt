package com.lyq2010.leesmusic.playback

import androidx.media3.common.C
import androidx.media3.common.Player

/** One pass through the real timeline, without repeats or duplicate media-id assumptions. */
fun queueOrder(player: Player): List<Int> {
    val timeline = player.currentTimeline
    val result = mutableListOf<Int>()
    var index = timeline.getFirstWindowIndex(player.shuffleModeEnabled)
    while (index != C.INDEX_UNSET && index !in result && result.size < timeline.windowCount) {
        result += index
        index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, player.shuffleModeEnabled)
    }
    return result
}

fun nextRepeatMode(mode: Int): Int = when (mode) {
    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
    else -> Player.REPEAT_MODE_OFF
}

/** Explicit queue edits freeze the current shuffle order, preserving the current item and position. */
fun freezeShuffleOrder(player: Player) {
    if (!player.shuffleModeEnabled) return
    val order = queueOrder(player)
    player.shuffleModeEnabled = false
    reorderQueue(player, order)
}

fun moveUpcomingEntry(player: Player, from: Int, to: Int) {
    if (player.shuffleModeEnabled) return
    val order = queueOrder(player)
    val position = order.indexOf(player.currentMediaItemIndex)
    val sequence = if (player.repeatMode == Player.REPEAT_MODE_ALL && position >= 0)
        (order.drop(position) + order.take(position)).toMutableList() else order.toMutableList()
    val destination = sequence.indexOf(to)
    val source = sequence.indexOf(from)
    if (source < 0 || destination < 0 || from == player.currentMediaItemIndex || to == player.currentMediaItemIndex) return
    sequence.add(destination, sequence.removeAt(source))
    reorderQueue(player, sequence)
}

private fun reorderQueue(player: Player, order: List<Int>) {
    val original = (0 until player.mediaItemCount).toMutableList()
    order.forEachIndexed { target, identity ->
        val from = original.indexOf(identity)
        if (from != target) {
            player.moveMediaItem(from, target)
            original.add(target, original.removeAt(from))
        }
    }
}
