package com.alhasanah.alhasanahmedia.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alhasanah.alhasanahmedia.data.local.AlumniCacheDao
import com.alhasanah.alhasanahmedia.data.local.AlumniCacheEntity
import com.alhasanah.alhasanahmedia.data.model.AlumniAccess
import com.alhasanah.alhasanahmedia.data.model.AlumniDirectoryItem
import com.alhasanah.alhasanahmedia.data.model.AlumniProfile
import com.alhasanah.alhasanahmedia.data.model.AlumniRecommendationItem
import com.alhasanah.alhasanahmedia.data.model.ChatConversationItem
import com.alhasanah.alhasanahmedia.data.model.ChatDetail
import com.alhasanah.alhasanahmedia.data.model.ForumThreadItem
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.alumniCacheDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "alumni_local_cache"
)

class AlumniLocalCacheStore(
    private val context: Context,
    private val dao: AlumniCacheDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val chatCipher = AlumniChatCacheCipher()

    suspend fun getAccess(userId: String): AlumniAccess? =
        read(key("access", userId), AlumniAccess.serializer())

    suspend fun saveAccess(access: AlumniAccess) {
        write(key("access", access.userId), "access", AlumniAccess.serializer(), access, ACCESS_TTL_MS)
        markSynced("access", access.userId)
    }

    suspend fun getDirectory(): List<AlumniDirectoryItem>? =
        readList(key("directory", "all"), AlumniDirectoryItem.serializer())

    suspend fun saveDirectory(items: List<AlumniDirectoryItem>) {
        writeList(key("directory", "all"), "directory", AlumniDirectoryItem.serializer(), items, DIRECTORY_TTL_MS)
        markSynced("directory", "all")
    }

    suspend fun getRecommendations(userId: String): List<AlumniRecommendationItem>? =
        readList(key("recommendations", userId), AlumniRecommendationItem.serializer())

    suspend fun saveRecommendations(userId: String, items: List<AlumniRecommendationItem>) {
        writeList(
            key("recommendations", userId),
            "recommendations",
            AlumniRecommendationItem.serializer(),
            items,
            DIRECTORY_TTL_MS
        )
        markSynced("recommendations", userId)
    }

    suspend fun getForumThreads(userId: String): List<ForumThreadItem>? =
        readList(key("forum_threads", userId), ForumThreadItem.serializer())

    suspend fun saveForumThreads(userId: String, items: List<ForumThreadItem>) {
        writeList(key("forum_threads", userId), "forum_threads", ForumThreadItem.serializer(), items, FORUM_TTL_MS)
        markSynced("forum_threads", userId)
    }

    suspend fun getProfile(viewerId: String, alumniId: String): AlumniProfile? =
        read(key("profile", "$viewerId:$alumniId"), AlumniProfile.serializer())

    suspend fun saveProfile(viewerId: String, profile: AlumniProfile) {
        write(
            key("profile", "$viewerId:${profile.alumni.id}"),
            "profile",
            AlumniProfile.serializer(),
            profile,
            DIRECTORY_TTL_MS
        )
        markSynced("profile", profile.alumni.id)
    }

    suspend fun getChatConversations(userId: String): List<ChatConversationItem>? =
        readEncryptedList(key("chat_conversations", userId), ChatConversationItem.serializer())

    suspend fun saveChatConversations(userId: String, items: List<ChatConversationItem>) {
        writeEncryptedList(
            key("chat_conversations", userId),
            "chat_conversations",
            ChatConversationItem.serializer(),
            items,
            CHAT_TTL_MS
        )
        markSynced("chat_conversations", userId)
    }

    suspend fun getChatDetail(userId: String, conversationId: String): ChatDetail? =
        readEncrypted(key("chat_detail", "$userId:$conversationId"), ChatDetail.serializer())

    suspend fun saveChatDetail(userId: String, detail: ChatDetail) {
        writeEncrypted(
            key("chat_detail", "${userId}:${detail.conversation.conversation.id}"),
            "chat_detail",
            ChatDetail.serializer(),
            detail,
            CHAT_TTL_MS
        )
        markSynced("chat_detail", detail.conversation.conversation.id)
    }

    private suspend fun <T> read(
        key: String,
        serializer: kotlinx.serialization.KSerializer<T>
    ): T? {
        val entity = dao.get(key) ?: return null
        val expiresAt = entity.expiresAt
        if (expiresAt != null && expiresAt < System.currentTimeMillis()) {
            dao.delete(key)
            return null
        }
        val value = entity.json
        return runCatching { json.decodeFromString(serializer, value) }.getOrNull()
    }

    private suspend fun <T> write(
        key: String,
        domain: String,
        serializer: kotlinx.serialization.KSerializer<T>,
        value: T,
        ttlMillis: Long? = null
    ) {
        val encoded = json.encodeToString(serializer, value)
        writeRaw(key, domain, encoded, ttlMillis)
    }

    private suspend fun <T> readEncrypted(
        key: String,
        serializer: kotlinx.serialization.KSerializer<T>
    ): T? {
        val entity = dao.get(key) ?: return null
        val expiresAt = entity.expiresAt
        if (expiresAt != null && expiresAt < System.currentTimeMillis()) {
            dao.delete(key)
            return null
        }
        return runCatching {
            if (entity.json.startsWith(AlumniChatCacheCipher.PREFIX)) {
                json.decodeFromString(serializer, chatCipher.decrypt(entity.json))
            } else {
                json.decodeFromString(serializer, entity.json).also { decoded ->
                    dao.upsert(entity.copy(json = chatCipher.encrypt(json.encodeToString(serializer, decoded))))
                }
            }
        }.getOrNull()
    }

    private suspend fun <T> writeEncrypted(
        key: String,
        domain: String,
        serializer: kotlinx.serialization.KSerializer<T>,
        value: T,
        ttlMillis: Long? = null
    ) {
        val encoded = chatCipher.encrypt(json.encodeToString(serializer, value))
        writeRaw(key, domain, encoded, ttlMillis)
    }

    private suspend fun writeRaw(
        key: String,
        domain: String,
        encoded: String,
        ttlMillis: Long? = null
    ) {
        val now = System.currentTimeMillis()
        dao.upsert(
            AlumniCacheEntity(
                cacheKey = key,
                domain = domain,
                json = encoded,
                updatedAt = now,
                expiresAt = ttlMillis?.let { now + it }
            )
        )
    }

    private suspend fun <T> readList(
        key: String,
        serializer: kotlinx.serialization.KSerializer<T>
    ): List<T>? {
        val entity = dao.get(key) ?: return null
        val expiresAt = entity.expiresAt
        if (expiresAt != null && expiresAt < System.currentTimeMillis()) {
            dao.delete(key)
            return null
        }
        val value = entity.json
        return runCatching {
            json.decodeFromString(ListSerializer(serializer), value)
        }.getOrNull()
    }

    private suspend fun <T> writeList(
        key: String,
        domain: String,
        serializer: kotlinx.serialization.KSerializer<T>,
        value: List<T>,
        ttlMillis: Long? = null
    ) {
        val encoded = json.encodeToString(ListSerializer(serializer), value)
        writeRaw(key, domain, encoded, ttlMillis)
    }

    private suspend fun <T> readEncryptedList(
        key: String,
        serializer: kotlinx.serialization.KSerializer<T>
    ): List<T>? {
        val entity = dao.get(key) ?: return null
        val expiresAt = entity.expiresAt
        if (expiresAt != null && expiresAt < System.currentTimeMillis()) {
            dao.delete(key)
            return null
        }
        val listSerializer = ListSerializer(serializer)
        return runCatching {
            if (entity.json.startsWith(AlumniChatCacheCipher.PREFIX)) {
                json.decodeFromString(listSerializer, chatCipher.decrypt(entity.json))
            } else {
                json.decodeFromString(listSerializer, entity.json).also { decoded ->
                    dao.upsert(entity.copy(json = chatCipher.encrypt(json.encodeToString(listSerializer, decoded))))
                }
            }
        }.getOrNull()
    }

    private suspend fun <T> writeEncryptedList(
        key: String,
        domain: String,
        serializer: kotlinx.serialization.KSerializer<T>,
        value: List<T>,
        ttlMillis: Long? = null
    ) {
        val encoded = chatCipher.encrypt(json.encodeToString(ListSerializer(serializer), value))
        writeRaw(key, domain, encoded, ttlMillis)
    }

    private suspend fun markSynced(prefix: String, id: String) {
        val stampKey = longPreferencesKey(key("synced_at_$prefix", id))
        context.alumniCacheDataStore.edit { it[stampKey] = System.currentTimeMillis() }
        dao.deleteExpired(System.currentTimeMillis())
    }

    private fun key(prefix: String, id: String): String =
        "${prefix}_${id.replace(Regex("[^A-Za-z0-9_-]"), "_")}"

    private companion object {
        const val ACCESS_TTL_MS = 15 * 60 * 1000L
        const val DIRECTORY_TTL_MS = 5 * 60 * 1000L
        const val FORUM_TTL_MS = 45 * 60 * 1000L
        const val CHAT_TTL_MS = 24 * 60 * 60 * 1000L
    }
}

private class AlumniChatCacheCipher {
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return "$PREFIX${cipher.iv.base64()}:${cipher.doFinal(plainText.toByteArray(Charsets.UTF_8)).base64()}"
    }

    fun decrypt(value: String): String {
        require(value.startsWith(PREFIX)) { "Unsupported encrypted alumni chat cache format." }
        val parts = value.removePrefix(PREFIX).split(":", limit = 2)
        require(parts.size == 2) { "Invalid encrypted alumni chat cache payload." }
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
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun ByteArray.base64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    companion object {
        const val PREFIX = "enc:v1:"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "alhasanah_alumni_chat_cache_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }
}
