package com.lyq2010.leesmusic.playback

import androidx.media3.common.C
import androidx.media3.datasource.*
import java.io.EOFException

/** A server refusing Range is retried as a full GET; discarded bytes are never returned as audio. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class RangeFallbackDataSource(private val upstream: DataSource) : DataSource by upstream {
    private var remaining = C.LENGTH_UNSET.toLong()
    override fun open(dataSpec: DataSpec): Long {
        remaining = C.LENGTH_UNSET.toLong()
        return try { upstream.open(dataSpec) } catch (error: HttpDataSource.InvalidResponseCodeException) {
            if (error.responseCode != 416 || dataSpec.position == 0L) throw error
            upstream.close()
            val length = upstream.open(dataSpec.buildUpon().setPosition(0).setLength(C.LENGTH_UNSET.toLong()).build())
            var skip = dataSpec.position
            val buffer = ByteArray(32768)
            while (skip > 0) {
                val read = upstream.read(buffer, 0, minOf(skip, buffer.size.toLong()).toInt())
                if (read == C.RESULT_END_OF_INPUT) throw EOFException("Requested audio position exceeds resource")
                skip -= read
            }
            val available = if (length == C.LENGTH_UNSET.toLong()) length else length - dataSpec.position
            remaining = when {
                dataSpec.length == C.LENGTH_UNSET.toLong() -> available
                available == C.LENGTH_UNSET.toLong() -> dataSpec.length
                else -> minOf(dataSpec.length, available)
            }
            remaining
        }
    }
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (remaining == 0L) return C.RESULT_END_OF_INPUT
        val read = upstream.read(buffer, offset, if (remaining < 0) length else minOf(length.toLong(), remaining).toInt())
        if (read > 0 && remaining > 0) remaining -= read
        return read
    }
}
