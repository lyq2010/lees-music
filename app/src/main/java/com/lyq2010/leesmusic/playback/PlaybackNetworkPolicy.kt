package com.lyq2010.leesmusic.playback

import android.content.Context
import android.net.ConnectivityManager
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import com.lyq2010.leesmusic.data.settings.PlaybackPreferences
import java.io.IOException

/** Enforce at the data boundary, including buffered reads and media-session initiated playback. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackNetworkPolicy(context: Context) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val preferences = PlaybackPreferences(context)
    fun blocked(): Boolean = connectivity.activeNetwork != null && !preferences.allowMetered && connectivity.isActiveNetworkMetered
    fun online(): Boolean = connectivity.activeNetwork?.let { connectivity.getNetworkCapabilities(it) }
        ?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    fun wrap(source: DataSource): DataSource = object : DataSource by source {
        private var remote = false
        override fun open(dataSpec: DataSpec): Long {
            remote = dataSpec.uri.scheme in setOf("http", "https")
            checkAllowed()
            return source.open(dataSpec)
        }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            checkAllowed()
            return source.read(buffer, offset, length)
        }
        private fun checkAllowed() {
            if (remote && blocked()) throw IOException("已关闭计费网络播放，请连接 Wi-Fi 或修改网络设置")
        }
    }
}
