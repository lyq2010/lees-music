package com.lyq2010.leesmusic.update

import java.io.File

/** Called on an I/O thread. Startup cleanup cannot race a download or its verification. */
internal object UpdateCache {
    @Synchronized fun removeInstalled(file: File, installedVersion: Long, readVersion: (File) -> Long?) {
        if (!file.isFile) return
        val version = readVersion(file)
        if (version == null || version <= installedVersion) file.delete()
    }

    @Synchronized fun download(file: File, transferAndVerify: () -> Unit) {
        try {
            file.parentFile?.mkdirs()
            transferAndVerify()
        } catch (error: Exception) {
            file.delete()
            throw error
        }
    }
}
