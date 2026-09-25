package com.lyq2010.leesmusic.ui.catalog

import android.graphics.Bitmap
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
import androidx.compose.ui.platform.LocalContext
import com.lyq2010.leesmusic.ui.shell.ShellCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@Composable
fun RemoteCover(coverId: String?, url: String?, modifier: Modifier = Modifier, http: OkHttpClient) {
    val context = LocalContext.current
    var bitmap by remember(coverId) { mutableStateOf<Bitmap?>(coverId?.let(CoverImages::peek)) }
    LaunchedEffect(coverId) {
        if (coverId.isNullOrBlank() || url.isNullOrBlank() || bitmap != null) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            runCatching { CoverImages.load(context, coverId, url, http) }.getOrNull()
        }
    }
    if (bitmap == null) {
        Box(modifier.background(ShellCard))
    } else {
        Image(bitmap!!.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    }
}
