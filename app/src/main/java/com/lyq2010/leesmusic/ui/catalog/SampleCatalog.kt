package com.lyq2010.leesmusic.ui.catalog

import androidx.compose.ui.graphics.Color

data class Track(
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val cover: Cover,
    val lyrics: List<String>,
)

enum class Cover {
    NightVoyage,
    Morning,
    FarHills,
    Echo,
}

object SampleCatalog {
    val tracks = listOf(
        Track(
            title = "夜航",
            artist = "林歌",
            album = "夜航",
            durationSeconds = 221,
            cover = Cover.NightVoyage,
            lyrics = listOf("路灯还亮着", "我们沿着河走", "风把歌声送很远", "船灯一盏一盏灭"),
        ),
        Track("清晨", "林歌", "清晨", durationSeconds = 186, cover = Cover.Morning, lyrics = listOf("窗先亮了", "水还是凉的")),
        Track("远山", "林歌", "远山", durationSeconds = 204, cover = Cover.FarHills, lyrics = listOf("山在雾后面")),
        Track("回声", "林歌", "回声", durationSeconds = 198, cover = Cover.Echo, lyrics = listOf("唱完还有一声")),
    )

    val filters = listOf("最新", "随机", "常听", "收藏")

    val nowPlaying: Track = tracks.first()
}

fun Cover.playerBackground(): Color = when (this) {
    Cover.NightVoyage -> Color(0xFF2A2158)
    Cover.Morning -> Color(0xFF8C4A3A)
    Cover.FarHills -> Color(0xFF14352F)
    Cover.Echo -> Color(0xFF3A2450)
}
