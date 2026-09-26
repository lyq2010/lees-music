package com.lyq2010.leesmusic.ui

import com.lyq2010.leesmusic.data.settings.EqualizerPreset
import org.junit.Assert.*
import org.junit.Test

class EqualizerPresetTest {
    @Test fun balancedIsFlatAndPreservesGain() {
        listOf(20f, 60f, 1000f, 20000f).forEach { assertEquals(0f, EqualizerPreset.Balanced.gainDb(it), 0f) }
        assertEquals(1f, EqualizerPreset.Balanced.headroom, 0f)
    }
    @Test fun genresAreBoundedAndReserveHeadroom() {
        EqualizerPreset.entries.forEach { preset ->
            (20..20000 step 40).forEach { hz -> assertTrue(preset.gainDb(hz.toFloat()) in -3f..3f) }
            assertTrue(preset.headroom in .7f..1f)
        }
        assertEquals(3f, EqualizerPreset.Rock.gainDb(60f), .001f)
        assertEquals(2f, EqualizerPreset.Rock.gainDb(kotlin.math.sqrt(60f * 230f)), .001f)
    }
}
