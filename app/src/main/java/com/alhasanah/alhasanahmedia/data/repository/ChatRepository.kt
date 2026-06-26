package com.alhasanah.alhasanahmedia.data.repository

import com.alhasanah.alhasanahmedia.data.model.AlumniDataDto
import com.alhasanah.alhasanahmedia.data.model.ChatConversationDto
import com.alhasanah.alhasanahmedia.data.model.ChatConversationItem
import com.alhasanah.alhasanahmedia.data.model.ChatDetail
import com.alhasanah.alhasanahmedia.data.model.ChatMessageDto
import com.alhasanah.alhasanahmedia.data.model.ChatMessageItem
import com.alhasanah.alhasanahmedia.data.model.ChatParticipantDto
import com.alhasanah.alhasanahmedia.data.model.ChatPresenceDto
import com.alhasanah.alhasanahmedia.data.model.ChatDeviceKeyDto
import com.alhasanah.alhasanahmedia.data.model.ChatKeyBackupDto
import com.alhasanah.alhasanahmedia.data.model.ChatMessageCiphertextDto
import com.alhasanah.alhasanahmedia.data.model.CreateChatConversationDto
import com.alhasanah.alhasanahmedia.data.model.CreateChatMessageDto
import com.alhasanah.alhasanahmedia.data.model.CreateChatMessageCiphertextDto
import com.alhasanah.alhasanahmedia.data.model.CreateChatParticipantDto
import com.alhasanah.alhasanahmedia.data.model.CreateChatBlockDto
import com.alhasanah.alhasanahmedia.data.model.CreateChatMessageReportDto
import com.alhasanah.alhasanahmedia.data.model.UpdateChatParticipantReadDto
import com.alhasanah.alhasanahmedia.data.model.UpdateChatParticipantArchiveDto
import com.alhasanah.alhasanahmedia.data.model.UpdateChatParticipantMuteDto
import com.alhasanah.alhasanahmedia.data.model.UpdateChatMessageStatusDto
import com.alhasanah.alhasanahmedia.data.model.UpsertChatDeviceKeyDto
import com.alhasanah.alhasanahmedia.data.model.UpsertChatKeyBackupDto
import com.alhasanah.alhasanahmedia.data.model.UpsertChatPresenceDto
import com.alhasanah.alhasanahmedia.data.model.UpdateChatDeviceKeyRevocationDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import java.time.Instant

interface ChatRepository {
    suspend fun getCachedConversations(userId: String): List<ChatConversationItem>?
    suspend fun getConversations(userId: String): List<ChatConversationItem>
    suspend fun getCachedMessages(userId: String, conversationId: String): ChatDetail?
    suspend fun getMessages(userId: String, conversationId: String, before: String? = null, limit: Int = 50): ChatDetail
    suspend fun getOrCreateDirectConversation(userId: String, otherUserId: String): String
    suspend fun sendMessage(userId: String, conversationId: String, content: String): ChatMessageDto
    suspend fun markRead(userId: String, conversationId: String)
    suspend fun archiveConversation(userId: String, conversationId: String, archived: Boolean)
    suspend fun muteConversation(userId: String, conversationId: String, muted: Boolean)
    suspend fun deleteOwnMessage(userId: String, messageId: String)
    suspend fun reportMessage(userId: String, messageId: String, conversationId: String, reason: String, note: String?)
    suspend fun blockUser(userId: String, blockedUserId: String)
    suspend fun getPresence(userIds: List<String>): Map<String, ChatPresenceDto>
    suspend fun setPresence(userId: String, isOnline: Boolean)
    suspend fun getDeviceKeys(userId: String): List<ChatDeviceKeyDto>
    suspend fun revokeDeviceKey(userId: String, deviceId: String)
    suspend fun revokeCurrentDeviceKey(userId: String)
    suspend fun createEncryptedKeyBackup(userId: String, passphrase: CharArray)
    suspend fun restoreEncryptedKeyBackup(userId: String, passphrase: CharArray)
}

