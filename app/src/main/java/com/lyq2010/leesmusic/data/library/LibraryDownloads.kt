package com.lyq2010.leesmusic.data.library

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.lyq2010.leesmusic.data.api.SubsonicClient
import com.lyq2010.leesmusic.data.api.SubsonicServer
import com.lyq2010.leesmusic.ui.catalog.LibrarySong
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SavedDownload(val requestId: Long, val song: LibrarySong)
data class LibraryDownload(val song: LibrarySong, val status: Int, val bytes: Long, val total: Long, val uri: String?) {
    val playable get() = status == DownloadManager.STATUS_SUCCESSFUL && uri != null
    val label get() = when (status) {
        DownloadManager.STATUS_SUCCESSFUL -> if (uri != null) "已下载" else "文件不可用，请重试"
        DownloadManager.STATUS_FAILED -> "下载失败，请重试"
        DownloadManager.STATUS_PAUSED -> "等待网络或系统恢复"
        DownloadManager.STATUS_RUNNING -> if (total > 0) "下载中 ${bytes * 100 / total}%" else "下载中"
        else -> "等待下载"
    }
}

/** Android system owns transfer lifetime. Records and files are scoped to the server/account. */
class LibraryDownloads(context: Context, private val namespace: String) {
    private val app = context.applicationContext
    private val manager = app.getSystemService(DownloadManager::class.java)
    private val prefs = app.getSharedPreferences("downloads-$namespace", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private fun records(): List<SavedDownload> = prefs.all.values.filterIsInstance<String>()
        .mapNotNull { runCatching { json.decodeFromString<SavedDownload>(it) }.getOrNull() }

    fun list(): List<LibraryDownload> = records().map { record ->
        manager.query(DownloadManager.Query().setFilterById(record.requestId)).use { cursor ->
            if (!cursor.moveToFirst()) return@map LibraryDownload(record.song, DownloadManager.STATUS_FAILED, 0, 0, null)
            var status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val mediaType = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE)).orEmpty()
            // Subsonic can return an XML error document with HTTP 200; it is not an offline song.
            if (mediaType.contains("xml", true) || mediaType.contains("html", true) || mediaType.contains("json", true)) status = DownloadManager.STATUS_FAILED
            var uri: String? = null
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                uri = manager.getUriForDownloadedFile(record.requestId)?.toString()
                if (uri != null && runCatching { app.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } == true }.getOrDefault(false).not()) uri = null
            }
            LibraryDownload(record.song, status,
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)), uri)
        }
    }

    fun enqueue(server: SubsonicServer, song: LibrarySong): String {
        val existing = list().firstOrNull { it.song.id == song.id }
        if (existing?.playable == true) return "这首歌已经下载"
        if (existing != null && existing.status in listOf(DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED))
            return "已在下载列表中"
        val extension = song.suffix.filter { it.isLetterOrDigit() }.take(12).ifBlank { "audio" }
        val path = "$namespace/${java.util.UUID.randomUUID()}.$extension"
        val directory = app.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: error("存储空间不可用")
        java.io.File(directory, namespace).mkdirs()
        val request = DownloadManager.Request(Uri.parse(SubsonicClient().downloadUrl(server, song.id)))
            .setAllowedOverMetered(!com.lyq2010.leesmusic.data.settings.PlaybackPreferences(app).downloadUnmetered)
            .setAllowedOverRoaming(false)
            .setTitle(song.title).setDescription(song.artist)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(app, Environment.DIRECTORY_MUSIC, path)
        val id = manager.enqueue(request)
        // Do not persist signed artwork URLs or remote credentials.
        val saved = SavedDownload(id, song.copy(coverUrl = null, localUri = null))
        prefs.edit().putString(song.id, json.encodeToString(saved)).apply()
        return "已加入下载"
    }
}
