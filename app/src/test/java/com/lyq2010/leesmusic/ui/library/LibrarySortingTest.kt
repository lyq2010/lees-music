package com.lyq2010.leesmusic.ui.library

import com.lyq2010.leesmusic.ui.catalog.LibraryAlbum
import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySortingTest {
    @Test fun playlistDefaultsToServerAdditionOrderAndKeepsOriginalIndicesAfterSort() {
        val songs = listOf(
            LibrarySong("same", "Z", "B", null, null),
            LibrarySong("other", "A", "A", null, null),
            LibrarySong("same", "Z", "B", null, null),
        )
        assertEquals(listOf(0, 1, 2), sortedSongs(songs, 0).map { it.index })
        assertEquals(listOf(1, 0, 2), sortedSongs(songs, 1).map { it.index })
        assertEquals(listOf(0, 2, 1), sortedSongs(songs, 2).map { it.index })
    }

    @Test fun albumsCanSortByNameAndArtist() {
        val albums = listOf(LibraryAlbum("z", "Z", "A", null, null), LibraryAlbum("a", "A", "B", null, null))
        assertEquals(listOf("a", "z"), sortedAlbums(albums, 1).map { it.id })
        assertEquals(listOf("z", "a"), sortedAlbums(albums, 3).map { it.id })
    }
}
