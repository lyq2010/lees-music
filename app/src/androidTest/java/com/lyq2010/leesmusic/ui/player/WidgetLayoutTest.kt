package com.lyq2010.leesmusic.ui.player

import android.view.View
import android.widget.FrameLayout
import androidx.test.platform.app.InstrumentationRegistry
import com.lyq2010.leesmusic.R
import com.lyq2010.leesmusic.widget.MusicWidget
import com.lyq2010.leesmusic.widget.WidgetSize
import org.junit.Assert.*
import org.junit.Test

class WidgetLayoutTest {
    @Test fun allFourRemoteViewsInflateAndKeepControlsInsideTheirCells() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val density = context.resources.displayMetrics.density
        android.appwidget.AppWidgetManager.getInstance(context).installedProviders
            .filter { it.provider.packageName == context.packageName }.forEach {
                println("WIDGET ${it.provider.shortClassName} min=${it.minWidth}x${it.minHeight} target=${it.targetCellWidth}x${it.targetCellHeight} density=$density")
            }
        val dimensions = listOf(48 to 48, 140 to 104, 180 to 104, 280 to 50)
        instrumentation.runOnMainSync {
            WidgetSize.entries.zip(dimensions).forEach { (size, bounds) ->
                val view = MusicWidget.views(context, size.layout).apply(context, FrameLayout(context))
                val width = (bounds.first * density).toInt()
                val height = (bounds.second * density).toInt()
                view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, width, height)
                val rootLocation = IntArray(2).also(view::getLocationOnScreen)
                val controls = if (size == WidgetSize.Mini) listOf(R.id.widget_play)
                    else if (size == WidgetSize.Square) listOf(R.id.widget_play, R.id.widget_next)
                    else listOf(R.id.widget_previous, R.id.widget_play, R.id.widget_next)
                controls.forEach { id ->
                    val control = view.findViewById<View>(id)
                    val location = IntArray(2).also(control::getLocationOnScreen)
                    val x = location[0] - rootLocation[0]; val y = location[1] - rootLocation[1]
                    assertTrue("${size.label}: control outside cell", x >= 0 && y >= 0 &&
                        x + control.width <= width && y + control.height <= height)
                    assertTrue("${size.label}: zero-size control", control.width > 0 && control.height > 0)
                }
            }
        }
    }
    @Test fun renderPickerPreviewsWhenRequested() {
        if (InstrumentationRegistry.getArguments().getString("widgetPreviews") != "true") return
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val density = context.resources.displayMetrics.density
        val dimensions = listOf(96 to 96, 192 to 192, 288 to 192, 384 to 96)
        val names = listOf("music_widget_mini_preview", "music_widget_square_preview", "music_widget_preview", "music_widget_strip_preview")
        instrumentation.runOnMainSync {
            WidgetSize.entries.forEachIndexed { index, size ->
                val view = MusicWidget.views(context, size.layout).apply(context, FrameLayout(context))
                val width = (dimensions[index].first * density).toInt()
                val height = (dimensions[index].second * density).toInt()
                view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, width, height)
                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                val clip = android.graphics.Path().apply {
                    addRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 24 * density, 24 * density,
                        android.graphics.Path.Direction.CW)
                }
                canvas.clipPath(clip)
                view.draw(canvas)
                java.io.File(context.getExternalFilesDir(null), "${names[index]}.png").outputStream().use {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                bitmap.recycle()
            }
        }
    }

}
