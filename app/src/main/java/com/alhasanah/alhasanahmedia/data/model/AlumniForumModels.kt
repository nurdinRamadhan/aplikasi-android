package com.alhasanah.alhasanahmedia.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileAccessDto(
    @SerialName("id")
    val id: String,
    @SerialName("role")
    val role: String? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("foto_url")
    val fotoUrl: String? = null
)

@Serializable
data class AlumniDataDto(
    @SerialName("id")
    val id: String,
    @SerialName("full_name")
    val fullName: String,
    @SerialName("tahun_lulus")
    val tahunLulus: Int,
    @SerialName("no_wa")
    val noWa: String? = null,
    @SerialName("profesi_sekarang")
    val profesiSekarang: String? = null,
    @SerialName("instansi_kerja")
    val instansiKerja: String? = null,
    @SerialName("alamat_domisili")
    val alamatDomisili: String? = null,
    @SerialName("province_code")
    val provinceCode: String? = null,
    @SerialName("province_name")
    val provinceName: String? = null,
    @SerialName("regency_code")
    val regencyCode: String? = null,
    @SerialName("regency_name")
    val regencyName: String? = null,
    @SerialName("district_code")
    val districtCode: String? = null,
    @SerialName("district_name")
    val districtName: String? = null,
    @SerialName("village_code")
    val villageCode: String? = null,
    @SerialName("village_name")
    val villageName: String? = null,
    @SerialName("postal_code")
    val postalCode: String? = null,
    @SerialName("address_detail")
    val addressDetail: String? = null,
    @SerialName("bio")
    val bio: String? = null,
    @SerialName("avatar_storage_path")
    val avatarStoragePath: String? = null,
    @SerialName("show_whatsapp")
    val showWhatsapp: Boolean = false,
    @SerialName("show_profession")
    val showProfession: Boolean = true,
    @SerialName("show_location")
    val showLocation: Boolean = true,
    @SerialName("forum_notify_replies")
    val forumNotifyReplies: Boolean = true,
    @SerialName("forum_notify_reactions")
    val forumNotifyReactions: Boolean = true
)

@Serializable
data class UpdateAlumniProfileDto(
    @SerialName("full_name")
    val fullName: String,
    @SerialName("no_wa")
    val noWa: String? = null,
    @SerialName("profesi_sekarang")
    val profesiSekarang: String? = null,
    @SerialName("instansi_kerja")
    val instansiKerja: String? = null,
    @SerialName("alamat_domisili")
    val alamatDomisili: String? = null,
    @SerialName("province_code")
    val provinceCode: String? = null,
    @SerialName("province_name")
    val provinceName: String? = null,
    @SerialName("regency_code")
    val regencyCode: String? = null,
    @SerialName("regency_name")
    val regencyName: String? = null,
    @SerialName("district_code")
    val districtCode: String? = null,
    @SerialName("district_name")
    val districtName: String? = null,
    @SerialName("village_code")
    val villageCode: String? = null,
    @SerialName("village_name")
    val villageName: String? = null,
    @SerialName("postal_code")
    val postalCode: String? = null,
    @SerialName("address_detail")
    val addressDetail: String? = null,
    @SerialName("bio")
    val bio: String? = null,
    @SerialName("avatar_storage_path")
    val avatarStoragePath: String? = null,
    @SerialName("show_whatsapp")
    val showWhatsapp: Boolean = false,
    @SerialName("show_profession")
    val showProfession: Boolean = true,
    @SerialName("show_location")
    val showLocation: Boolean = true,
    @SerialName("forum_notify_replies")
    val forumNotifyReplies: Boolean = true,
    @SerialName("forum_notify_reactions")
    val forumNotifyReactions: Boolean = true
)

