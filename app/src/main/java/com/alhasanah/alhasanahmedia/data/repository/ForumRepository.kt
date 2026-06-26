package com.alhasanah.alhasanahmedia.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.alhasanah.alhasanahmedia.data.model.AlumniDataDto
import com.alhasanah.alhasanahmedia.data.model.CreateForumAttachmentDto
import com.alhasanah.alhasanahmedia.data.model.CreateForumCommentDto
import com.alhasanah.alhasanahmedia.data.model.CreateForumReactionDto
import com.alhasanah.alhasanahmedia.data.model.CreateForumReportDto
import com.alhasanah.alhasanahmedia.data.model.CreateForumThreadDto
import com.alhasanah.alhasanahmedia.data.model.ForumAttachmentDto
import com.alhasanah.alhasanahmedia.data.model.ForumAttachmentItem
import com.alhasanah.alhasanahmedia.data.model.ForumCommentDto
import com.alhasanah.alhasanahmedia.data.model.ForumCommentItem
import com.alhasanah.alhasanahmedia.data.model.ForumReactionDto
import com.alhasanah.alhasanahmedia.data.model.ForumThreadDetail
import com.alhasanah.alhasanahmedia.data.model.ForumThreadDto
import com.alhasanah.alhasanahmedia.data.model.ForumThreadItem
import com.alhasanah.alhasanahmedia.data.model.UpdateForumCommentDto
import com.alhasanah.alhasanahmedia.data.model.UpdateForumThreadDto
import com.alhasanah.alhasanahmedia.data.model.UpdateForumThreadLockedDto
import com.alhasanah.alhasanahmedia.data.model.UpdateForumThreadPinnedDto
import com.alhasanah.alhasanahmedia.data.model.UpdateForumThreadStatusDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.Storage
import io.ktor.http.ContentType
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Duration.Companion.hours

interface ForumRepository {
    suspend fun getCachedThreads(userId: String): List<ForumThreadItem>?
    suspend fun getThreads(userId: String): List<ForumThreadItem>
    suspend fun getThreadsByAuthor(userId: String, authorId: String): List<ForumThreadItem>
    suspend fun getThreadDetail(userId: String, threadId: String): ForumThreadDetail
    suspend fun createThread(authorId: String, content: String, imageUri: Uri? = null)
    suspend fun createRepost(authorId: String, sourceThreadId: String, content: String)
    suspend fun updateThread(threadId: String, content: String)
    suspend fun moderateThread(threadId: String, status: String? = null, isPinned: Boolean? = null, isLocked: Boolean? = null)
    suspend fun deleteThread(threadId: String)
    suspend fun createComment(authorId: String, threadId: String, content: String)
    suspend fun updateComment(commentId: String, content: String)
    suspend fun deleteComment(commentId: String)
    suspend fun toggleThreadLove(userId: String, threadId: String, currentlyLoved: Boolean)
    suspend fun toggleCommentLove(userId: String, commentId: String, currentlyLoved: Boolean)
    suspend fun reportThread(reporterId: String, threadId: String, reason: String, note: String?)
    suspend fun reportComment(reporterId: String, commentId: String, reason: String, note: String?)
}

