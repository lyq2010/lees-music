package com.lyq2010.leesmusic.ui.catalog

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun AlbumCover(cover: Cover, modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(1f)) {
        when (cover) {
            Cover.NightVoyage -> {
                drawRect(Color(0xFF1E2250))
                drawArc(
                    color = Color(0xFFFFB089),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(-size.width * 0.15f, size.height * 0.35f),
                    size = Size(size.width * 1.5f, size.height * 0.9f),
                    style = Stroke(width = size.minDimension * 0.16f),
                )
            }
            Cover.Morning -> {
                drawRect(Brush.linearGradient(listOf(Color(0xFFF6D7C3), Color(0xFFE28B6A))))
                val hill = Path().apply {
                    moveTo(0f, size.height * 0.55f)
                    quadraticTo(size.width * 0.35f, size.height * 0.25f, size.width, size.height * 0.6f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(hill, Color(0xFFD46A52))
            }
            Cover.FarHills -> {
                drawRect(Color(0xFF1C4F45))
                val ridge = Path().apply {
                    moveTo(0f, size.height * 0.7f)
                    quadraticTo(size.width * 0.4f, size.height * 0.2f, size.width, size.height * 0.55f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(ridge, Color(0xFF0E332C))
            }
            Cover.Echo -> {
                drawRect(Color(0xFF4A2C6A))
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFE2B84A), Color(0x004A2C6A))),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.72f, size.height * 0.62f),
                )
            }
        }
    }
}
