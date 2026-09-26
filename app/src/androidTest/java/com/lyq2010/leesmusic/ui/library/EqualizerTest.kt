package com.lyq2010.leesmusic.ui.library

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.platform.app.InstrumentationRegistry
import com.lyq2010.leesmusic.data.settings.*
import com.lyq2010.leesmusic.playback.SessionEqualizer
import com.lyq2010.leesmusic.ui.settings.EqualizerScreen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class EqualizerTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun presetsPersistAndBalancedRestoresDefault() {
        val prefs = PlaybackPreferences(context); val old = prefs.equalizer
        try {
            compose.setContent { EqualizerScreen(prefs) {} }
            compose.onNodeWithText("电子").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(EqualizerPreset.Electronic, PlaybackPreferences(context).equalizer) }
            compose.onNodeWithText("均衡（默认）").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(EqualizerPreset.Balanced, PlaybackPreferences(context).equalizer) }
        } finally { prefs.equalizer = old }
    }
    @Test fun bothAudioSessionsReceiveRealPlatformBandLevels() {
        compose.runOnUiThread {
            val players = List(2) { ExoPlayer.Builder(context).build().apply {
                setAudioSessionId((context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager).generateAudioSessionId())
            } }
            val effects = players.map { SessionEqualizer(it) {} }
            try {
                EqualizerPreset.entries.forEach { preset ->
                    effects.forEach { effect ->
                        effect.apply(preset)
                        assertFalse("MuMu equalizer unavailable", effect.unavailable)
                        val bands = effect.appliedBands()
                        assertTrue("No platform bands", bands.isNotEmpty())
                        bands.forEach { (frequency, gain) ->
                            // MuMu's platform effect quantizes gains to whole dB (e.g. 1.5 -> 2).
                            assertEquals(preset.gainDb(frequency / 1000f) * 100, gain.toFloat(), 50f)
                        }
                    }
                    assertEquals(effects[0].appliedBands(), effects[1].appliedBands())
                }
            } finally { effects.forEach { it.close() }; players.forEach { it.release() } }
        }
    }
}
