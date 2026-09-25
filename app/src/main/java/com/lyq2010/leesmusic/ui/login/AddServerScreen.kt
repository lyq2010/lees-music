package com.lyq2010.leesmusic.ui.login

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.data.settings.ServerKind
import com.lyq2010.leesmusic.ui.shell.ShellBg
import com.lyq2010.leesmusic.ui.shell.ShellCard
import com.lyq2010.leesmusic.ui.shell.ShellMuted
import com.lyq2010.leesmusic.ui.shell.ShellText

@Composable
fun AddServerScreen(onBack: () -> Unit, onPick: (ServerKind) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ShellBg)
            .padding(20.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = ShellText)
        }
        Text("添加服务器", color = ShellText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text("手动添加", color = ShellMuted, modifier = Modifier.padding(top = 20.dp, bottom = 12.dp))
        ServerKind.entries.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { kind ->
                    Surface(
                        onClick = { onPick(kind) },
                        modifier = Modifier.weight(1f),
                        color = ShellCard,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ServerMark(kind)
                            Text(kind.name, color = ShellText, modifier = Modifier.padding(top = 10.dp), fontSize = 16.sp)
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ServerMark(kind: ServerKind) {
    val color = when (kind) {
        ServerKind.Navidrome -> Color(0xFF4C8DFF)
        ServerKind.Emby -> Color(0xFF52B54B)
        ServerKind.Plex -> Color(0xFFE5A00D)
    }
    Canvas(Modifier.size(42.dp)) {
        drawCircle(color)
        when (kind) {
            ServerKind.Navidrome -> {
                drawCircle(Color.White, radius = size.minDimension * 0.28f, style = Stroke(width = 3.dp.toPx()))
                drawCircle(Color.White, radius = size.minDimension * 0.08f)
            }
            ServerKind.Emby -> {
                val path = Path().apply {
                    moveTo(size.width * 0.38f, size.height * 0.28f)
                    lineTo(size.width * 0.72f, size.height * 0.5f)
                    lineTo(size.width * 0.38f, size.height * 0.72f)
                    close()
                }
                drawPath(path, Color.White)
            }
            ServerKind.Plex -> {
                val path = Path().apply {
                    moveTo(size.width * 0.5f, size.height * 0.22f)
                    lineTo(size.width * 0.78f, size.height * 0.72f)
                    lineTo(size.width * 0.22f, size.height * 0.72f)
                    close()
                }
                drawPath(path, Color.White)
            }
        }
    }
}