@Serializable
data class ForumThreadDto(
    @SerialName("id")
    val id: String,
    @SerialName("author_id")
    val authorId: String,
    @SerialName("content")
    val content: String,
    @SerialName("status")
    val status: String = "published",
    @SerialName("is_pinned")
    val isPinned: Boolean = false,
    @SerialName("is_locked")
    val isLocked: Boolean = false,
    @SerialName("comment_count")
    val commentCount: Int = 0,
    @SerialName("reaction_count")
    val reactionCount: Int = 0,
    @SerialName("repost_of_thread_id")
    val repostOfThreadId: String? = null,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class CreateForumThreadDto(
    @SerialName("author_id")
    val authorId: String,
    @SerialName("content")
    val content: String,
    @SerialName("repost_of_thread_id")
    val repostOfThreadId: String? = null
)

@Serializable
data class UpdateForumThreadDto(
    @SerialName("content")
    val content: String,
    @SerialName("edited_at")
    val editedAt: String
)

@Serializable
data class UpdateForumThreadModerationDto(
    @SerialName("status")
    val status: String? = null,
    @SerialName("is_pinned")
    val isPinned: Boolean? = null,
    @SerialName("is_locked")
    val isLocked: Boolean? = null
)

@Serializable
data class UpdateForumThreadStatusDto(
    @SerialName("status")
    val status: String
)

@Serializable
data class UpdateForumThreadPinnedDto(
    @SerialName("is_pinned")
    val isPinned: Boolean
)

@Serializable
data class UpdateForumThreadLockedDto(
    @SerialName("is_locked")
    val isLocked: Boolean
)

@Serializable
data class ForumCommentDto(
    @SerialName("id")
    val id: String,
    @SerialName("thread_id")
    val threadId: String,
    @SerialName("author_id")
    val authorId: String,
    @SerialName("content")
    val content: String,
    @SerialName("reaction_count")
    val reactionCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class CreateForumCommentDto(
    @SerialName("thread_id")
    val threadId: String,
    @SerialName("author_id")
    val authorId: String,
    @SerialName("content")
    val content: String
)

@Serializable
data class UpdateForumCommentDto(
    @SerialName("content")
    val content: String,
    @SerialName("edited_at")
    val editedAt: String
)

@Serializable
data class ForumReactionDto(
    @SerialName("id")
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("thread_id")
    val threadId: String? = null,
    @SerialName("comment_id")
    val commentId: String? = null,
    @SerialName("reaction_type")
    val reactionType: String = "love"
)

@Serializable
data class CreateForumReactionDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("thread_id")
    val threadId: String? = null,
    @SerialName("comment_id")
    val commentId: String? = null,
    @SerialName("reaction_type")
    val reactionType: String = "love"
)

@Serializable
data class ForumAttachmentDto(
    @SerialName("id")
    val id: String,
    @SerialName("thread_id")
    val threadId: String? = null,
    @SerialName("comment_id")
    val commentId: String? = null,
    @SerialName("uploader_id")
    val uploaderId: String,
    @SerialName("storage_bucket")
    val storageBucket: String = "forum-media",
    @SerialName("storage_path")
    val storagePath: String,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("file_size")
    val fileSize: Long? = null,
    @SerialName("alt_text")
    val altText: String? = null
)

@Serializable
data class CreateForumAttachmentDto(
    @SerialName("thread_id")
    val threadId: String? = null,
    @SerialName("comment_id")
    val commentId: String? = null,
    @SerialName("uploader_id")
    val uploaderId: String,
    @SerialName("storage_path")
    val storagePath: String,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("file_size")
    val fileSize: Long? = null,
    @SerialName("alt_text")
    val altText: String? = null
)

@Serializable
data class CreateForumReportDto(
    @SerialName("reporter_id")
    val reporterId: String,
    @SerialName("thread_id")
    val threadId: String? = null,
    @SerialName("comment_id")
    val commentId: String? = null,
    @SerialName("reason")
    val reason: String,
    @SerialName("note")
    val note: String? = null
)

@Serializable
data class AlumniAccess(
    val userId: String,
    val role: String,
    val isActive: Boolean,
    val profileName: String?,
    val alumniData: AlumniDataDto?
) {
    val isAlumni: Boolean = role.equals("alumni", ignoreCase = true)
    val isForumAdmin: Boolean = role.lowercase() in setOf("super_admin", "kesantrian", "rois")
    val canOpenForum: Boolean = isActive && (isAlumni || isForumAdmin)
}

@Serializable
data class AlumniFollowDto(
    @SerialName("follower_id")
    val followerId: String,
    @SerialName("following_id")
    val followingId: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class CreateAlumniFollowDto(
    @SerialName("follower_id")
    val followerId: String,
    @SerialName("following_id")
    val followingId: String
)

@Serializable
data class AlumniFollowStats(
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val followedByMe: Boolean = false
)

@Serializable
data class AlumniRecommendationItem(
    val alumni: AlumniDataDto,
    val avatarUrl: String?,
    val reason: String,
    val followedByMe: Boolean = false
)

@Serializable
data class AlumniFollowUser(
    val alumni: AlumniDataDto,
    val avatarUrl: String?,
    val followedByMe: Boolean = false
)

@Serializable
data class ForumThreadItem(
    val thread: ForumThreadDto,
    val author: AlumniDataDto?,
    val attachments: List<ForumAttachmentItem> = emptyList(),
    val lovedByMe: Boolean = false
)

@Serializable
data class ForumCommentItem(
    val comment: ForumCommentDto,
    val author: AlumniDataDto?,
    val lovedByMe: Boolean = false
)

@Serializable
data class ForumAttachmentItem(
    val attachment: ForumAttachmentDto,
    val signedUrl: String
)

@Serializable
data class ForumThreadDetail(
    val item: ForumThreadItem,
    val comments: List<ForumCommentItem>
)

@Serializable
data class AlumniProfile(
    val alumni: AlumniDataDto,
    val avatarUrl: String?,
    val posts: List<ForumThreadItem>,
    val replies: List<ForumCommentItem> = emptyList(),
    val postCount: Int,
    val commentCount: Int,
    val reactionCount: Int,
    val followStats: AlumniFollowStats = AlumniFollowStats(),
    val isOwnProfile: Boolean
)

@Serializable
data class AlumniDirectoryItem(
    val alumni: AlumniDataDto,
    val avatarUrl: String?
)

@Serializable
data class RegisterAlumniRequest(
    @SerialName("email")
    val email: String,
    @SerialName("password")
    val password: String,
    @SerialName("full_name")
    val fullName: String,
    @SerialName("tahun_lulus")
    val tahunLulus: Int,
    @SerialName("no_wa")
    val noWa: String? = null,
    @SerialName("profesi_sekarang")
    val profesiSekarang: String? = null,
    @SerialName("instansi_kerja")
    val instansiKerja: String? = null,
    @SerialName("alamat_domisili")
    val alamatDomisili: String? = null,
    @SerialName("province_code")
    val provinceCode: String? = null,
    @SerialName("province_name")
    val provinceName: String? = null,
    @SerialName("regency_code")
    val regencyCode: String? = null,
    @SerialName("regency_name")
    val regencyName: String? = null,
    @SerialName("district_code")
    val districtCode: String? = null,
    @SerialName("district_name")
    val districtName: String? = null,
    @SerialName("village_code")
    val villageCode: String? = null,
    @SerialName("village_name")
    val villageName: String? = null,
    @SerialName("postal_code")
    val postalCode: String? = null,
    @SerialName("address_detail")
    val addressDetail: String? = null
)

@Serializable
data class IndonesiaRegionItem(
    @SerialName("code")
    val code: String,
    @SerialName("name")
    val name: String
)

@Serializable
data class IndonesiaRegionResponse(
    @SerialName("data")
    val data: List<IndonesiaRegionItem> = emptyList()
)

@Serializable
data class RegisterAlumniResponse(
    @SerialName("success")
    val success: Boolean = false,
    @SerialName("message")
    val message: String = ""
)

@Serializable
data class RegisterAlumniErrorResponse(
    @SerialName("error")
    val error: String = "Pendaftaran tidak dapat diproses."
)

@Serializable
data class ChatConversationDto(
    @SerialName("id")
    val id: String,
    @SerialName("type")
    val type: String = "direct",
    @SerialName("title")
    val title: String? = null,
    @SerialName("created_by")
    val createdBy: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("last_message_at")
    val lastMessageAt: String? = null,
    @SerialName("last_message_preview")
    val lastMessagePreview: String? = null,
    @SerialName("last_message_sender_id")
    val lastMessageSenderId: String? = null
)

@Serializable
data class CreateChatConversationDto(
    @SerialName("type")
    val type: String = "direct",
    @SerialName("title")
    val title: String? = null,
    @SerialName("created_by")
    val createdBy: String
)

@Serializable
data class ChatParticipantDto(
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("role")
    val role: String = "member",
    @SerialName("joined_at")
    val joinedAt: String,
    @SerialName("last_read_at")
    val lastReadAt: String? = null,
    @SerialName("muted_until")
    val mutedUntil: String? = null,
    @SerialName("archived_at")
    val archivedAt: String? = null
)

@Serializable
data class CreateChatParticipantDto(
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("role")
    val role: String = "member"
)

@Serializable
data class UpdateChatParticipantReadDto(
    @SerialName("last_read_at")
    val lastReadAt: String
)

@Serializable
data class UpdateChatParticipantArchiveDto(
    @SerialName("archived_at")
    val archivedAt: String? = null
)

@Serializable
data class UpdateChatParticipantMuteDto(
    @SerialName("muted_until")
    val mutedUntil: String? = null
)

@Serializable
data class ChatMessageDto(
    @SerialName("id")
    val id: String,
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("sender_id")
    val senderId: String,
    @SerialName("content")
    val content: String? = null,
    @SerialName("message_type")
    val messageType: String = "text",
    @SerialName("status")
    val status: String = "sent",
    @SerialName("reply_to_message_id")
    val replyToMessageId: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("edited_at")
    val editedAt: String? = null,
    @SerialName("deleted_at")
    val deletedAt: String? = null,
    @SerialName("encryption_scheme")
    val encryptionScheme: String = "legacy_plaintext",
    @SerialName("e2ee_version")
    val e2eeVersion: Int = 0
)

@Serializable
data class CreateChatMessageDto(
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("sender_id")
    val senderId: String,
    @SerialName("message_type")
    val messageType: String = "text",
    @SerialName("encryption_scheme")
    val encryptionScheme: String = "legacy_plaintext",
    @SerialName("e2ee_version")
    val e2eeVersion: Int = 0
)

@Serializable
data class ChatDeviceKeyDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("device_name")
    val deviceName: String? = null,
    @SerialName("public_key")
    val publicKey: String,
    @SerialName("key_algorithm")
    val keyAlgorithm: String = "P-256-ECDH-AES-GCM",
    @SerialName("key_version")
    val keyVersion: Int = 1,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("last_seen_at")
    val lastSeenAt: String? = null,
    @SerialName("revoked_at")
    val revokedAt: String? = null
)

@Serializable
data class UpsertChatDeviceKeyDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("device_name")
    val deviceName: String? = null,
    @SerialName("public_key")
    val publicKey: String,
    @SerialName("key_algorithm")
    val keyAlgorithm: String = "P-256-ECDH-AES-GCM",
    @SerialName("key_version")
    val keyVersion: Int = 1,
    @SerialName("last_seen_at")
    val lastSeenAt: String
)

