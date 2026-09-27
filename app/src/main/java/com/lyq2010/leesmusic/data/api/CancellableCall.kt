package com.lyq2010.leesmusic.data.api

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal data class ResponseText(val code: Int, val body: String)

/** Cancel the socket as soon as the caller leaves its coroutine. */
internal suspend fun Call.awaitText(): ResponseText = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                val text = response.use { ResponseText(it.code, it.body.string()) }
                continuation.resume(text)
            } catch (error: Exception) {
                continuation.resumeWithException(error)
            }
        }
    })
}

internal suspend fun Call.awaitBytes(maxBytes: Int): ByteArray? = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                val bytes = response.use {
                    if (!it.isSuccessful) return@use null
                    val output = ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    val stream = it.body.byteStream()
                    while (true) {
                        val read = stream.read(chunk)
                        if (read < 0) break
                        if (output.size() + read > maxBytes) return@use null
                        output.write(chunk, 0, read)
                    }
                    output.toByteArray()
                }
                continuation.resume(bytes)
            } catch (error: Exception) {
                continuation.resumeWithException(error)
            }
        }
    })
}
