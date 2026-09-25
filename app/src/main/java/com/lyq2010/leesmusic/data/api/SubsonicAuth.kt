package com.lyq2010.leesmusic.data.api

import java.security.MessageDigest
import java.security.SecureRandom

object SubsonicAuth {
    private val random = SecureRandom()

    fun salt(): String = ByteArray(6).also(random::nextBytes).joinToString("") { "%02x".format(it) }

    fun token(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("MD5").digest((password + salt).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
