package com.lyq2010.leesmusic.data.settings

import android.content.Context

enum class StreamQuality(val label: String, val bitRate: Int) {
    Original("原始音质", 0), High("高品质 · 320 kbps", 320), Balanced("标准 · 192 kbps", 192), DataSaver("省流量 · 128 kbps", 128)
}

class PlaybackPreferences(context: Context) {
    val prefs = context.applicationContext.getSharedPreferences("playback-preferences", Context.MODE_PRIVATE)
    var playbackCacheMb: Int
        get() = prefs.getInt("playback-cache-mb", 512).takeIf { it in listOf(512, 1024, 2048) } ?: 512
        set(value) { if (value in listOf(512, 1024, 2048)) prefs.edit().putInt("playback-cache-mb", value).apply() }
    var equalizer: EqualizerPreset
        get() = EqualizerPreset.entries.firstOrNull { it.name == prefs.getString("equalizer", null) } ?: EqualizerPreset.Balanced
        set(value) { prefs.edit().putString("equalizer", value.name).apply() }
    var appearance: String
        get() = prefs.getString("appearance", "dark") ?: "dark"
        set(value) { if (value in listOf("system", "light", "dark")) prefs.edit().putString("appearance", value).apply() }
    var accent: String
        get() = prefs.getString("accent", "blue") ?: "blue"
        set(value) { if (value in listOf("blue", "purple", "green", "rose")) prefs.edit().putString("accent", value).apply() }
    var fadeAudio: Boolean
        get() = prefs.getBoolean("fade-audio", false)
        set(value) { prefs.edit().putBoolean("fade-audio", value).apply() }
    var notificationLyrics: Boolean
        get() = prefs.getBoolean("notification-lyrics", false)
        set(value) { prefs.edit().putBoolean("notification-lyrics", value).apply() }
    var speed: Float
        get() = prefs.getFloat("speed", 1f).takeIf { it in listOf(.75f, 1f, 1.25f, 1.5f, 2f) } ?: 1f
        set(value) { if (value in listOf(.75f, 1f, 1.25f, 1.5f, 2f)) prefs.edit().putFloat("speed", value).apply() }
    var mixAudio: Boolean
        get() = prefs.getBoolean("mix-audio", false)
        set(value) { prefs.edit().putBoolean("mix-audio", value).apply() }
    var pauseOnDisconnect: Boolean
        get() = prefs.getBoolean("pause-disconnect", true)
        set(value) { prefs.edit().putBoolean("pause-disconnect", value).apply() }
    var allowMetered: Boolean
        get() = prefs.getBoolean("allow-metered", true)
        set(value) { prefs.edit().putBoolean("allow-metered", value).apply() }
    var downloadUnmetered: Boolean
        get() = prefs.getBoolean("download-unmetered", true)
        set(value) { prefs.edit().putBoolean("download-unmetered", value).apply() }
    var quality: StreamQuality
        get() = StreamQuality.entries.firstOrNull { it.name == prefs.getString("quality", null) } ?: StreamQuality.Original
        set(value) { prefs.edit().putString("quality", value.name).apply() }
}
