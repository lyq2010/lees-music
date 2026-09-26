package com.lyq2010.leesmusic.ui.library

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException

/** Owned above navigation and tabs, recreated whenever the configured account changes. */
class LibraryBrowseCache {
    val overview = LibraryOverviewState()
    private val pages = mutableMapOf<Pair<String, String>, LibraryPageState>()
    var generation by mutableIntStateOf(0)
        private set

    fun page(target: LibraryDestination): LibraryPageState =
        pages.getOrPut(target.kind to target.id) { LibraryPageState() }

    fun invalidate() { generation++ }
}

class LibraryPageState {
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
            val result = fetch()
            if (current == request) {
                data = result
                loadedGeneration = generation
                loadedRevision = revision
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
