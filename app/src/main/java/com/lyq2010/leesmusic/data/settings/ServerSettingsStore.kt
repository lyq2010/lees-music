package com.lyq2010.leesmusic.data.settings

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
private data class SavedServer(val kind: String, val url: String, val username: String, val encryptedPassword: String)

enum class ServerKind {
    Navidrome,
    Emby,
    Plex,
}

data class ServerSettings(
    val kind: ServerKind = ServerKind.Navidrome,
    val url: String,
    val username: String,
    val password: String,
)

private val Context.serverSettingsDataStore by preferencesDataStore(name = "server_settings")

class ServerSettingsStore(private val context: Context) {
    suspend fun archiveUnreadableSettings(): Boolean {
        val source = File(context.filesDir, "datastore/server_settings.preferences_pb")
        if (!source.isFile) return false
        val backup = File(context.filesDir, "recovery/server_settings_${System.currentTimeMillis()}.preferences_pb")
        if (backup.parentFile?.mkdirs() != true && backup.parentFile?.isDirectory != true) return false
        if (!source.renameTo(backup)) return false
        context.serverSettingsDataStore.edit { it.clear() }
        return true
    }

    private val serversKey = stringPreferencesKey("saved_servers")
    suspend fun savedServers(): List<ServerSettings> {
        val prefs = context.serverSettingsDataStore.data.first()
        val records = prefs[serversKey]?.let { runCatching { Json.decodeFromString<List<SavedServer>>(it) }.getOrNull() }.orEmpty()
        val saved = records.mapNotNull { record ->
            runCatching { ServerSettings(ServerKind.valueOf(record.kind), record.url, record.username,
                PasswordCipher.decrypt(record.encryptedPassword)) }.getOrNull()
        }
        val active = load()
        return (saved + listOfNotNull(active)).distinctBy { Triple(it.kind, it.url, it.username) }
    }
    private val kindKey = stringPreferencesKey("kind")
    private val urlKey = stringPreferencesKey("url")
    private val lanKey = stringPreferencesKey("lan_url")
    private val wanKey = stringPreferencesKey("wan_url")
    private val userKey = stringPreferencesKey("username")
    private val passwordKey = stringPreferencesKey("password")

    suspend fun load(): ServerSettings? {
        val prefs = context.serverSettingsDataStore.data.first()
        val username = prefs[userKey] ?: return null
        val password = prefs[passwordKey]?.let(PasswordCipher::decrypt) ?: return null
        val kind = prefs[kindKey]?.let { runCatching { ServerKind.valueOf(it) }.getOrNull() } ?: ServerKind.Navidrome
        return ServerSettings(
            kind = kind,
            url = normalizeServerUrl(prefs[urlKey] ?: prefs[lanKey] ?: prefs[wanKey].orEmpty()),
            username = username,
            password = password,
        )
    }

    suspend fun save(settings: ServerSettings) {
        context.serverSettingsDataStore.edit { prefs ->
            val existing = prefs[serversKey]?.let { raw ->
                runCatching { Json.decodeFromString<List<SavedServer>>(raw) }.getOrElse {
                    val backup = File(context.filesDir, "recovery/saved_servers_${System.currentTimeMillis()}.json")
                    check(backup.parentFile?.mkdirs() == true || backup.parentFile?.isDirectory == true)
                    backup.writeText(raw)
                    emptyList()
                }
            }.orEmpty().toMutableList()
            val legacyUrl = prefs[urlKey] ?: prefs[lanKey] ?: prefs[wanKey]
            if (legacyUrl != null && prefs[userKey] != null && prefs[passwordKey] != null) {
                val legacy = SavedServer(prefs[kindKey] ?: ServerKind.Navidrome.name, normalizeServerUrl(legacyUrl), prefs[userKey]!!, prefs[passwordKey]!!)
                if (existing.none { it.kind == legacy.kind && it.url == legacy.url && it.username == legacy.username }) existing += legacy
            }
            existing.removeAll { it.kind == settings.kind.name && it.url == settings.url.trim() && it.username == settings.username.trim() }
            existing += SavedServer(settings.kind.name, settings.url.trim(), settings.username.trim(), PasswordCipher.encrypt(settings.password))
            prefs[serversKey] = Json.encodeToString(existing)
            prefs[kindKey] = settings.kind.name
            prefs[urlKey] = settings.url.trim()
            prefs.remove(lanKey)
            prefs.remove(wanKey)
            prefs[userKey] = settings.username.trim()
            prefs[passwordKey] = PasswordCipher.encrypt(settings.password)
        }
    }
}

private object PasswordCipher {
    private const val ALIAS = "lees_music_server_password"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = cipher.iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted).toString(Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }
}
