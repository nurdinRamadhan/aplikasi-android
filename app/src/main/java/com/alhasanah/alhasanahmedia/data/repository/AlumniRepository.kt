package com.alhasanah.alhasanahmedia.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.alhasanah.alhasanahmedia.data.model.AlumniAccess
import com.alhasanah.alhasanahmedia.data.model.AlumniDataDto
import com.alhasanah.alhasanahmedia.data.model.AlumniDirectoryItem
import com.alhasanah.alhasanahmedia.data.model.AlumniFollowDto
import com.alhasanah.alhasanahmedia.data.model.AlumniFollowStats
import com.alhasanah.alhasanahmedia.data.model.AlumniFollowUser
import com.alhasanah.alhasanahmedia.data.model.AlumniRecommendationItem
import com.alhasanah.alhasanahmedia.data.model.CreateAlumniFollowDto
import com.alhasanah.alhasanahmedia.data.model.ForumCommentDto
import com.alhasanah.alhasanahmedia.data.model.ForumCommentItem
import com.alhasanah.alhasanahmedia.data.model.AlumniProfile
import com.alhasanah.alhasanahmedia.data.model.ProfileAccessDto
import com.alhasanah.alhasanahmedia.data.model.UpdateAlumniProfileDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage
import io.ktor.http.ContentType
import java.util.UUID
import kotlin.time.Duration.Companion.hours

interface AlumniRepository {
    suspend fun getCachedAccess(userId: String): AlumniAccess?
    suspend fun getAccess(userId: String): AlumniAccess
    suspend fun getAlumniDirectory(): List<AlumniDataDto>
    suspend fun getAlumniDirectoryItems(): List<AlumniDirectoryItem>
    suspend fun getCachedProfile(viewerId: String, alumniId: String): AlumniProfile?
    suspend fun getProfile(viewerId: String, alumniId: String): AlumniProfile
    suspend fun updateProfile(userId: String, update: UpdateAlumniProfileDto): AlumniDataDto
    suspend fun uploadAvatar(userId: String, uri: Uri): String
    suspend fun getCachedRecommendations(userId: String): List<AlumniRecommendationItem>?
    suspend fun getRecommendations(userId: String, limit: Int = 10): List<AlumniRecommendationItem>
    suspend fun getFollowers(viewerId: String, alumniId: String): List<AlumniFollowUser>
    suspend fun getFollowing(viewerId: String, alumniId: String): List<AlumniFollowUser>
    suspend fun follow(followerId: String, followingId: String)
    suspend fun unfollow(followerId: String, followingId: String)
}