@Serializable
data class UpdateChatDeviceKeyRevocationDto(
    @SerialName("revoked_at")
    val revokedAt: String
)

@Serializable
data class ChatMessageCiphertextDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("message_id")
    val messageId: String,
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("recipient_user_id")
    val recipientUserId: String,
    @SerialName("recipient_device_id")
    val recipientDeviceId: String,
    @SerialName("sender_device_id")
    val senderDeviceId: String,
    @SerialName("ciphertext")
    val ciphertext: String,
    @SerialName("nonce")
    val nonce: String,
    @SerialName("encrypted_message_key")
    val encryptedMessageKey: String? = null,
    @SerialName("key_algorithm")
    val keyAlgorithm: String = "P-256-ECDH-AES-GCM",
    @SerialName("key_version")
    val keyVersion: Int = 1
)

@Serializable
data class CreateChatMessageCiphertextDto(
    @SerialName("message_id")
    val messageId: String,
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("recipient_user_id")
    val recipientUserId: String,
    @SerialName("recipient_device_id")
    val recipientDeviceId: String,
    @SerialName("sender_device_id")
    val senderDeviceId: String,
    @SerialName("ciphertext")
    val ciphertext: String,
    @SerialName("nonce")
    val nonce: String,
    @SerialName("encrypted_message_key")
    val encryptedMessageKey: String? = null,
    @SerialName("key_algorithm")
    val keyAlgorithm: String = "P-256-ECDH-AES-GCM",
    @SerialName("key_version")
    val keyVersion: Int = 1
)

