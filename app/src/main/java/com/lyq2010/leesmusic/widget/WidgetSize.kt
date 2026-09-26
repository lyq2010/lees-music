package com.lyq2010.leesmusic.widget

import com.lyq2010.leesmusic.R

class MiniMusicWidget : MusicWidget()
class SquareMusicWidget : MusicWidget()
class StripMusicWidget : MusicWidget()

enum class WidgetSize(val label: String, val provider: Class<out MusicWidget>, val layout: Int) {
    Mini("1×1 · 快捷播放", MiniMusicWidget::class.java, R.layout.music_widget_mini),
    Square("2×2 · 封面播放器", SquareMusicWidget::class.java, R.layout.music_widget_square),
    Medium("3×2 · 音乐播放", MusicWidget::class.java, R.layout.music_widget),
    Strip("4×1 · 播放条", StripMusicWidget::class.java, R.layout.music_widget_strip),
}
