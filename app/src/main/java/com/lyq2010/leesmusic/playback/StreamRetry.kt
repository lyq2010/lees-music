package com.lyq2010.leesmusic.playback

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import java.io.IOException
import java.security.cert.CertificateException
import javax.net.ssl.SSLHandshakeException

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal object StreamRetry {
    // A zero minimum retry count only exposes errors sooner; it does not disable loader retries.
    val loadPolicy = object : androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy(0) {
        override fun getRetryDelayMsFor(info: androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy.LoadErrorInfo) =
            androidx.media3.common.C.TIME_UNSET
    }
    const val MAX = 5
    fun delayMs(attempt: Int) = (1000L shl attempt.coerceIn(0, 4))
    fun isTransient(error: Throwable): Boolean {
        val causes = generateSequence(error) { it.cause }.take(12).toList()
        if (causes.any { it is SSLHandshakeException || it is CertificateException }) return false
        val http = causes.filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()
        if (http != null) return http.responseCode in setOf(408, 429) || http.responseCode in 500..599
        val playback = error as? PlaybackException
        if (playback != null && playback.errorCode !in 2000..2008) return false
        return causes.any { it is IOException } || playback?.errorCode in setOf(2000, 2001, 2002)
    }
}
