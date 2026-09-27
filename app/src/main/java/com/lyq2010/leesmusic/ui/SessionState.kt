package com.lyq2010.leesmusic.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.lyq2010.leesmusic.data.settings.ServerSettings
import com.lyq2010.leesmusic.ui.catalog.LibrarySong

internal class AccountSessionState {
    val settings = mutableStateOf<ServerSettings?>(null)
    val savedServers = mutableStateOf<List<ServerSettings>>(emptyList())
    val ready = mutableStateOf(false)
    val startupError = mutableStateOf(false)
    val recoveryError = mutableStateOf(false)
    val status = mutableStateOf("")
    val busy = mutableStateOf(false)
    private var playRequest = 0L

    fun restore(current: ServerSettings?, saved: List<ServerSettings>) {
        settings.value = current
        savedServers.value = saved
        ready.value = true
    }

    fun nextPlayRequest(): Long = ++playRequest
    fun isCurrentPlayRequest(request: Long): Boolean = request == playRequest
    fun invalidatePlayRequests() { playRequest++ }
}

internal class AlbumSelectionState {
    var title by mutableStateOf("")
    var artist by mutableStateOf("")
    var year by mutableStateOf(0)
    var coverId by mutableStateOf<String?>(null)
    var coverUrl by mutableStateOf<String?>(null)
    var songs by mutableStateOf<List<LibrarySong>>(emptyList())
    private var request = 0L

    fun nextRequest(): Long = ++request
    fun isCurrent(request: Long): Boolean = request == this.request
    fun clear() {
        request++
        title = ""
        artist = ""
        year = 0
        coverId = null
        coverUrl = null
        songs = emptyList()
    }
}
