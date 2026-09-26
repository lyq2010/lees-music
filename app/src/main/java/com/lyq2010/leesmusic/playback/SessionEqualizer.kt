package com.lyq2010.leesmusic.playback

import android.media.audiofx.Equalizer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.lyq2010.leesmusic.data.settings.EqualizerPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.roundToInt

internal object EqualizerStatus {
    val unavailable = MutableStateFlow(false)
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class SessionEqualizer(private val player: ExoPlayer, private val changed: () -> Unit) {
    private var effect: Equalizer? = null
    private var session = 0
    private var preset = EqualizerPreset.Balanced
    var unavailable = false; private set
    val headroom: Float get() = if (effect != null && !unavailable) preset.headroom else 1f
    private val listener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) { apply(preset); changed() }
    }
    init { player.addListener(listener) }

    fun apply(value: EqualizerPreset) {
        preset = value
        try {
            if (session != player.audioSessionId) {
                effect?.release(); effect = null
                session = player.audioSessionId
            }
            if (session <= 0) return
            if (effect == null) effect = Equalizer(0, session)
            val eq = effect!!
            check(eq.hasControl())
            val range = eq.bandLevelRange
            check(eq.numberOfBands > 0)
            for (index in 0 until eq.numberOfBands) {
                val band = index.toShort()
                val gain = (preset.gainDb(eq.getCenterFreq(band) / 1000f) * 100).roundToInt()
                eq.setBandLevel(band, gain.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
            }
            check(eq.setEnabled(preset != EqualizerPreset.Balanced) == 0)
            unavailable = false
        } catch (_: RuntimeException) {
            effect?.release(); effect = null
            unavailable = true
        }
    }

    /** Reads the platform effect, used to verify the applied curve rather than only preferences. */
    internal fun appliedBands(): List<Pair<Int, Short>> = effect?.let { eq ->
        (0 until eq.numberOfBands).map { eq.getCenterFreq(it.toShort()) to eq.getBandLevel(it.toShort()) }
    }.orEmpty()
    fun close() { player.removeListener(listener); effect?.release(); effect = null }
}
