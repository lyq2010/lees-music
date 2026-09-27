package com.lyq2010.leesmusic.ui

import org.junit.Assert.*
import org.junit.Test

class SessionStateTest {
    @Test fun accountChangeInvalidatesOldPlayPreparation() {
        val session = AccountSessionState()
        val old = session.nextPlayRequest()
        session.invalidatePlayRequests()
        assertFalse(session.isCurrentPlayRequest(old))
        assertTrue(session.isCurrentPlayRequest(session.nextPlayRequest()))
    }

    @Test fun leavingAnAlbumSelectionInvalidatesItsRequest() {
        val selection = AlbumSelectionState()
        val old = selection.nextRequest()
        val latest = selection.nextRequest()
        assertFalse(selection.isCurrent(old))
        assertTrue(selection.isCurrent(latest))
        selection.clear()
        assertFalse(selection.isCurrent(latest))
    }
}
