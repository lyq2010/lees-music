package com.lyq2010.leesmusic.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF1E2250)
val Peach = Color(0xFFFFD6B8)
val Coral = Color(0xFFFF8A65)
val Paper = Color(0xFFF7F4EF)
val PlayerInk = Color(0xFF2A2158)

private val LeesLight = lightColorScheme(
    primary = Ink,
    onPrimary = Peach,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
)

@Composable
fun LeesTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(colorScheme = LeesLight) {
        Surface(modifier = Modifier.fillMaxSize(), color = Paper, contentColor = Ink, content = content)
    }
}
