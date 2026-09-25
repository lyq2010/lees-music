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

data class ServerSettings(
    val lanUrl: String,
    val wanUrl: String,
    val username: String,
    val password: String,
)

private val Context.serverSettingsDataStore by preferencesDataStore(name = "server_settings")

class ServerSettingsStore(private val context: Context) {
    private val lanKey = stringPreferencesKey("lan_url")
    private val wanKey = stringPreferencesKey("wan_url")
    private val userKey = stringPreferencesKey("username")
    private val passwordKey = stringPreferencesKey("password")

    suspend fun load(): ServerSettings? {
        val prefs = context.serverSettingsDataStore.data.first()
        val username = prefs[userKey] ?: return null
        val password = prefs[passwordKey]?.let(PasswordCipher::decrypt) ?: return null
        return ServerSettings(
            lanUrl = prefs[lanKey].orEmpty(),
            wanUrl = prefs[wanKey].orEmpty(),
            username = username,
            password = password,
        )
    }

    suspend fun save(settings: ServerSettings) {
        context.serverSettingsDataStore.edit { prefs ->
            prefs[lanKey] = settings.lanUrl.trim()
            prefs[wanKey] = settings.wanUrl.trim()
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
