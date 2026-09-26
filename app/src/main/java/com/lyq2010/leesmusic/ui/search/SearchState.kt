package com.lyq2010.leesmusic.ui.search

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import com.lyq2010.leesmusic.data.api.*
import kotlinx.coroutines.CancellationException

class SearchState {
    var query by mutableStateOf("")
    var kind by mutableStateOf("song")
    var artist by mutableStateOf<Artist?>(null)
    private val pages = linkedMapOf<Pair<String, String>, SearchPage>()
    fun page(query: String = this.query.trim(), kind: String = this.kind): SearchPage =
        pages.getOrPut(query to kind) { SearchPage().also {
            if (pages.size >= 12) pages.remove(pages.keys.first())
        } }
}

class SearchPage {
    var result by mutableStateOf(SearchResult()); private set
    var loaded by mutableStateOf(false); private set
    var loading by mutableStateOf(false); private set
    var end by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    val list = LazyListState()
    private var offset = 0
    suspend fun load(kind: String, fetch: suspend (Int) -> SearchResult) {
        if (loading || end) return
        loading = true
        error = null
        try {
            val next = fetch(offset)
            val count = when (kind) { "album" -> next.album.size; "artist" -> next.artist.size; else -> next.song.size }
            val combined = SearchResult((result.song + next.song).distinctBy { it.id },
                (result.album + next.album).distinctBy { it.id }, (result.artist + next.artist).distinctBy { it.id })
            if (count > 0 && combined == result) error("服务器返回了重复结果，请稍后重试")
            result = combined
            offset += count
            end = count == 0
            loaded = true
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { error = "搜索失败，请检查连接后重试" }
        finally { loading = false }
    }
}
