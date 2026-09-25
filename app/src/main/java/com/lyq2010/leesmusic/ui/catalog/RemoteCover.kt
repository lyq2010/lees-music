package com.lyq2010.leesmusic.ui.catalog

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.lyq2010.leesmusic.ui.shell.ShellCard

@Composable
fun RemoteCover(url: String?, modifier: Modifier = Modifier, http: OkHttpClient) {
    var bytes by remember(url) { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(url) {
        bytes = if (url.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    http.newCall(Request.Builder().url(url).build()).execute().body.bytes()
                }.getOrNull()
            }
        }
    }
    val bitmap = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }?.asImageBitmap()
    if (bitmap == null) {
        Box(modifier.background(ShellCard))
    } else {
        Image(bitmap, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    }
}
