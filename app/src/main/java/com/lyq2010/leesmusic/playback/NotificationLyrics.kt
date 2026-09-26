package com.lyq2010.leesmusic.playback

import android.content.Context
import com.lyq2010.leesmusic.data.api.*
import com.lyq2010.leesmusic.data.library.LyricsRepository
import com.lyq2010.leesmusic.data.settings.*
import kotlinx.coroutines.*

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class NotificationLyrics(context: Context, private val player: ManagedPlayer, private val preferences: PlaybackPreferences) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store = ServerSettingsStore(context)
    private val repository = LyricsRepository(SubsonicClient())
    private var key: String? = null
    private var lyrics: StructuredLyrics? = null
    private var load: Job? = null
    init {
        scope.launch {
            while (isActive) {
                val item = player.currentMediaItem
                val next = if (preferences.notificationLyrics) item?.mediaId else null
                if (key != next) {
                    key = next; lyrics = null; load?.cancel()
                    if (next != null && item != null) load = launch {
                        try {
                            val saved = store.load()?.takeIf { it.kind == ServerKind.Navidrome } ?: return@launch
                            val result = repository.load(SubsonicServer(saved.url, saved.username, saved.password), next,
                                item.mediaMetadata.artist?.toString().orEmpty(), item.mediaMetadata.title?.toString().orEmpty())
                            if (key == next) lyrics = result?.takeIf { it.synced }
                        } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) { /* Fall back to artist. */ }
                    }
                }
                val text = lyrics?.let { value -> value.line.getOrNull(value.activeLine(player.currentPosition))?.let { active ->
                    value.line.filter { it.start == active.start }.joinToString(" · ") { it.value }.takeIf { it.isNotBlank() }
                } }
                player.setNotificationLyric(if (preferences.notificationLyrics) text else null)
                delay(500)
            }
        }
    }
    fun close() { scope.cancel() }
}
