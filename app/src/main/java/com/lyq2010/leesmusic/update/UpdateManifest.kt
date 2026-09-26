package com.lyq2010.leesmusic.update

import kotlinx.serialization.Serializable

@Serializable
data class UpdateManifest(
    val version: String,
    val versionCode: Long,
    val packageName: String,
    val apk: String,
    val sha256: String,
    val size: Long,
    val minSdk: Int,
    val notes: String = "",
) {
    fun validate() {
        require(version.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+")))
        require(versionCode > 0 && packageName == "com.lyq2010.leesmusic")
        require(apk.matches(Regex("lees-music-[0-9.]+\\.apk")))
        require(sha256.matches(Regex("[a-fA-F0-9]{64}")))
        require(size in 1..300L * 1024 * 1024 && minSdk >= 26)
        require(notes.length <= 10000)
    }
}
