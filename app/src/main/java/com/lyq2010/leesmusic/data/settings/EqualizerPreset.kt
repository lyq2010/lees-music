package com.lyq2010.leesmusic.data.settings

import kotlin.math.ln
import kotlin.math.pow

enum class EqualizerPreset(val label: String, val description: String, private val gains: List<Float>) {
    Balanced("均衡", "自然原声", listOf(0f, 0f, 0f, 0f, 0f)),
    Pop("流行", "突出人声与节奏", listOf(1.5f, 0f, 1f, 2f, 1f)),
    Rock("摇滚", "增强低频与吉他表现", listOf(3f, 1f, -1f, 2f, 2f)),
    Jazz("爵士", "温暖低频，清晰器乐", listOf(2f, 1f, 0f, 1f, 2f)),
    Classical("古典", "舒展低频与高频", listOf(1.5f, 0f, 0f, 0.5f, 2f)),
    Electronic("电子", "强化低频与高频节奏", listOf(3f, 2f, -1f, 1f, 3f));

    // Gentle starting curves, interpolated to the device's actual logarithmic band centres.
    fun gainDb(frequencyHz: Float): Float {
        val frequencies = listOf(60f, 230f, 910f, 3600f, 14000f)
        if (frequencyHz <= frequencies.first()) return gains.first()
        if (frequencyHz >= frequencies.last()) return gains.last()
        val band = frequencies.indexOfFirst { it >= frequencyHz }
        val ratio = ln(frequencyHz / frequencies[band - 1]) / ln(frequencies[band] / frequencies[band - 1])
        return gains[band - 1] + (gains[band] - gains[band - 1]) * ratio
    }
    val headroom: Float get() = 10.0.pow(-gains.max().coerceAtLeast(0f) / 20.0).toFloat()
}
