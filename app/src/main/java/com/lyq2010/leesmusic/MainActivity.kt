package com.lyq2010.leesmusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lyq2010.leesmusic.ui.LeesApp
import com.lyq2010.leesmusic.ui.theme.LeesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LeesTheme {
                LeesApp()
            }
        }
    }
}
