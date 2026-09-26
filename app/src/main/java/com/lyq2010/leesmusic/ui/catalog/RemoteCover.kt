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
fun RemoteCover(coverId: String?, url: String?, modifier: Modifier = Modifier, http: OkHttpClient,
    placeholder: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    val cacheKey = url?.let { com.lyq2010.leesmusic.data.library.resourceCacheIdentity(it) }
    var bitmap by remember(cacheKey) { mutableStateOf<Bitmap?>(cacheKey?.let(CoverImages::peek)) }
    LaunchedEffect(cacheKey) {
        if (coverId.isNullOrBlank() || url.isNullOrBlank() || bitmap != null) return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) {
            runCatching { CoverImages.load(context, cacheKey ?: return@withContext null, url, http) }.getOrNull()
        }
    }
    if (bitmap == null) {
        Box(modifier.background(ShellCard), contentAlignment = androidx.compose.ui.Alignment.Center) { placeholder() }
    } else {
        Image(bitmap!!.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    }
}
