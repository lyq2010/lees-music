package com.lyq2010.leesmusic.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                        Text(kind.name, color = ShellText, modifier = Modifier.padding(22.dp), fontSize = 16.sp)
                    }
                }
                if (row.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
