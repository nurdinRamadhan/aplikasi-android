package com.alhasanah.alhasanahmedia.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.alhasanah.alhasanahmedia.data.model.ChatOutboxMessage
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChatOutboxStore(context: Context) {
    private val prefs = context.getSharedPreferences("alumni_chat_outbox", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val cipher = LocalOutboxCipher()

    fun getAll(): List<ChatOutboxMessage> {
        val raw = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()
        val decrypted = runCatching {
            if (raw.startsWith(ENCRYPTED_PREFIX)) cipher.decrypt(raw) else raw
        }.getOrNull() ?: return emptyList()
        return runCatching { json.decodeFromString<List<ChatOutboxMessage>>(decrypted) }.getOrDefault(emptyList())
    }

    fun getConversation(conversationId: String): List<ChatOutboxMessage> {
        return getAll()
            .filter { it.conversationId == conversationId }
            .sortedBy { it.createdAt }
    }

    fun getConversations(): Set<String> =
        getAll().map { it.conversationId }.filter { it.isNotBlank() }.toSet()

    fun enqueue(message: ChatOutboxMessage) {
        val messages = (getAll().filterNot { it.id == message.id } + message).sortedBy { it.createdAt }
        save(messages)
    }

    fun remove(messageId: String) {
        save(getAll().filterNot { it.id == messageId })
    }

    private fun save(messages: List<ChatOutboxMessage>) {
        prefs.edit()
            .putString(KEY_MESSAGES, cipher.encrypt(json.encodeToString(messages)))
            .apply()
    }

    private companion object {
        const val KEY_MESSAGES = "messages"
        const val ENCRYPTED_PREFIX = "enc:v1:"
    }
}

private class LocalOutboxCipher {
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return "$PREFIX${cipher.iv.base64()}:${cipher.doFinal(value.toByteArray(Charsets.UTF_8)).base64()}"
    }

    fun decrypt(value: String): String {
        val parts = value.removePrefix(PREFIX).split(":", limit = 2)
        require(parts.size == 2) { "Invalid encrypted outbox payload." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_BITS, Base64.decode(parts[0], Base64.NO_WRAP))
        )
        return cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun ByteArray.base64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "alhasanah_chat_outbox_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val PREFIX = "enc:v1:"
    }
}
