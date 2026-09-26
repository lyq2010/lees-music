package com.lyq2010.leesmusic.ui.library

import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException

class LibraryOverviewState {
    var data by mutableStateOf<LibraryOverview?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var loadedKey: Pair<Int, Int>? = null
    private var request = 0

    suspend fun load(generation: Int, revision: Int, force: Boolean = false, fetch: suspend () -> LibraryOverview) {
        val key = generation to revision
        if (!force && data != null && loadedKey == key) return
        val current = ++request
        loading = true
        error = null
        try {
            val result = fetch()
            if (request == current) { data = result; loadedKey = key }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (_: Exception) { if (request == current) error = "音乐库读取失败，请检查连接后重试" }
        finally { if (request == current) loading = false }
    }
}
