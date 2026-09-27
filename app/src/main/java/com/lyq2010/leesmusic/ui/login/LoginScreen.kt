package com.lyq2010.leesmusic.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyq2010.leesmusic.data.settings.ServerEndpoint
import com.lyq2010.leesmusic.data.settings.ServerKind
import com.lyq2010.leesmusic.data.settings.ServerSettings
import com.lyq2010.leesmusic.data.settings.parseEndpoint
import com.lyq2010.leesmusic.data.settings.endpointFromHostInput
import com.lyq2010.leesmusic.data.settings.toUrl
import com.lyq2010.leesmusic.ui.shell.ShellBg
import com.lyq2010.leesmusic.ui.shell.ShellMuted
import com.lyq2010.leesmusic.ui.shell.ShellText

@Composable
fun LoginScreen(
    kind: ServerKind,
    initial: ServerSettings?,
    status: String,
    busy: Boolean,
    onBack: () -> Unit,
    onSave: (ServerSettings) -> Unit,
) {
    val saved = parseEndpoint(initial?.url.orEmpty(), httpsDefault = false)
    var host by rememberSaveable { mutableStateOf(saved.host) }
    var port by rememberSaveable { mutableStateOf(saved.port) }
    var path by rememberSaveable { mutableStateOf(saved.path) }
    var https by rememberSaveable { mutableStateOf(saved.https) }
    var username by rememberSaveable { mutableStateOf(initial?.username.orEmpty()) }
    var password by rememberSaveable { mutableStateOf(initial?.password.orEmpty()) }
    Column(
        Modifier
            .fillMaxSize()
            .background(ShellBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = ShellText)
            }
            Text("配置${kind.name}", color = ShellText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    onSave(
                        ServerSettings(
                            kind = kind,
                            url = ServerEndpoint(host, port, path, https).toUrl(),
                            username = username,
                            password = password,
                        ),
                    )
                },
                enabled = !busy && username.isNotBlank() && password.isNotBlank() && host.isNotBlank(),
            ) {
                Icon(Icons.Filled.Check, contentDescription = if (busy) "正在连接" else "保存", tint = ShellText)
            }
        }
        Text("连接设置", color = ShellMuted, modifier = Modifier.padding(top = 8.dp))
        EndpointFields(host, port, path, https, {
            val endpoint = endpointFromHostInput(it, ServerEndpoint(host, port, path, https))
            host = endpoint.host
            port = endpoint.port
            path = endpoint.path
            https = endpoint.https
        }, { port = it }, { path = it }, { https = it })
        Text("登录信息", color = ShellMuted, modifier = Modifier.padding(top = 16.dp))
        Field("用户名", username, { username = it })
        Field("密码", password, { password = it }, password = true)
        if (status.isNotEmpty()) {
            Text(status, color = ShellText, modifier = Modifier.padding(vertical = 16.dp))
        }
    }
}

@Composable
private fun EndpointFields(
    host: String,
    port: String,
    path: String,
    https: Boolean,
    onHost: (String) -> Unit,
    onPort: (String) -> Unit,
    onPath: (String) -> Unit,
    onHttps: (Boolean) -> Unit,
) {
    OutlinedTextField(
        value = host,
        onValueChange = onHost,
        label = { Text("主机地址") },
        prefix = { Text(if (https) "https://" else "http://") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    Row {
        Field("端口", port, onPort, Modifier.weight(1f))
        Field("路径", path, onPath, Modifier.weight(1f).padding(start = 8.dp))
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("使用 HTTPS", color = ShellText, modifier = Modifier.weight(1f))
        Switch(checked = https, onCheckedChange = onHttps)
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    password: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else KeyboardType.Uri),
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
    )
}