class ForumRepositoryImpl(
    private val context: Context,
    private val postgrest: Postgrest,
    private val storage: Storage,
    private val cacheStore: AlumniLocalCacheStore
) : ForumRepository {

    private val authorCache = mutableMapOf<String, AlumniDataDto>()
    private val signedUrlCache = mutableMapOf<String, CachedSignedUrl>()

    override suspend fun getCachedThreads(userId: String): List<ForumThreadItem>? =
        cacheStore.getForumThreads(userId)

    override suspend fun getThreads(userId: String): List<ForumThreadItem> {
        val threads = postgrest.from("forum_threads").select {
            order("is_pinned", Order.DESCENDING)
            order("created_at", Order.DESCENDING)
            limit(50)
        }.decodeList<ForumThreadDto>()

        if (threads.isEmpty()) return emptyList()

        return coroutineScope {
            val threadIds = threads.map { it.id }
            val authorsDeferred = async { loadAuthors(threads.map { it.authorId }) }
            val attachmentsDeferred = async { loadThreadAttachments(threadIds) }
            val lovedDeferred = async { loadLovedThreadIds(userId, threadIds) }

            val authors = authorsDeferred.await()
            val attachmentsByThread = attachmentsDeferred.await()
            val lovedThreadIds = lovedDeferred.await()

            threads.map { thread ->
                ForumThreadItem(
                    thread = thread,
                    author = authors[thread.authorId],
                    attachments = attachmentsByThread[thread.id].orEmpty(),
                    lovedByMe = thread.id in lovedThreadIds
                )
            }.also { cacheStore.saveForumThreads(userId, it) }
        }
    }

    override suspend fun getThreadsByAuthor(userId: String, authorId: String): List<ForumThreadItem> {
        val threads = postgrest.from("forum_threads").select {
            filter { eq("author_id", authorId) }
            order("created_at", Order.DESCENDING)
            limit(30)
        }.decodeList<ForumThreadDto>()

        if (threads.isEmpty()) return emptyList()

        return coroutineScope {
            val threadIds = threads.map { it.id }
            val authorsDeferred = async { loadAuthors(threads.map { it.authorId }) }
            val attachmentsDeferred = async { loadThreadAttachments(threadIds) }
            val lovedDeferred = async { loadLovedThreadIds(userId, threadIds) }

            val authors = authorsDeferred.await()
            val attachmentsByThread = attachmentsDeferred.await()
            val lovedThreadIds = lovedDeferred.await()

            threads.map { thread ->
                ForumThreadItem(
                    thread = thread,
                    author = authors[thread.authorId],
                    attachments = attachmentsByThread[thread.id].orEmpty(),
                    lovedByMe = thread.id in lovedThreadIds
                )
            }
        }
    }

    override suspend fun getThreadDetail(userId: String, threadId: String): ForumThreadDetail = coroutineScope {
        val threadDeferred = async {
            postgrest.from("forum_threads").select {
                filter { eq("id", threadId) }
                limit(1)
            }.decodeSingle<ForumThreadDto>()
        }
        val commentsDeferred = async {
            postgrest.from("forum_comments").select {
                filter { eq("thread_id", threadId) }
                order("created_at", Order.ASCENDING)
            }.decodeList<ForumCommentDto>()
        }
        val attachmentsDeferred = async { loadThreadAttachments(listOf(threadId)) }
        val lovedThreadDeferred = async { loadLovedThreadIds(userId, listOf(threadId)) }

        val thread = threadDeferred.await()
        val comments = commentsDeferred.await()
        val authorsDeferred = async { loadAuthors((comments.map { it.authorId } + thread.authorId).distinct()) }
        val lovedCommentDeferred = async { loadLovedCommentIds(userId, comments.map { it.id }) }

        val authors = authorsDeferred.await()
        val attachmentsByThread = attachmentsDeferred.await()
        val lovedThreadIds = lovedThreadDeferred.await()
        val lovedCommentIds = lovedCommentDeferred.await()

        ForumThreadDetail(
            item = ForumThreadItem(
                thread = thread,
                author = authors[thread.authorId],
                attachments = attachmentsByThread[thread.id].orEmpty(),
                lovedByMe = thread.id in lovedThreadIds
            ),
            comments = comments.map { comment ->
                ForumCommentItem(
                    comment = comment,
                    author = authors[comment.authorId],
                    lovedByMe = comment.id in lovedCommentIds
                )
            }
        )
    }

    override suspend fun createThread(authorId: String, content: String, imageUri: Uri?) {
        val thread = postgrest.from("forum_threads").insert(
            CreateForumThreadDto(
                authorId = authorId,
                content = content.trim()
            )
        ) {
            select()
        }.decodeList<ForumThreadDto>().first()

        if (imageUri != null) {
            uploadThreadImage(authorId = authorId, threadId = thread.id, uri = imageUri)
        }
    }

    override suspend fun createRepost(authorId: String, sourceThreadId: String, content: String) {
        postgrest.from("forum_threads").insert(
            CreateForumThreadDto(
                authorId = authorId,
                content = content.trim().ifBlank { "Membagikan ulang postingan alumni." },
                repostOfThreadId = sourceThreadId
            )
        )
    }

    override suspend fun updateThread(threadId: String, content: String) {
        postgrest.from("forum_threads").update(
            UpdateForumThreadDto(
                content = content.trim(),
                editedAt = Instant.now().toString()
            )
        ) {
            filter { eq("id", threadId) }
        }
    }

    override suspend fun moderateThread(threadId: String, status: String?, isPinned: Boolean?, isLocked: Boolean?) {
        if (status != null) {
            postgrest.from("forum_threads").update(UpdateForumThreadStatusDto(status)) {
                filter { eq("id", threadId) }
            }
        }
        if (isPinned != null) {
            postgrest.from("forum_threads").update(UpdateForumThreadPinnedDto(isPinned)) {
                filter { eq("id", threadId) }
            }
        }
        if (isLocked != null) {
            postgrest.from("forum_threads").update(UpdateForumThreadLockedDto(isLocked)) {
                filter { eq("id", threadId) }
            }
        }
    }

    override suspend fun deleteThread(threadId: String) {
        postgrest.from("forum_threads").delete {
            filter { eq("id", threadId) }
        }
    }

    override suspend fun createComment(authorId: String, threadId: String, content: String) {
        postgrest.from("forum_comments").insert(
            CreateForumCommentDto(
                threadId = threadId,
                authorId = authorId,
                content = content.trim()
            )
        )
    }

    override suspend fun updateComment(commentId: String, content: String) {
        postgrest.from("forum_comments").update(
            UpdateForumCommentDto(
                content = content.trim(),
                editedAt = Instant.now().toString()
            )
        ) {
            filter { eq("id", commentId) }
        }
    }

    override suspend fun deleteComment(commentId: String) {
        postgrest.from("forum_comments").delete {
            filter { eq("id", commentId) }
        }
    }

    override suspend fun toggleThreadLove(userId: String, threadId: String, currentlyLoved: Boolean) {
        if (currentlyLoved) {
            postgrest.from("forum_reactions").delete {
                filter {
                    eq("user_id", userId)
                    eq("thread_id", threadId)
                    eq("reaction_type", "love")
                }
            }
        } else {
            postgrest.from("forum_reactions").insert(
                CreateForumReactionDto(
                    userId = userId,
                    threadId = threadId,
                    reactionType = "love"
                )
            )
        }
    }

    override suspend fun toggleCommentLove(userId: String, commentId: String, currentlyLoved: Boolean) {
        if (currentlyLoved) {
            postgrest.from("forum_reactions").delete {
                filter {
                    eq("user_id", userId)
                    eq("comment_id", commentId)
                    eq("reaction_type", "love")
                }
            }
        } else {
            postgrest.from("forum_reactions").insert(
                CreateForumReactionDto(
                    userId = userId,
                    commentId = commentId,
                    reactionType = "love"
                )
            )
        }
    }

    override suspend fun reportThread(reporterId: String, threadId: String, reason: String, note: String?) {
        postgrest.from("forum_reports").insert(
            CreateForumReportDto(
                reporterId = reporterId,
                threadId = threadId,
                reason = reason,
                note = note?.trim()?.ifBlank { null }
            )
        )
    }

    override suspend fun reportComment(reporterId: String, commentId: String, reason: String, note: String?) {
        postgrest.from("forum_reports").insert(
            CreateForumReportDto(
                reporterId = reporterId,
                commentId = commentId,
                reason = reason,
                note = note?.trim()?.ifBlank { null }
            )
        )
    }

    private suspend fun loadAuthors(authorIds: List<String>): Map<String, AlumniDataDto> {
        val ids = authorIds.distinct().filter { it.isNotBlank() }
        if (ids.isEmpty()) return emptyMap()

        val cached = ids.mapNotNull { id -> authorCache[id]?.let { id to it } }.toMap()
        val missingIds = ids.filterNot { it in cached }

        if (missingIds.isNotEmpty()) {
            val fetched = postgrest.from("alumni_data").select {
                filter { isIn("id", missingIds) }
            }.decodeList<AlumniDataDto>()
            authorCache.putAll(fetched.associateBy { it.id })
        }

        return ids.mapNotNull { id -> authorCache[id]?.let { id to it } }.toMap()
    }

    private suspend fun loadLovedThreadIds(userId: String, threadIds: List<String>): Set<String> {
        if (threadIds.isEmpty()) return emptySet()
        return postgrest.from("forum_reactions").select {
            filter {
                eq("user_id", userId)
                eq("reaction_type", "love")
                isIn("thread_id", threadIds)
            }
        }.decodeList<ForumReactionDto>()
            .mapNotNull { it.threadId }
            .toSet()
    }

    private suspend fun loadLovedCommentIds(userId: String, commentIds: List<String>): Set<String> {
        if (commentIds.isEmpty()) return emptySet()
        return postgrest.from("forum_reactions").select {
            filter {
                eq("user_id", userId)
                eq("reaction_type", "love")
                isIn("comment_id", commentIds)
            }
        }.decodeList<ForumReactionDto>()
            .mapNotNull { it.commentId }
            .toSet()
    }

    private suspend fun loadThreadAttachments(threadIds: List<String>): Map<String, List<ForumAttachmentItem>> {
        if (threadIds.isEmpty()) return emptyMap()

        val attachments = postgrest.from("forum_attachments").select {
            filter {
                isIn("thread_id", threadIds)
            }
        }.decodeList<ForumAttachmentDto>()

        if (attachments.isEmpty()) return emptyMap()

        val signedUrls = createCachedSignedUrls(attachments.map { it.storagePath })

        return attachments
            .map { attachment ->
                ForumAttachmentItem(
                    attachment = attachment,
                    signedUrl = signedUrls[attachment.storagePath].orEmpty()
                )
            }
            .groupBy { it.attachment.threadId.orEmpty() }
    }

    private suspend fun createCachedSignedUrls(paths: List<String>): Map<String, String> {
        val distinctPaths = paths.distinct().filter { it.isNotBlank() }
        if (distinctPaths.isEmpty()) return emptyMap()

        val now = System.currentTimeMillis()
        val validCached = distinctPaths.mapNotNull { path ->
            signedUrlCache[path]
                ?.takeIf { it.expiresAtMillis > now }
                ?.let { path to it.url }
        }.toMap()
        val missing = distinctPaths.filterNot { it in validCached }

        if (missing.isNotEmpty()) {
            storage.from(FORUM_MEDIA_BUCKET)
                .createSignedUrls(1.hours, missing)
                .forEach { result ->
                    signedUrlCache[result.path] = CachedSignedUrl(
                        url = result.signedURL,
                        expiresAtMillis = now + SIGNED_URL_CACHE_TTL_MILLIS
                    )
                }
        }

        return distinctPaths.mapNotNull { path ->
            signedUrlCache[path]?.url?.let { path to it }
        }.toMap()
    }

    private suspend fun uploadThreadImage(authorId: String, threadId: String, uri: Uri) {
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        require(mimeType in ALLOWED_IMAGE_TYPES) { "Format gambar harus JPG, PNG, atau WebP." }

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Gambar tidak dapat dibaca.")
        require(bytes.size <= MAX_IMAGE_SIZE_BYTES) { "Ukuran gambar maksimal 5 MB." }

        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?: when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
        val path = "$authorId/${UUID.randomUUID()}.$extension"

        storage.from(FORUM_MEDIA_BUCKET).upload(path, bytes) {
            contentType = ContentType.parse(mimeType)
            upsert = false
        }

        postgrest.from("forum_attachments").insert(
            CreateForumAttachmentDto(
                threadId = threadId,
                uploaderId = authorId,
                storagePath = path,
                mimeType = mimeType,
                fileSize = bytes.size.toLong(),
                altText = "Gambar forum alumni"
            )
        )
    }

    private companion object {
        const val FORUM_MEDIA_BUCKET = "forum-media"
        const val MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024
        const val SIGNED_URL_CACHE_TTL_MILLIS = 50 * 60 * 1000L
        val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }

    private data class CachedSignedUrl(
        val url: String,
        val expiresAtMillis: Long
    )
}