@Serializable
data class UpsertChatKeyBackupDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("encrypted_private_key")
    val encryptedPrivateKey: String,
    @SerialName("salt")
    val salt: String,
    @SerialName("nonce")
    val nonce: String,
    @SerialName("kdf")
    val kdf: String = "PBKDF2WithHmacSHA256",
    @SerialName("kdf_iterations")
    val kdfIterations: Int,
    @SerialName("key_algorithm")
    val keyAlgorithm: String = "P-256-ECDH-AES-GCM",
    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class ChatKeyBackupDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("encrypted_private_key")
    val encryptedPrivateKey: String,
    @SerialName("salt")
    val salt: String,
    @SerialName("nonce")
    val nonce: String,
    @SerialName("kdf")
    val kdf: String = "PBKDF2WithHmacSHA256",
    @SerialName("kdf_iterations")
    val kdfIterations: Int,
    @SerialName("key_algorithm")
    val keyAlgorithm: String = "P-256-ECDH-AES-GCM",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class UpdateChatMessageStatusDto(
    @SerialName("status")
    val status: String,
    @SerialName("deleted_at")
    val deletedAt: String? = null
)

@Serializable
data class CreateChatMessageReportDto(
    @SerialName("message_id")
    val messageId: String,
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("reporter_id")
    val reporterId: String,
    @SerialName("reason")
    val reason: String,
    @SerialName("note")
    val note: String? = null
)

