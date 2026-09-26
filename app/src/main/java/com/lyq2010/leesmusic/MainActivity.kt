package com.lyq2010.leesmusic

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.lyq2010.leesmusic.update.UpdateInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lyq2010.leesmusic.ui.LeesApp
import com.lyq2010.leesmusic.ui.theme.LeesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) { runCatching { UpdateInstaller.cleanupCache(applicationContext) } }
        enableEdgeToEdge()
        setContent {
            LeesTheme {
                LeesApp()
            }
        }
    }
}
