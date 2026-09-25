package com.lyq2010.leesmusic.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lyq2010.leesmusic.ui.catalog.SampleCatalog
import com.lyq2010.leesmusic.ui.home.HomeScreen
import com.lyq2010.leesmusic.ui.lyrics.LyricsScreen
import com.lyq2010.leesmusic.ui.player.PlayerScreen

private object Routes {
    const val Home = "home"
    const val Player = "player"
    const val Lyrics = "lyrics"
}

@Composable
fun LeesApp() {
    val nav = rememberNavController()
    val track = SampleCatalog.nowPlaying
    NavHost(navController = nav, startDestination = Routes.Home, modifier = Modifier.fillMaxSize()) {
        composable(Routes.Home) {
            HomeScreen(
                nowPlaying = track,
                onOpenPlayer = { nav.navigate(Routes.Player) },
                onOpenSettings = {},
            )
        }
        composable(Routes.Player) {
            PlayerScreen(
                track = track,
                onBack = { nav.popBackStack() },
                onOpenLyrics = { nav.navigate(Routes.Lyrics) },
            )
        }
        composable(Routes.Lyrics) {
            LyricsScreen(track = track, onBack = { nav.popBackStack() })
        }
    }
}
