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
    val lanInitial = parseEndpoint(initial?.lanUrl.orEmpty(), httpsDefault = false)
    val wanInitial = parseEndpoint(initial?.wanUrl.orEmpty(), httpsDefault = true)
    var lanHost by rememberSaveable { mutableStateOf(lanInitial.host) }
    var lanPort by rememberSaveable { mutableStateOf(lanInitial.port) }
    var lanPath by rememberSaveable { mutableStateOf(lanInitial.path) }
    var lanHttps by rememberSaveable { mutableStateOf(lanInitial.https) }
    var wanHost by rememberSaveable { mutableStateOf(wanInitial.host) }
    var wanPort by rememberSaveable { mutableStateOf(wanInitial.port) }
    var wanPath by rememberSaveable { mutableStateOf(wanInitial.path) }
    var wanHttps by rememberSaveable { mutableStateOf(wanInitial.https) }
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
                            lanUrl = ServerEndpoint(lanHost, lanPort, lanPath, lanHttps).toUrl(),
                            wanUrl = ServerEndpoint(wanHost, wanPort, wanPath, wanHttps).toUrl(),
                            username = username,
                            password = password,
                        ),
                    )
                },
                enabled = !busy && username.isNotBlank() && password.isNotBlank() && (lanHost.isNotBlank() || wanHost.isNotBlank()),
            ) {
                Icon(Icons.Filled.Check, contentDescription = if (busy) "正在连接" else "保存", tint = ShellText)
            }
        }
        Text("内网", color = ShellMuted, modifier = Modifier.padding(top = 8.dp))
        EndpointFields(lanHost, lanPort, lanPath, lanHttps, { lanHost = it }, { lanPort = it }, { lanPath = it }, { lanHttps = it })
        Text("外网", color = ShellMuted, modifier = Modifier.padding(top = 16.dp))
        EndpointFields(wanHost, wanPort, wanPath, wanHttps, { wanHost = it }, { wanPort = it }, { wanPath = it }, { wanHttps = it })
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
    Field("主机地址", host, onHost)
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
