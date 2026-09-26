package com.lyq2010.leesmusic.data.library

import android.content.Context
import com.lyq2010.leesmusic.data.api.Song
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

@Serializable
data class SavedDailyMix(
    val day: String,
    val songs: List<Song>,
)

fun dailyMixIsCurrent(savedDay: String, today: String): Boolean = savedDay == today

fun millisUntilNextMidnight(now: ZonedDateTime = ZonedDateTime.now()): Long {
    val next = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
    return Duration.between(now, next).toMillis().coerceAtLeast(1_000L)
}

fun todayStamp(now: ZonedDateTime = ZonedDateTime.now()): String = now.toLocalDate().toString()

class DailyMixStore(context: Context, namespace: String) {
    private val file = File(context.filesDir, "libraries/$namespace/daily-mix.json")
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): SavedDailyMix? {
        if (!file.exists()) return null
        return runCatching { json.decodeFromString(SavedDailyMix.serializer(), file.readText()) }.getOrNull()
    }

    fun save(songs: List<Song>, day: String = LocalDate.now().toString()) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(SavedDailyMix.serializer(), SavedDailyMix(day, songs)))
    }
}
