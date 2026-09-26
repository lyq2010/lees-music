package com.lyq2010.leesmusic.ui.shell

import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.lyq2010.leesmusic.data.settings.PlaybackPreferences

@Composable
fun ShellTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { PlaybackPreferences(context) }
    var revision by remember { mutableIntStateOf(0) }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> revision++ }
        preferences.prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val mode = remember(revision) { preferences.appearance }
    val accent = remember(revision) { preferences.accent }
    val dark = when (mode) { "light" -> false; "system" -> isSystemInDarkTheme(); else -> true }
    val primary = when (accent) {
        "purple" -> if (dark) Color(0xFFD3BBFF) else Color(0xFF6941A5)
        "green" -> if (dark) Color(0xFF8AD5B0) else Color(0xFF166B4D)
        "rose" -> if (dark) Color(0xFFFFB0CB) else Color(0xFF993B62)
        else -> if (dark) Color(0xFFBFD4FF) else Color(0xFF315E9C)
    }
    val colors = if (dark) darkColorScheme(primary = primary, onPrimary = Color(0xFF15202B),
        background = Color(0xFF101114), onBackground = Color(0xFFF3F4F6), surface = Color(0xFF1C1E24),
        onSurface = Color(0xFFF3F4F6), onSurfaceVariant = Color(0xFFADB2BD), surfaceContainerHighest = Color(0xFF27292E),
        secondaryContainer = Color(0xFF3D465A), onSecondaryContainer = primary)
    else lightColorScheme(primary = primary, onPrimary = Color.White, background = Color(0xFFF5F6FA),
        onBackground = Color(0xFF1C1E24), surface = Color.White, onSurface = Color(0xFF1C1E24),
        onSurfaceVariant = Color(0xFF555D6A), surfaceContainerHighest = Color(0xFFE8EBF1),
        secondaryContainer = primary.copy(alpha = .15f), onSecondaryContainer = primary)
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalContentColor provides colors.onSurface, content = content)
    }
}