class AlumniRepositoryImpl(
    private val context: Context,
    private val postgrest: Postgrest,
    private val storage: Storage,
    private val forumRepository: ForumRepository,
    private val cacheStore: AlumniLocalCacheStore
) : AlumniRepository {

    private var accessCache: Pair<String, AlumniAccess>? = null
    private var directoryItemsCache: Pair<Long, List<AlumniDirectoryItem>>? = null

    override suspend fun getCachedAccess(userId: String): AlumniAccess? =
        accessCache?.takeIf { it.first == userId }?.second ?: cacheStore.getAccess(userId)

    override suspend fun getAccess(userId: String): AlumniAccess {
        val profile = postgrest.from("profiles").select {
            filter {
                eq("id", userId)
            }
        }.decodeSingle<ProfileAccessDto>()

        val alumniData = runCatching {
            postgrest.from("alumni_data").select {
                filter {
                    eq("id", userId)
                }
            }.decodeSingleOrNull<AlumniDataDto>()
        }.getOrNull()

        return AlumniAccess(
            userId = profile.id,
            role = profile.role.orEmpty(),
            isActive = profile.isActive == true,
            profileName = profile.fullName,
            alumniData = alumniData
        ).also {
            accessCache = userId to it
            cacheStore.saveAccess(it)
        }
    }

    override suspend fun getAlumniDirectory(): List<AlumniDataDto> {
        return postgrest.from("alumni_data").select()
            .decodeList<AlumniDataDto>()
    }

    override suspend fun getAlumniDirectoryItems(): List<AlumniDirectoryItem> {
        directoryItemsCache?.let { (cachedAt, items) ->
            if (System.currentTimeMillis() - cachedAt < DIRECTORY_CACHE_TTL_MS) return items
        }
        cacheStore.getDirectory()?.let {
            directoryItemsCache = System.currentTimeMillis() to it
            return it
        }

        val alumni = postgrest.from("alumni_data").select()
            .decodeList<AlumniDataDto>()
            .sortedWith(compareByDescending<AlumniDataDto> { it.tahunLulus }.thenBy { it.fullName })

        val signedUrls = createAvatarSignedUrls(alumni.mapNotNull { it.avatarStoragePath })

        return alumni.map { item ->
            AlumniDirectoryItem(
                alumni = item,
                avatarUrl = signedUrls[item.avatarStoragePath]
            )
        }.also {
            directoryItemsCache = System.currentTimeMillis() to it
            cacheStore.saveDirectory(it)
        }
    }

    override suspend fun getCachedProfile(viewerId: String, alumniId: String): AlumniProfile? =
        cacheStore.getProfile(viewerId, alumniId)

    override suspend fun getProfile(viewerId: String, alumniId: String): AlumniProfile {
        val alumni = postgrest.from("alumni_data").select {
            filter { eq("id", alumniId) }
            limit(1)
        }.decodeSingle<AlumniDataDto>()

        val posts = forumRepository.getThreadsByAuthor(viewerId, alumniId)
        val replies = runCatching {
            postgrest.from("forum_comments").select {
                filter { eq("author_id", alumniId) }
            }.decodeList<ForumCommentDto>()
                .sortedByDescending { it.createdAt }
                .map { comment ->
                    ForumCommentItem(
                        comment = comment,
                        author = alumni,
                        lovedByMe = false
                    )
                }
        }.getOrDefault(emptyList())

        return AlumniProfile(
            alumni = alumni,
            avatarUrl = createAvatarSignedUrl(alumni.avatarStoragePath),
            posts = posts,
            replies = replies,
            postCount = posts.size,
            commentCount = replies.size,
            reactionCount = posts.sumOf { it.thread.reactionCount },
            followStats = loadFollowStats(viewerId, alumniId),
            isOwnProfile = viewerId == alumniId
        ).also { cacheStore.saveProfile(viewerId, it) }
    }

    override suspend fun updateProfile(userId: String, update: UpdateAlumniProfileDto): AlumniDataDto {
        return postgrest.from("alumni_data").update(update) {
            filter { eq("id", userId) }
            select()
            single()
        }.decodeSingle<AlumniDataDto>().also {
            directoryItemsCache = null
            accessCache = null
        }
    }

    override suspend fun uploadAvatar(userId: String, uri: Uri): String {
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        require(mimeType in ALLOWED_AVATAR_TYPES) { "Format foto harus JPG, PNG, atau WebP." }

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Foto profil tidak dapat dibaca.")
        require(bytes.size <= MAX_AVATAR_SIZE_BYTES) { "Ukuran foto profil maksimal 2 MB." }

        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType)
            ?: when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
        val path = "$userId/${UUID.randomUUID()}.$extension"

        storage.from(ALUMNI_AVATAR_BUCKET).upload(path, bytes) {
            contentType = ContentType.parse(mimeType)
            upsert = false
        }

        return path
    }

    override suspend fun getCachedRecommendations(userId: String): List<AlumniRecommendationItem>? =
        cacheStore.getRecommendations(userId)

    override suspend fun getRecommendations(userId: String, limit: Int): List<AlumniRecommendationItem> {
        val me = postgrest.from("alumni_data").select {
            filter { eq("id", userId) }
            limit(1)
        }.decodeSingleOrNull<AlumniDataDto>()

        val followingIds = postgrest.from("alumni_follows").select {
            filter { eq("follower_id", userId) }
        }.decodeList<AlumniFollowDto>().map { it.followingId }.toSet()

        val candidates = postgrest.from("alumni_data").select()
            .decodeList<AlumniDataDto>()
            .filter { it.id != userId && it.id !in followingIds }

        val ranked = candidates.sortedWith(
            compareByDescending<AlumniDataDto> { candidate ->
                recommendationScore(me, candidate)
            }.thenByDescending { it.tahunLulus }.thenBy { it.fullName }
        ).take(limit)

        val signedUrls = createAvatarSignedUrls(ranked.mapNotNull { it.avatarStoragePath })
        return ranked.map { alumni ->
            AlumniRecommendationItem(
                alumni = alumni,
                avatarUrl = signedUrls[alumni.avatarStoragePath],
                reason = recommendationReason(me, alumni),
                followedByMe = false
            )
        }.also { cacheStore.saveRecommendations(userId, it) }
    }

    override suspend fun getFollowers(viewerId: String, alumniId: String): List<AlumniFollowUser> {
        val rows = postgrest.from("alumni_follows").select {
            filter { eq("following_id", alumniId) }
        }.decodeList<AlumniFollowDto>()
        return loadFollowUsers(viewerId, rows.map { it.followerId })
    }

    override suspend fun getFollowing(viewerId: String, alumniId: String): List<AlumniFollowUser> {
        val rows = postgrest.from("alumni_follows").select {
            filter { eq("follower_id", alumniId) }
        }.decodeList<AlumniFollowDto>()
        return loadFollowUsers(viewerId, rows.map { it.followingId })
    }

    override suspend fun follow(followerId: String, followingId: String) {
        postgrest.from("alumni_follows").insert(
            CreateAlumniFollowDto(followerId = followerId, followingId = followingId)
        )
    }

    override suspend fun unfollow(followerId: String, followingId: String) {
        postgrest.from("alumni_follows").delete {
            filter {
                eq("follower_id", followerId)
                eq("following_id", followingId)
            }
        }
    }

    private suspend fun loadFollowStats(viewerId: String, alumniId: String): AlumniFollowStats {
        val followers = postgrest.from("alumni_follows").select {
            filter { eq("following_id", alumniId) }
        }.decodeList<AlumniFollowDto>()
        val following = postgrest.from("alumni_follows").select {
            filter { eq("follower_id", alumniId) }
        }.decodeList<AlumniFollowDto>()
        return AlumniFollowStats(
            followerCount = followers.size,
            followingCount = following.size,
            followedByMe = followers.any { it.followerId == viewerId }
        )
    }

    private suspend fun loadFollowUsers(viewerId: String, userIds: List<String>): List<AlumniFollowUser> {
        val ids = userIds.distinct().filter { it.isNotBlank() }
        if (ids.isEmpty()) return emptyList()
        val alumni = postgrest.from("alumni_data").select {
            filter { isIn("id", ids) }
        }.decodeList<AlumniDataDto>()
        val followingByViewer = postgrest.from("alumni_follows").select {
            filter {
                eq("follower_id", viewerId)
                isIn("following_id", ids)
            }
        }.decodeList<AlumniFollowDto>().map { it.followingId }.toSet()
        val signedUrls = createAvatarSignedUrls(alumni.mapNotNull { it.avatarStoragePath })
        val byId = alumni.associateBy { it.id }
        return ids.mapNotNull { id ->
            byId[id]?.let { item ->
                AlumniFollowUser(
                    alumni = item,
                    avatarUrl = signedUrls[item.avatarStoragePath],
                    followedByMe = id in followingByViewer
                )
            }
        }
    }

    private fun recommendationScore(me: AlumniDataDto?, candidate: AlumniDataDto): Int {
        if (me == null) return candidate.tahunLulus
        var score = 0
        if (candidate.tahunLulus == me.tahunLulus) score += 80
        if (!candidate.regencyCode.isNullOrBlank() && candidate.regencyCode == me.regencyCode) score += 60
        if (!candidate.provinceCode.isNullOrBlank() && candidate.provinceCode == me.provinceCode) score += 35
        if (!candidate.profesiSekarang.isNullOrBlank() && candidate.profesiSekarang.equals(me.profesiSekarang, ignoreCase = true)) score += 25
        score += candidate.tahunLulus.coerceAtMost(9999) / 100
        return score
    }

    private fun recommendationReason(me: AlumniDataDto?, candidate: AlumniDataDto): String {
        if (me != null && candidate.tahunLulus == me.tahunLulus) return "Satu angkatan ${candidate.tahunLulus}"
        if (me != null && !candidate.regencyName.isNullOrBlank() && candidate.regencyCode == me.regencyCode) return "Satu kota domisili"
        if (me != null && !candidate.provinceName.isNullOrBlank() && candidate.provinceCode == me.provinceCode) return "Satu provinsi"
        if (me != null && !candidate.profesiSekarang.isNullOrBlank() && candidate.profesiSekarang.equals(me.profesiSekarang, ignoreCase = true)) return "Profesi serupa"
        return "Alumni angkatan ${candidate.tahunLulus}"
    }

    private suspend fun createAvatarSignedUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return storage.from(ALUMNI_AVATAR_BUCKET)
            .createSignedUrls(6.hours, listOf(path))
            .firstOrNull()
            ?.signedURL
    }

    private suspend fun createAvatarSignedUrls(paths: List<String>): Map<String, String> {
        if (paths.isEmpty()) return emptyMap()
        return storage.from(ALUMNI_AVATAR_BUCKET)
            .createSignedUrls(6.hours, paths.distinct())
            .associate { it.path to it.signedURL }
    }

    private companion object {
        const val ALUMNI_AVATAR_BUCKET = "alumni-avatars"
        const val MAX_AVATAR_SIZE_BYTES = 2 * 1024 * 1024
        const val DIRECTORY_CACHE_TTL_MS = 5 * 60 * 1000L
        val ALLOWED_AVATAR_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }
}
