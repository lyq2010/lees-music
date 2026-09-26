package com.lyq2010.leesmusic.widget

import android.graphics.*

/** Small, softly sampled artwork keeps binder payloads bounded and text readable. */
internal fun widgetBackdrop(artwork: Bitmap): Bitmap {
    val sample = Bitmap.createScaledBitmap(artwork, 6, 6, true)
    val output = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(sample, null, Rect(0, 0, 320, 240), paint)
    paint.shader = LinearGradient(0f, 0f, 320f, 240f,
        intArrayOf(0x80151B2B.toInt(), 0xE0101420.toInt()), null, Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, 320f, 240f, paint)
    return output
}
