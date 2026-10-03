package com.lyq2010.leesmusic.dependencies

import com.google.common.util.concurrent.Futures
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okio.ByteString.Companion.encodeUtf8
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DependencyCompatibilityTest {
    @Test
    fun base64RetainsPaddingForShortInputs() {
        assertEquals("TQ==", "M".encodeUtf8().base64())
        assertEquals("TWE=", "Ma".encodeUtf8().base64())
        assertEquals("TWFu", "Man".encodeUtf8().base64())
    }

    @Test
    fun androidGuavaFuturesRemainAvailable() {
        val future = Futures.immediateFuture("ready")
        assertTrue(future.isDone)
        assertEquals("ready", future.get())
    }

    @Test
    fun cancellationStillRunsCleanup() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val pending = CompletableDeferred<Unit>()
        var cleanedUp = false
        val job = launch {
            try {
                entered.complete(Unit)
                pending.await()
            } finally {
                cleanedUp = true
            }
        }
        entered.await()
        job.cancelAndJoin()
        assertTrue(cleanedUp)
        assertTrue(job.isCancelled)
    }
}
