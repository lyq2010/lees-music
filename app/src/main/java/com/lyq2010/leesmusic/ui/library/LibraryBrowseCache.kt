package com.lyq2010.leesmusic.ui.library

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException

/** Owned above navigation and tabs, recreated whenever the configured account changes. */
class LibraryBrowseCache(private val disk: com.lyq2010.leesmusic.data.library.LibraryDiskCache? = null) {
    val overview = LibraryOverviewState({ disk?.overview() }, { disk?.saveOverview(it) })
    private val pages = mutableMapOf<Pair<String, String>, LibraryPageState>()
    private val albums = mutableMapOf<String, Pair<Int, com.lyq2010.leesmusic.data.api.Album>>()
    var generation by mutableIntStateOf(0)
        private set

    fun page(target: LibraryDestination): LibraryPageState =
        pages.getOrPut(target.kind to target.id) { LibraryPageState({ disk?.page(target) }, { disk?.save(target, it) }) }

    fun invalidate() { generation++ }

    suspend fun album(id: String, fetch: suspend () -> com.lyq2010.leesmusic.data.api.Album): com.lyq2010.leesmusic.data.api.Album {
        albums[id]?.takeIf { it.first == generation }?.let { return it.second }
        val requestedGeneration = generation
        val saved = if (generation == 0) disk?.album(id) else null
        val result = saved ?: fetch()
        if (generation == requestedGeneration) {
            albums[id] = generation to result
            if (saved == null) disk?.saveAlbum(result)
        }
        return result
    }
}

class LibraryPageState(private val restore: suspend () -> LibraryPage? = { null },
    private val persist: suspend (LibraryPage) -> Unit = {}) {
    var data by mutableStateOf<LibraryPage?>(null)
        private set
    var filter by mutableStateOf("")
    val list = LazyListState()
    val grid = androidx.compose.foundation.lazy.grid.LazyGridState()
    var artistGrid by mutableStateOf(true)
    var artistSort by mutableIntStateOf(0)
    var favoritesOnly by mutableStateOf(false)
    fun updateArtist(id: String, favorite: Boolean) {
        data = data?.let { page -> page.copy(artists = page.artists.map {
            if (it.id == id) it.copy(starred = if (favorite) "saved" else null) else it
        }) }
    }
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var loadedGeneration = -1
    private var loadedRevision = -1
    private var request = 0

    suspend fun load(generation: Int, revision: Int, force: Boolean = false, fetch: suspend () -> LibraryPage) {
        if (!force && data != null && loadedGeneration == generation && loadedRevision == revision) return
        val current = ++request
        loading = true
        error = null
        try {
            val cached = if (!force && data == null && generation == 0 && revision == 0) restore() else null
            val result = cached ?: fetch()
            if (current == request) {
                data = result
                loadedGeneration = generation
                loadedRevision = revision
                if (cached == null) persist(result)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (current == request) error = "读取失败，请检查连接后重试"
        } finally {
            if (current == request) loading = false
        }
    }
}
