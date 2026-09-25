package com.lyq2010.leesmusic.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.ui.catalog.AlbumCover
import com.lyq2010.leesmusic.ui.catalog.Cover
import com.lyq2010.leesmusic.ui.shell.ShellAccent
import com.lyq2010.leesmusic.ui.shell.ShellAccentText
import com.lyq2010.leesmusic.ui.shell.ShellBg
import com.lyq2010.leesmusic.ui.shell.ShellCard
import com.lyq2010.leesmusic.ui.shell.ShellMuted
import com.lyq2010.leesmusic.ui.shell.ShellText

@Composable
fun WelcomeScreen(onAddServer: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ShellBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AlbumCover(
            Cover.NightVoyage,
            Modifier
                .padding(top = 48.dp)
                .height(72.dp)
                .clip(RoundedCornerShape(18.dp)),
        )
        Text("Lee's Music", color = ShellText, fontSize = 32.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 20.dp))
        Text("连接 Navidrome、Emby 或 Plex", color = ShellMuted, fontSize = 16.sp)
        Surface(Modifier.fillMaxWidth().padding(top = 28.dp), color = ShellCard, shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(20.dp)) {
                Text("三台服务器", color = ShellText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("Navidrome、Emby、Plex，填一个服务器地址", color = ShellMuted, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard("锁屏接着播", "不靠一条一直挂着的连接", Modifier.weight(1f))
            FeatureCard("歌词单独打开", "不挤在播放页下面", Modifier.weight(1f))
        }
        Button(
            onClick = onAddServer,
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp).height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ShellAccent, contentColor = ShellAccentText),
        ) {
            Text("添加服务器", fontSize = 16.sp)
        }
    }
}

@Composable
private fun FeatureCard(title: String, body: String, modifier: Modifier) {
    Surface(modifier, color = ShellCard, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = ShellText, fontWeight = FontWeight.SemiBold)
            Text(body, color = ShellMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
