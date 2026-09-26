package com.lyq2010.leesmusic.data.library

import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import org.junit.Assert.*
import org.junit.Test

class DownloadedSongsTest {
    @Test fun downloadsResolveInEveryQueuePositionWithoutChangingOrderOrMetadata() {
        val songs = listOf(LibrarySong(id = "a", title = "A", artist = "Artist", coverArtId = null, coverUrl = null),
            LibrarySong(id = "b", title = "B", artist = "Artist", coverArtId = null, coverUrl = null),
            LibrarySong(id = "a", title = "A", artist = "Artist", coverArtId = null, coverUrl = null))
        val resolved = withDownloadedSongs(songs, mapOf("a" to "content://downloads/42"))
        assertEquals(songs.map { it.id }, resolved.map { it.id })
        assertEquals(listOf("content://downloads/42", null, "content://downloads/42"), resolved.map { it.localUri })
        assertEquals(songs, resolved.map { it.copy(localUri = null) })
    }
    @Test fun anotherAccountWithoutVerifiedDownloadsKeepsRemoteSongs() {
        val songs = listOf(LibrarySong(id = "a", title = "A", artist = "Artist", coverArtId = null, coverUrl = null))
        assertEquals(songs, withDownloadedSongs(songs, emptyMap()))
    }
}