class ChatRepositoryImpl(
    private val postgrest: Postgrest,
    private val cacheStore: AlumniLocalCacheStore,
    private val e2eeCrypto: ChatE2eeCrypto
) : ChatRepository {

    override suspend fun getCachedConversations(userId: String): List<ChatConversationItem>? =
        cacheStore.getChatConversations(userId)

    override suspend fun getConversations(userId: String): List<ChatConversationItem> {
        ensureDeviceKey(userId)
        val myParticipants = postgrest.from("chat_participants").select {
            filter { eq("user_id", userId) }
        }.decodeList<ChatParticipantDto>()

        val visibleParticipants = myParticipants.filter { it.archivedAt == null }
        val conversationIds = visibleParticipants.map { it.conversationId }.distinct()
        if (conversationIds.isEmpty()) return emptyList()

        val conversations = postgrest.from("chat_conversations").select {
            filter { isIn("id", conversationIds) }
            order("updated_at", Order.DESCENDING)
        }.decodeList<ChatConversationDto>()

        val participants = postgrest.from("chat_participants").select {
            filter { isIn("conversation_id", conversationIds) }
        }.decodeList<ChatParticipantDto>()

        val alumni = loadAlumni(participants.map { it.userId })
        val unreadCounts = loadUnreadCounts(userId, visibleParticipants)

        return conversations.map { conversation ->
            val conversationParticipants = participants.filter { it.conversationId == conversation.id }
            val members = participants
                .filter { it.conversationId == conversation.id }
                .mapNotNull { alumni[it.userId] }
            val myParticipant = conversationParticipants.firstOrNull { it.userId == userId }
            ChatConversationItem(
                conversation = conversation,
                participants = members,
                otherParticipant = members.firstOrNull { it.id != userId },
                unreadCount = unreadCounts[conversation.id] ?: 0,
                myParticipant = myParticipant,
                otherLastReadAt = conversationParticipants
                    .filter { it.userId != userId }
                    .mapNotNull { it.lastReadAt }
                    .maxOrNull()
            )
        }.also { cacheStore.saveChatConversations(userId, it) }
    }

    override suspend fun getCachedMessages(userId: String, conversationId: String): ChatDetail? =
        cacheStore.getChatDetail(userId, conversationId)

    override suspend fun getMessages(
        userId: String,
        conversationId: String,
        before: String?,
        limit: Int
    ): ChatDetail {
        ensureDeviceKey(userId)
        val conversations = getConversations(userId)
        val conversation = conversations.first { it.conversation.id == conversationId }
        val messages = postgrest.from("chat_messages").select {
            filter {
                eq("conversation_id", conversationId)
                neq("status", "deleted")
                if (before != null) lt("created_at", before)
            }
            order("created_at", Order.DESCENDING)
            limit(limit.toLong())
        }.decodeList<ChatMessageDto>().asReversed()

        val decryptedByMessageId = decryptMessagesForCurrentDevice(userId, messages)
        val alumni = loadAlumni(messages.map { it.senderId } + conversation.participants.map { it.id })
        return ChatDetail(
            conversation = conversation,
            messages = messages.map { message ->
                ChatMessageItem(
                    message = message,
                    sender = alumni[message.senderId],
                    isMine = message.senderId == userId,
                    decryptedContent = decryptedByMessageId[message.id],
                    decryptError = message.encryptionScheme != LEGACY_ENCRYPTION && decryptedByMessageId[message.id] == null
                )
            }
        ).also {
            if (before == null) cacheStore.saveChatDetail(userId, it.withoutDecryptedPlaintext())
        }
    }

    override suspend fun getOrCreateDirectConversation(userId: String, otherUserId: String): String {
        val myParticipants = postgrest.from("chat_participants").select {
            filter { eq("user_id", userId) }
        }.decodeList<ChatParticipantDto>()
        val myConversationIds = myParticipants.map { it.conversationId }
        if (myConversationIds.isNotEmpty()) {
            val shared = postgrest.from("chat_participants").select {
                filter {
                    eq("user_id", otherUserId)
                    isIn("conversation_id", myConversationIds)
                }
            }.decodeList<ChatParticipantDto>()
            val sharedIds = shared.map { it.conversationId }.toSet()
            if (sharedIds.isNotEmpty()) {
                val direct = postgrest.from("chat_conversations").select {
                    filter {
                        isIn("id", sharedIds.toList())
                        eq("type", "direct")
                    }
                    limit(1)
                }.decodeList<ChatConversationDto>().firstOrNull()
                if (direct != null) return direct.id
            }
        }

        val conversation = postgrest.from("chat_conversations").insert(
            CreateChatConversationDto(createdBy = userId)
        ) {
            select()
        }.decodeList<ChatConversationDto>().first()

        postgrest.from("chat_participants").insert(
            listOf(
                CreateChatParticipantDto(conversationId = conversation.id, userId = userId),
                CreateChatParticipantDto(conversationId = conversation.id, userId = otherUserId)
            )
        )
        return conversation.id
    }

    override suspend fun sendMessage(userId: String, conversationId: String, content: String): ChatMessageDto {
        val senderIdentity = ensureDeviceKey(userId)
        val participantRows = postgrest.from("chat_participants").select {
            filter { eq("conversation_id", conversationId) }
        }.decodeList<ChatParticipantDto>()
        val participantIds = participantRows.map { it.userId }.distinct()
        val recipientDevices = loadActiveDeviceKeys(participantIds)
        require(recipientDevices.isNotEmpty()) { "Kunci E2EE percakapan belum tersedia." }
        require(recipientDevices.any { it.userId == userId && it.deviceId == senderIdentity.deviceId }) {
            "Kunci E2EE perangkat pengirim belum siap."
        }

        val message = postgrest.from("chat_messages").insert(
            CreateChatMessageDto(
                conversationId = conversationId,
                senderId = userId,
                encryptionScheme = E2EE_ENCRYPTION,
                e2eeVersion = 1
            )
        ) {
            select()
        }.decodeList<ChatMessageDto>().first()

        val ciphertexts = recipientDevices.map { recipientDevice ->
            val encrypted = e2eeCrypto.encryptForDevice(userId, recipientDevice, content.trim())
            CreateChatMessageCiphertextDto(
                messageId = message.id,
                conversationId = conversationId,
                recipientUserId = recipientDevice.userId,
                recipientDeviceId = recipientDevice.deviceId,
                senderDeviceId = senderIdentity.deviceId,
                ciphertext = encrypted.ciphertext,
                nonce = encrypted.nonce
            )
        }
        postgrest.from("chat_message_device_ciphertexts").insert(ciphertexts)
        return message.copy(content = content.trim())
    }

    override suspend fun markRead(userId: String, conversationId: String) {
        postgrest.from("chat_participants").update(
            UpdateChatParticipantReadDto(Instant.now().toString())
        ) {
            filter {
                eq("conversation_id", conversationId)
                eq("user_id", userId)
            }
        }
    }

    override suspend fun archiveConversation(userId: String, conversationId: String, archived: Boolean) {
        postgrest.from("chat_participants").update(
            UpdateChatParticipantArchiveDto(if (archived) Instant.now().toString() else null)
        ) {
            filter {
                eq("conversation_id", conversationId)
                eq("user_id", userId)
            }
        }
    }

    override suspend fun muteConversation(userId: String, conversationId: String, muted: Boolean) {
        val until = if (muted) Instant.now().plusSeconds(365L * 24L * 60L * 60L).toString() else null
        postgrest.from("chat_participants").update(UpdateChatParticipantMuteDto(until)) {
            filter {
                eq("conversation_id", conversationId)
                eq("user_id", userId)
            }
        }
    }

    override suspend fun deleteOwnMessage(userId: String, messageId: String) {
        postgrest.from("chat_messages").update(
            UpdateChatMessageStatusDto(
                status = "deleted",
                deletedAt = Instant.now().toString()
            )
        ) {
            filter {
                eq("id", messageId)
                eq("sender_id", userId)
            }
        }
    }

    override suspend fun reportMessage(
        userId: String,
        messageId: String,
        conversationId: String,
        reason: String,
        note: String?
    ) {
        postgrest.from("chat_message_reports").insert(
            CreateChatMessageReportDto(
                messageId = messageId,
                conversationId = conversationId,
                reporterId = userId,
                reason = reason,
                note = null
            )
        )
    }

    override suspend fun blockUser(userId: String, blockedUserId: String) {
        postgrest.from("chat_blocks").insert(
            CreateChatBlockDto(
                blockerId = userId,
                blockedId = blockedUserId
            )
        )
    }

    override suspend fun getPresence(userIds: List<String>): Map<String, ChatPresenceDto> {
        val ids = userIds.distinct().filter { it.isNotBlank() }
        if (ids.isEmpty()) return emptyMap()
        return postgrest.from("chat_user_presence").select {
            filter { isIn("user_id", ids) }
        }.decodeList<ChatPresenceDto>().associateBy { it.userId }
    }

    override suspend fun setPresence(userId: String, isOnline: Boolean) {
        postgrest["chat_user_presence"].upsert(
            listOf(
                UpsertChatPresenceDto(
                    userId = userId,
                    isOnline = isOnline,
                    lastSeenAt = Instant.now().toString()
                )
            )
        )
    }

    override suspend fun getDeviceKeys(userId: String): List<ChatDeviceKeyDto> {
        return postgrest.from("chat_device_keys").select {
            filter { eq("user_id", userId) }
            order("last_seen_at", Order.DESCENDING)
        }.decodeList<ChatDeviceKeyDto>()
    }

    override suspend fun revokeDeviceKey(userId: String, deviceId: String) {
        postgrest.from("chat_device_keys").update(
            UpdateChatDeviceKeyRevocationDto(Instant.now().toString())
        ) {
            filter {
                eq("user_id", userId)
                eq("device_id", deviceId)
            }
        }
        if (deviceId == e2eeCrypto.deviceId(userId)) {
            e2eeCrypto.forgetIdentity(userId, deviceId)
        }
    }

    override suspend fun revokeCurrentDeviceKey(userId: String) {
        revokeDeviceKey(userId, e2eeCrypto.deviceId(userId))
    }

    override suspend fun createEncryptedKeyBackup(userId: String, passphrase: CharArray) {
        require(passphrase.size >= 12) { "Passphrase backup minimal 12 karakter." }
        val backup = e2eeCrypto.createEncryptedPrivateKeyBackup(userId, passphrase)
        postgrest["chat_key_backups"].upsert(
            listOf(
                UpsertChatKeyBackupDto(
                    userId = userId,
                    deviceId = backup.deviceId,
                    encryptedPrivateKey = backup.encryptedPrivateKey,
                    salt = backup.salt,
                    nonce = backup.nonce,
                    kdfIterations = backup.iterations,
                    updatedAt = Instant.now().toString()
                )
            )
        ) {
            onConflict = "user_id,device_id"
        }
    }

    override suspend fun restoreEncryptedKeyBackup(userId: String, passphrase: CharArray) {
        require(passphrase.size >= 12) { "Passphrase backup minimal 12 karakter." }
        val backup = postgrest.from("chat_key_backups").select {
            filter { eq("user_id", userId) }
            order("updated_at", Order.DESCENDING)
            limit(1)
        }.decodeList<ChatKeyBackupDto>().firstOrNull()
            ?: error("Backup kunci E2EE tidak ditemukan.")

        val deviceKey = postgrest.from("chat_device_keys").select {
            filter {
                eq("user_id", userId)
                eq("device_id", backup.deviceId)
                exact("revoked_at", null)
            }
            limit(1)
        }.decodeList<ChatDeviceKeyDto>().firstOrNull()
            ?: error("Device key backup sudah dicabut atau tidak ditemukan.")

        e2eeCrypto.restoreEncryptedPrivateKeyBackup(
            userId = userId,
            deviceId = backup.deviceId,
            publicKey = deviceKey.publicKey,
            encryptedPrivateKey = backup.encryptedPrivateKey,
            salt = backup.salt,
            nonce = backup.nonce,
            iterations = backup.kdfIterations,
            passphrase = passphrase
        )
        ensureDeviceKey(userId)
    }

    private suspend fun loadAlumni(userIds: List<String>): Map<String, AlumniDataDto> {
        val ids = userIds.distinct().filter { it.isNotBlank() }
        if (ids.isEmpty()) return emptyMap()
        return postgrest.from("alumni_data").select {
            filter { isIn("id", ids) }
        }.decodeList<AlumniDataDto>().associateBy { it.id }
    }

    private suspend fun ensureDeviceKey(userId: String): LocalChatDeviceIdentity {
        val identity = e2eeCrypto.getOrCreateIdentity(userId)
        postgrest["chat_device_keys"].upsert(
            listOf(
                UpsertChatDeviceKeyDto(
                    userId = identity.userId,
                    deviceId = identity.deviceId,
                    deviceName = identity.deviceName,
                    publicKey = identity.publicKey,
                    lastSeenAt = Instant.now().toString()
                )
            )
        ) {
            onConflict = "user_id,device_id"
        }
        return identity
    }

    private suspend fun loadActiveDeviceKeys(userIds: List<String>): List<ChatDeviceKeyDto> {
        val ids = userIds.distinct().filter { it.isNotBlank() }
        if (ids.isEmpty()) return emptyList()
        return postgrest.from("chat_device_keys").select {
            filter {
                isIn("user_id", ids)
                exact("revoked_at", null)
            }
        }.decodeList<ChatDeviceKeyDto>()
    }

    private suspend fun decryptMessagesForCurrentDevice(
        userId: String,
        messages: List<ChatMessageDto>
    ): Map<String, String> {
        val encryptedMessages = messages.filter { it.encryptionScheme != LEGACY_ENCRYPTION }
        if (encryptedMessages.isEmpty()) return emptyMap()
        val deviceId = e2eeCrypto.deviceId(userId)
        val messageIds = encryptedMessages.map { it.id }
        val ciphertexts = postgrest.from("chat_message_device_ciphertexts").select {
            filter {
                isIn("message_id", messageIds)
                eq("recipient_user_id", userId)
                eq("recipient_device_id", deviceId)
            }
        }.decodeList<ChatMessageCiphertextDto>()
        if (ciphertexts.isEmpty()) return emptyMap()

        val messageById = encryptedMessages.associateBy { it.id }
        val senderKeys = loadActiveDeviceKeys(encryptedMessages.map { it.senderId })
            .associateBy { "${it.userId}:${it.deviceId}" }
        return ciphertexts.mapNotNull { encrypted ->
            val message = messageById[encrypted.messageId] ?: return@mapNotNull null
            val senderKey = senderKeys["${message.senderId}:${encrypted.senderDeviceId}"] ?: return@mapNotNull null
            val plain = runCatching {
                e2eeCrypto.decryptFromDevice(
                    recipientUserId = userId,
                    recipientDeviceId = deviceId,
                    senderDeviceId = encrypted.senderDeviceId,
                    senderPublicKey = senderKey.publicKey,
                    ciphertext = encrypted.ciphertext,
                    nonce = encrypted.nonce
                )
            }.getOrNull() ?: return@mapNotNull null
            encrypted.messageId to plain
        }.toMap()
    }

    private fun ChatDetail.withoutDecryptedPlaintext(): ChatDetail =
        copy(messages = messages.map { it.copy(decryptedContent = null, decryptError = false) })

    private companion object {
        const val LEGACY_ENCRYPTION = "legacy_plaintext"
        const val E2EE_ENCRYPTION = "e2ee_v1"
        const val ENCRYPTED_PLACEHOLDER = "Pesan terenkripsi"
    }

    private suspend fun loadUnreadCounts(
        userId: String,
        myParticipants: List<ChatParticipantDto>
    ): Map<String, Int> {
        val conversationIds = myParticipants.map { it.conversationId }
        if (conversationIds.isEmpty()) return emptyMap()
        val lastReadByConversation = myParticipants.associate { it.conversationId to it.lastReadAt }
        val incomingMessages = postgrest.from("chat_messages").select {
            filter {
                isIn("conversation_id", conversationIds)
                neq("sender_id", userId)
                neq("status", "deleted")
            }
        }.decodeList<ChatMessageDto>()

        return incomingMessages
            .filter { message ->
                val lastRead = lastReadByConversation[message.conversationId]
                lastRead == null || message.createdAt > lastRead
            }
            .groupingBy { it.conversationId }
            .eachCount()
    }
}