@Serializable
data class CreateChatBlockDto(
    @SerialName("blocker_id")
    val blockerId: String,
    @SerialName("blocked_id")
    val blockedId: String
)

@Serializable
data class ChatPresenceDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("is_online")
    val isOnline: Boolean = false,
    @SerialName("last_seen_at")
    val lastSeenAt: String,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class UpsertChatPresenceDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("is_online")
    val isOnline: Boolean,
    @SerialName("last_seen_at")
    val lastSeenAt: String
)

@Serializable
data class ChatOutboxMessage(
    @SerialName("id")
    val id: String,
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("content")
    val content: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("last_error")
    val lastError: String? = null
)

@Serializable
data class ChatConversationItem(
    val conversation: ChatConversationDto,
    val participants: List<AlumniDataDto>,
    val otherParticipant: AlumniDataDto?,
    val unreadCount: Int,
    val myParticipant: ChatParticipantDto? = null,
    val otherLastReadAt: String? = null
)

@Serializable
data class ChatMessageItem(
    val message: ChatMessageDto,
    val sender: AlumniDataDto?,
    val isMine: Boolean,
    val deliveryState: String = "sent",
    val decryptedContent: String? = null,
    val decryptError: Boolean = false
)

@Serializable
data class ChatDetail(
    val conversation: ChatConversationItem,
    val messages: List<ChatMessageItem>
)

@Serializable
data class ChatTypingPayload(
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("name")
    val name: String,
    @SerialName("is_typing")
    val isTyping: Boolean
)
