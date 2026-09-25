package com.lyq2010.leesmusic.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.data.settings.ServerKind
import com.lyq2010.leesmusic.data.settings.ServerSettings
import com.lyq2010.leesmusic.ui.theme.Ink

@Composable
fun LoginScreen(
    initial: ServerSettings?,
    status: String,
    busy: Boolean,
    onSave: (ServerSettings) -> Unit,
) {
    var kindName by rememberSaveable { mutableStateOf((initial?.kind ?: ServerKind.Navidrome).name) }
    val kind = ServerKind.valueOf(kindName)
    var lanUrl by rememberSaveable { mutableStateOf(initial?.lanUrl.orEmpty()) }
    var wanUrl by rememberSaveable { mutableStateOf(initial?.wanUrl.orEmpty()) }
    var username by rememberSaveable { mutableStateOf(initial?.username.orEmpty()) }
    var password by rememberSaveable { mutableStateOf(initial?.password.orEmpty()) }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("连接服务器", color = Ink, fontSize = 28.sp, modifier = Modifier.padding(bottom = 8.dp))
        Text("可选 Navidrome、Emby、Plex。内网可以是 http，外网必须是 https。", color = Ink, fontSize = 14.sp)
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ServerKind.entries.forEach { item ->
                Button(onClick = { kindName = item.name }, enabled = kind != item) {
                    Text(item.name)
                }
            }
        }
        Field("内网地址", lanUrl) { lanUrl = it }
        Field("外网地址", wanUrl) { wanUrl = it }
        Field("用户名", username) { username = it }
        Field("密码", password, password = true) { password = it }
        Button(
            onClick = {
                onSave(ServerSettings(kind, lanUrl, wanUrl, username, password))
            },
            enabled = !busy && username.isNotBlank() && password.isNotBlank() && wanUrl.isNotBlank(),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(if (busy) "正在连接" else "保存并连接")
        }
        if (status.isNotEmpty()) {
            Text(status, color = Ink, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun Field(label: String, value: String, password: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    )
}
