package com.lyq2010.leesmusic.playback

import android.content.Context
import android.util.AtomicFile
import androidx.media3.common.Player
import com.lyq2010.leesmusic.data.api.*
import com.lyq2010.leesmusic.data.library.*
import com.lyq2010.leesmusic.data.settings.*
import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
internal data class PlaybackSnapshot(val namespace: String, val songs: List<LibrarySong>, val index: Int,
    val position: Long, val repeat: Int, val shuffle: Boolean)

/** Saves metadata and position, never credentials or expiring stream URLs. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class PlaybackPersistence(private val context: Context, private val player: Player, private val onReady: () -> Unit) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val writes = Channel<PlaybackSnapshot>(Channel.CONFLATED)
    private val json = Json { ignoreUnknownKeys = true }
    private var revision = 0
    private val ticker: Job
    private val restore: Job
    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_TIMELINE_CHANGED)) revision++
            if (listOf(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_REPEAT_MODE_CHANGED, Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED).any(events::contains)) save()
        }
    }
    private fun file(namespace: String) = AtomicFile(File(context.filesDir, "libraries/$namespace/playback.json"))
    init {
        player.addListener(listener)
        scope.launch(Dispatchers.IO) {
            for (snapshot in writes) runCatching {
                val target = file(snapshot.namespace)
                target.baseFile.parentFile?.mkdirs()
                val stream = target.startWrite()
                try { stream.write(json.encodeToString(snapshot).toByteArray()); target.finishWrite(stream) }
                catch (error: Exception) { target.failWrite(stream); throw error }
            }
        }.invokeOnCompletion { scope.cancel() }
        ticker = scope.launch { while (isActive) { delay(2000); if (player.isPlaying) save() } }
        restore = scope.launch {
            val expected = revision
            val restored = withContext(Dispatchers.IO) {
                runCatching {
                    val settings = ServerSettingsStore(context).load()?.takeIf { it.kind == ServerKind.Navidrome } ?: return@runCatching null
                    val namespace = serverCacheIdentity(settings.url, settings.username, settings.kind.name)
                    val snapshot = file(namespace).openRead().bufferedReader().use { json.decodeFromString<PlaybackSnapshot>(it.readText()) }
                    if (snapshot.namespace != namespace || snapshot.songs.isEmpty() || snapshot.index !in snapshot.songs.indices) return@runCatching null
                    val server = SubsonicServer(settings.url, settings.username, settings.password)
                    val client = SubsonicClient()
                    val local = runCatching { LibraryDownloads(context, namespace).list().filter { it.playable }
                        .associate { it.song.id to it.uri!! } }.getOrDefault(emptyMap())
                    val songs = withDownloadedSongs(snapshot.songs.map { it.copy(coverUrl = it.coverArtId?.let { id -> client.coverArtUrl(server, id) }) }, local)
                    Triple(snapshot, server, songs)
                }.getOrNull()
            }
            if (restored != null && revision == expected && player.mediaItemCount == 0) {
                val (snapshot, server, songs) = restored
                player.playWhenReady = false
                player.setMediaItems(songs.map { playbackMediaItem(server, it, PlaybackPreferences(context).quality.bitRate) },
                    snapshot.index, snapshot.position.coerceAtLeast(0))
                player.repeatMode = snapshot.repeat.coerceIn(Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL)
                player.shuffleModeEnabled = snapshot.shuffle
            }
            onReady()
        }
    }
    fun save() {
        if (player.mediaItemCount == 0) return
        val namespace = player.currentMediaItem?.mediaMetadata?.extras?.getString("namespace") ?: return
        val songs = (0 until player.mediaItemCount).map { mediaItemSong(player.getMediaItemAt(it)).copy(coverUrl = null, localUri = null) }
        writes.trySend(PlaybackSnapshot(namespace, songs, player.currentMediaItemIndex, player.currentPosition.coerceAtLeast(0), player.repeatMode, player.shuffleModeEnabled))
    }
    fun close() {
        save()
        player.removeListener(listener)
        // Drain the last queued write before shutting down; no blocking disk I/O on the player thread.
        writes.close()
        ticker.cancel()
        restore.cancel()
    }
}
