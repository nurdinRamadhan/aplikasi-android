package com.alhasanah.alhasanahmedia.ui.alumni

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.SupabaseClient
import com.alhasanah.alhasanahmedia.data.model.AlumniAccess
import com.alhasanah.alhasanahmedia.data.model.AlumniRecommendationItem
import com.alhasanah.alhasanahmedia.data.model.ForumCommentDto
import com.alhasanah.alhasanahmedia.data.model.ForumCommentItem
import com.alhasanah.alhasanahmedia.data.model.ForumThreadDetail
import com.alhasanah.alhasanahmedia.data.model.ForumThreadItem
import com.alhasanah.alhasanahmedia.data.repository.AlumniRepository
import com.alhasanah.alhasanahmedia.data.repository.AuthRepository
import com.alhasanah.alhasanahmedia.data.repository.ForumRepository
import com.alhasanah.alhasanahmedia.ui.auth.AuthenticationState
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

sealed class ForumAlumniUiState {
    object Loading : ForumAlumniUiState()
    object LoginRequired : ForumAlumniUiState()
    data class NotAlumni(val role: String) : ForumAlumniUiState()
    data class WaitingVerification(val name: String?) : ForumAlumniUiState()
    data class Ready(
        val access: AlumniAccess,
        val threads: List<ForumThreadItem>,
        val recommendations: List<AlumniRecommendationItem> = emptyList(),
        val isPosting: Boolean = false,
        val isRefreshing: Boolean = false,
        val actionInProgress: Boolean = false,
        val selectedDetail: ForumThreadDetail? = null,
        val isDetailLoading: Boolean = false,
        val transientMessage: String? = null
    ) : ForumAlumniUiState()
    data class Error(val message: String) : ForumAlumniUiState()
}

class ForumAlumniViewModel(
    private val authRepository: AuthRepository,
    private val alumniRepository: AlumniRepository,
    private val forumRepository: ForumRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<ForumAlumniUiState>(ForumAlumniUiState.Loading)
    val uiState: StateFlow<ForumAlumniUiState> = _uiState.asStateFlow()

    private var currentAccess: AlumniAccess? = null
    private var realtimeChannel: RealtimeChannel? = null
    private var realtimeJob: Job? = null
    private var realtimeRefreshJob: Job? = null

    init {
        observeAuth()
    }

    private fun observeAuth() {
        viewModelScope.launch {
            authRepository.getAuthState().collect { state ->
                when (state) {
                    AuthenticationState.NotAuthenticated -> {
                        currentAccess = null
                        stopRealtime()
                        _uiState.value = ForumAlumniUiState.LoginRequired
                    }
                    is AuthenticationState.Authenticated -> {
                        if (currentAccess?.userId == state.userId && _uiState.value is ForumAlumniUiState.Ready) {
                            return@collect
                        }
                        loadAccess(state.userId)
                    }
                }
            }
        }
    }

    fun refresh() {
        val access = currentAccess ?: return
        if (access.canOpenForum) {
            loadFeed(access, keepCurrent = true)
        }
    }

    fun clearMessage() {
        val current = _uiState.value
        if (current is ForumAlumniUiState.Ready) {
            _uiState.value = current.copy(transientMessage = null)
        }
    }

    fun createThread(content: String, imageUri: Uri? = null) {
        val access = currentAccess ?: return
        val cleanContent = content.trim()
        if (!access.canOpenForum || (cleanContent.isBlank() && imageUri == null)) return

        viewModelScope.launch {
            val current = _uiState.value
            if (current is ForumAlumniUiState.Ready) {
                _uiState.value = current.copy(isPosting = true)
            }
            runCatching {
                forumRepository.createThread(access.userId, cleanContent.ifBlank { "Berbagi gambar alumni." }, imageUri)
                loadFeedData(access)
            }.onSuccess { (threads, recommendations) ->
                _uiState.value = ForumAlumniUiState.Ready(access = access, threads = threads, recommendations = recommendations)
            }.onFailure { error ->
                Log.e("ForumAlumniVM", "Gagal membuat thread", error)
                _uiState.value = ForumAlumniUiState.Error(
                    error.localizedMessage ?: "Gagal mengirim posting forum."
                )
            }
        }
    }

    fun openThread(threadId: String) {
        val access = currentAccess ?: return
        val current = _uiState.value
        if (current !is ForumAlumniUiState.Ready) return
        val feedItem = current.threads.firstOrNull { it.thread.id == threadId }

        viewModelScope.launch {
            _uiState.value = current.copy(
                selectedDetail = feedItem?.let { ForumThreadDetail(item = it, comments = emptyList()) }
                    ?: current.selectedDetail,
                isDetailLoading = true,
                transientMessage = null
            )
            runCatching {
                forumRepository.getThreadDetail(access.userId, threadId)
            }.onSuccess { detail ->
                val latest = _uiState.value
                if (latest is ForumAlumniUiState.Ready) {
                    _uiState.value = latest.copy(
                        selectedDetail = detail,
                        isDetailLoading = false
                    )
                }
            }.onFailure { error ->
                Log.e("ForumAlumniVM", "Gagal memuat detail thread", error)
                val latest = _uiState.value
                if (latest is ForumAlumniUiState.Ready) {
                    _uiState.value = latest.copy(
                        isDetailLoading = false,
                        transientMessage = error.localizedMessage ?: "Gagal memuat diskusi."
                    )
                }
            }
        }
    }

    fun closeThread() {
        val current = _uiState.value
        if (current is ForumAlumniUiState.Ready) {
            _uiState.value = current.copy(selectedDetail = null, transientMessage = null)
        }
    }

    fun createComment(threadId: String, content: String) {
        val access = currentAccess ?: return
        val cleanContent = content.trim()
        if (!access.canOpenForum || cleanContent.isBlank()) return

        viewModelScope.launch {
            val before = _uiState.value as? ForumAlumniUiState.Ready
            val optimisticComment = ForumCommentItem(
                comment = ForumCommentDto(
                    id = "local-${UUID.randomUUID()}",
                    threadId = threadId,
                    authorId = access.userId,
                    content = cleanContent,
                    reactionCount = 0,
                    createdAt = Instant.now().toString()
                ),
                author = access.alumniData,
                lovedByMe = false
            )
            applyOptimisticComment(threadId, optimisticComment)

            runCatching {
                forumRepository.createComment(access.userId, threadId, cleanContent)
                forumRepository.getThreadDetail(access.userId, threadId)
            }.onSuccess { detail ->
                val (threads, recommendations) = loadFeedData(access)
                _uiState.value = ForumAlumniUiState.Ready(
                    access = access,
                    threads = threads,
                    recommendations = recommendations,
                    selectedDetail = detail
                )
            }.onFailure { error ->
                Log.e("ForumAlumniVM", "Gagal mengirim komentar", error)
                before?.let { _uiState.value = it }
                setReadyMessage(error.localizedMessage ?: "Komentar gagal dikirim.")
            }
        }
    }

    fun updateThread(threadId: String, content: String) {
        val access = currentAccess ?: return
        val cleanContent = content.trim()
        if (!access.canOpenForum || cleanContent.isBlank()) return

        viewModelScope.launch {
            setActionInProgress(true)
            runCatching {
                forumRepository.updateThread(threadId, cleanContent)
                val (threads, recommendations) = loadFeedData(access)
                val selectedId = (_uiState.value as? ForumAlumniUiState.Ready)?.selectedDetail?.item?.thread?.id
                val selectedDetail = selectedId?.let { forumRepository.getThreadDetail(access.userId, it) }
                Triple(threads, recommendations, selectedDetail)
            }.onSuccess { (threads, recommendations, selectedDetail) ->
                _uiState.value = ForumAlumniUiState.Ready(
                    access = access,
                    threads = threads,
                    recommendations = recommendations,
                    selectedDetail = selectedDetail,
                    transientMessage = "Postingan diperbarui."
                )
            }.onFailure { error ->
                Log.e("ForumAlumniVM", "Gagal memperbarui thread", error)
                setReadyMessage(error.localizedMessage ?: "Gagal memperbarui postingan.")
                setActionInProgress(false)
            }
        }
    }

    fun deleteThread(threadId: String) {
        val access = currentAccess ?: return
        viewModelScope.launch {
            setActionInProgress(true)
            runCatching {
                forumRepository.deleteThread(threadId)
                loadFeedData(access)
            }.onSuccess { (threads, recommendations) ->
                _uiState.value = ForumAlumniUiState.Ready(
                    access = access,
                    threads = threads,
                    recommendations = recommendations,
                    selectedDetail = null,
                    transientMessage = "Postingan dihapus."
                )
            }.onFailure { error ->
                Log.e("ForumAlumniVM", "Gagal menghapus thread", error)
                setReadyMessage(error.localizedMessage ?: "Gagal menghapus postingan.")
                setActionInProgress(false)
            }
        }
    }

    fun moderateThread(threadId: String, status: String? = null, isPinned: Boolean? = null, isLocked: Boolean? = null) {
        val access = currentAccess ?: return
        if (!access.isForumAdmin) return
        viewModelScope.launch {
            setActionInProgress(true)
            runCatching {
                forumRepository.moderateThread(threadId, status, isPinned, isLocked)
                val (threads, recommendations) = loadFeedData(access)
                val selectedId = (_uiState.value as? ForumAlumniUiState.Ready)?.selectedDetail?.item?.thread?.id
                val selectedDetail = selectedId?.let { forumRepository.getThreadDetail(access.userId, it) }
                Triple(threads, recommendations, selectedDetail)
            }.onSuccess { (threads, recommendations, selectedDetail) ->
                _uiState.value = ForumAlumniUiState.Ready(
                    access = access,
                    threads = threads,
                    recommendations = recommendations,
                    selectedDetail = selectedDetail,
                    transientMessage = "Moderasi forum diperbarui."
                )
            }.onFailure { error ->
                Log.e("ForumAlumniVM", "Gagal moderasi thread", error)
                setReadyMessage(error.localizedMessage ?: "Gagal memperbarui moderasi.")
                setActionInProgress(false)
            }
        }
    }

    fun updateComment(commentId: String, threadId: String, content: String) {
        val access = currentAccess ?: return
        val cleanContent = content.trim()
        if (!access.canOpenForum || cleanContent.isBlank()) return

        viewModelScope.launch {
            setActionInProgress(true)
            runCatching {
                forumRepository.updateComment(commentId, cleanContent)
                forumRepository.getThreadDetail(access.userId, threadId)
            }.onSuccess { detail ->
                val (threads, recommendations) = loadFeedData(access)
                _uiState.value = ForumAlumniUiState.Ready(
                    access = access,
                    threads = threads,
                    recommendations = recommendations,
                    selectedDetail = detail,
                    transientMessage = "Komentar diperbarui."
                )
            }.onFailure { error ->
                Log.e("ForumAlumniVM", "Gagal memperbarui komentar", error)
                setReadyMessage(error.localizedMessage ?: "Gagal memperbarui komentar.")
                setActionInProgress(false)
            }
        }
    }

    fun deleteComment(commentId: String, threadId: String) {
        val access = currentAccess ?: return
        viewModelScope.launch {
            setActionInProgress(true)
            runCatching {
                forumRepository.deleteComment(commentId)
                forumRepository.getThreadDetail(access.userId, threadId)
            }.onSuccess { detail ->
                val (threads, recommendations) = loadFeedData(access)
                _uiState.value = ForumAlumniUiState.Ready(
                    access = access,
                    threads = threads,
                    recommendations = recommendations,
                    selectedDetail = detail,
                    transientMessage = "Komentar dihapus."
                )
            }.onFailure { error ->
                Log.e("ForumAlumniVM", "Gagal menghapus komentar", error)
                setReadyMessage(error.localizedMessage ?: "Gagal menghapus komentar.")
                setActionInProgress(false)
            }
        }
    }

    fun toggleLove(item: ForumThreadItem) {
        val access = currentAccess ?: return
        viewModelScope.launch {
            val before = _uiState.value as? ForumAlumniUiState.Ready
            applyOptimisticThreadLove(item.thread.id, item.lovedByMe)
            runCatching {
                forumRepository.toggleThreadLove(
                    userId = access.userId,
                    threadId = item.thread.id,
                    currentlyLoved = item.lovedByMe
                )
            }.onFailure { error ->
                Log.e("ForumAlumniVM", "Gagal mengubah reaction", error)
                before?.let { _uiState.value = it }
                setReadyMessage(error.localizedMessage ?: "Gagal memperbarui dukungan.")
            }
        }
    }

    fun toggleCommentLove(item: ForumCommentItem) {
        val access = currentAccess ?: return
        viewModelScope.launch {
            val before = _uiState.value as? ForumAlumniUiState.Ready
            applyOptimisticCommentLove(item.comment.id, item.lovedByMe)
            runCatching {
                forumRepository.toggleCommentLove(
                    userId = access.userId,
                    commentId = item.comment.id,
                    currentlyLoved = item.lovedByMe
                )
            }.onFailure { error ->
                Log.e("ForumAlumniVM", "Gagal mengubah reaction komentar", error)
                before?.let { _uiState.value = it }
                setReadyMessage(error.localizedMessage ?: "Gagal memperbarui dukungan komentar.")
            }
        }
    }

    fun reportThread(threadId: String, reason: String, note: String?) {
        val access = currentAccess ?: return
        viewModelScope.launch {
            runCatching {
                forumRepository.reportThread(access.userId, threadId, reason, note)
            }.onSuccess {
                setReadyMessage("Laporan terkirim. Admin akan meninjau konten ini.")
            }.onFailure { error ->
                Log.e("ForumAlumniVM", "Gagal melaporkan thread", error)
                setReadyMessage(error.localizedMessage ?: "Gagal mengirim laporan.")
            }
        }
    }

    fun reportComment(commentId: String, reason: String, note: String?) {
        val access = currentAccess ?: return
        viewModelScope.launch {
            runCatching {
                forumRepository.reportComment(access.userId, commentId, reason, note)
            }.onSuccess {
                setReadyMessage("Laporan komentar terkirim. Admin akan meninjau konten ini.")
            }.onFailure { error ->
                Log.e("ForumAlumniVM", "Gagal melaporkan komentar", error)
                setReadyMessage(error.localizedMessage ?: "Gagal mengirim laporan komentar.")
            }
        }
    }

    fun followAlumni(alumniId: String) {
        val access = currentAccess ?: return
        if (alumniId == access.userId) return
        viewModelScope.launch {
            val before = _uiState.value as? ForumAlumniUiState.Ready
            applyOptimisticFollow(alumniId)
            runCatching {
                alumniRepository.follow(access.userId, alumniId)
                alumniRepository.getRecommendations(access.userId)
            }.onSuccess { recommendations ->
                val current = _uiState.value as? ForumAlumniUiState.Ready ?: return@onSuccess
                _uiState.value = current.copy(
                    recommendations = recommendations,
                    transientMessage = "Alumni diikuti."
                )
            }.onFailure { error ->
                before?.let { _uiState.value = it }
                setReadyMessage(error.localizedMessage ?: "Gagal mengikuti alumni.")
            }
        }
    }

    private fun loadAccess(userId: String) {
        viewModelScope.launch {
            val cachedAccess = alumniRepository.getCachedAccess(userId)
            if (cachedAccess?.canOpenForum == true) {
                currentAccess = cachedAccess
                val cachedThreads = forumRepository.getCachedThreads(userId).orEmpty()
                val cachedRecommendations = alumniRepository.getCachedRecommendations(userId).orEmpty()
                if (cachedThreads.isNotEmpty() || cachedRecommendations.isNotEmpty()) {
                    _uiState.value = ForumAlumniUiState.Ready(
                        access = cachedAccess,
                        threads = cachedThreads,
                        recommendations = cachedRecommendations,
                        isRefreshing = true
                    )
                } else {
                    _uiState.value = ForumAlumniUiState.Loading
                }
            } else {
                _uiState.value = ForumAlumniUiState.Loading
            }
            runCatching {
                alumniRepository.getAccess(userId)
            }.onSuccess { access ->
                currentAccess = access
                when {
                    !access.isActive -> _uiState.value = ForumAlumniUiState.WaitingVerification(access.profileName)
                    !access.canOpenForum -> _uiState.value = ForumAlumniUiState.NotAlumni(access.role)
                    else -> {
                        startRealtime(access)
                        loadFeed(access)
                    }
                }
            }.onFailure { error ->
                Log.e("ForumAlumniVM", "Gagal memeriksa akses alumni", error)
                _uiState.value = ForumAlumniUiState.Error(
                    error.localizedMessage ?: "Gagal memeriksa akses forum alumni."
                )
            }
        }
    }

    private fun loadFeed(access: AlumniAccess, keepCurrent: Boolean = false) {
        viewModelScope.launch {
            val current = _uiState.value
            if (keepCurrent && current is ForumAlumniUiState.Ready) {
                _uiState.value = current.copy(isRefreshing = true, transientMessage = null)
            }
            runCatching {
                loadFeedData(access)
            }.onSuccess { (threads, recommendations) ->
                val selectedId = (_uiState.value as? ForumAlumniUiState.Ready)?.selectedDetail?.item?.thread?.id
                val selectedDetail = if (keepCurrent) selectedId?.let { forumRepository.getThreadDetail(access.userId, it) } else null
                _uiState.value = ForumAlumniUiState.Ready(
                    access = access,
                    threads = threads,
                    recommendations = recommendations,
                    selectedDetail = selectedDetail
                )
            }.onFailure { error ->
                Log.e("ForumAlumniVM", "Gagal memuat forum", error)
                if (keepCurrent && current is ForumAlumniUiState.Ready) {
                    _uiState.value = current.copy(
                        isRefreshing = false,
                        transientMessage = error.localizedMessage ?: "Gagal memuat ulang forum."
                    )
                } else {
                    _uiState.value = ForumAlumniUiState.Error(
                        error.localizedMessage ?: "Gagal memuat forum alumni."
                    )
                }
            }
        }
    }

    private suspend fun loadFeedData(access: AlumniAccess): Pair<List<ForumThreadItem>, List<AlumniRecommendationItem>> {
        return coroutineScope {
            val threads = async { forumRepository.getThreads(access.userId) }
            val recommendations = async {
                runCatching { alumniRepository.getRecommendations(access.userId) }.getOrDefault(emptyList())
            }
            threads.await() to recommendations.await()
        }
    }

    private fun setReadyMessage(message: String) {
        val current = _uiState.value
        if (current is ForumAlumniUiState.Ready) {
            _uiState.value = current.copy(transientMessage = message, actionInProgress = false)
        }
    }

    private fun setActionInProgress(value: Boolean) {
        val current = _uiState.value
        if (current is ForumAlumniUiState.Ready) {
            _uiState.value = current.copy(actionInProgress = value)
        }
    }

    private fun startRealtime(access: AlumniAccess) {
        if (realtimeChannel != null) return
        val channel = supabaseClient.channel("forum-alumni-live:${access.userId}:${System.identityHashCode(this)}")
        realtimeChannel = channel

        realtimeJob = merge(
            channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "forum_threads" },
            channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "forum_comments" },
            channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "forum_reactions" },
            channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "forum_reports" },
            channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "alumni_follows" }
        ).onEach {
            scheduleRealtimeRefresh(access)
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            runCatching {
                supabaseClient.realtime.setAuth()
                channel.subscribe()
            }.onFailure { error ->
                Log.e("ForumAlumniVM", "Gagal subscribe realtime forum", error)
            }
        }
    }

    private fun scheduleRealtimeRefresh(access: AlumniAccess) {
        realtimeRefreshJob?.cancel()
        realtimeRefreshJob = viewModelScope.launch {
            delay(350)
            refreshFromRealtime(access)
        }
    }

    private suspend fun refreshFromRealtime(access: AlumniAccess) {
        val current = _uiState.value as? ForumAlumniUiState.Ready ?: return
        runCatching {
            val (threads, recommendations) = loadFeedData(access)
            val selectedId = current.selectedDetail?.item?.thread?.id
            val selectedDetail = selectedId?.let { forumRepository.getThreadDetail(access.userId, it) }
            _uiState.value = current.copy(
                threads = threads,
                recommendations = recommendations,
                selectedDetail = selectedDetail,
                isRefreshing = false,
                isDetailLoading = false
            )
        }.onFailure { error ->
            Log.e("ForumAlumniVM", "Gagal sync realtime forum", error)
        }
    }

    private fun stopRealtime() {
        val channel = realtimeChannel
        realtimeRefreshJob?.cancel()
        realtimeJob?.cancel()
        realtimeRefreshJob = null
        realtimeJob = null
        realtimeChannel = null
        if (channel != null) {
            viewModelScope.launch {
                runCatching { supabaseClient.realtime.removeChannel(channel) }
            }
        }
    }

    private fun applyOptimisticThreadLove(threadId: String, currentlyLoved: Boolean) {
        val current = _uiState.value as? ForumAlumniUiState.Ready ?: return
        val delta = if (currentlyLoved) -1 else 1
        val updatedThreads = current.threads.map { item ->
            if (item.thread.id == threadId) {
                item.copy(
                    thread = item.thread.copy(
                        reactionCount = (item.thread.reactionCount + delta).coerceAtLeast(0)
                    ),
                    lovedByMe = !currentlyLoved
                )
            } else item
        }
        val updatedDetail = current.selectedDetail?.let { detail ->
            if (detail.item.thread.id == threadId) {
                detail.copy(
                    item = detail.item.copy(
                        thread = detail.item.thread.copy(
                            reactionCount = (detail.item.thread.reactionCount + delta).coerceAtLeast(0)
                        ),
                        lovedByMe = !currentlyLoved
                    )
                )
            } else detail
        }
        _uiState.value = current.copy(threads = updatedThreads, selectedDetail = updatedDetail)
    }

    private fun applyOptimisticFollow(alumniId: String) {
        val current = _uiState.value as? ForumAlumniUiState.Ready ?: return
        _uiState.value = current.copy(
            recommendations = current.recommendations.filterNot { it.alumni.id == alumniId }
        )
    }

    private fun applyOptimisticCommentLove(commentId: String, currentlyLoved: Boolean) {
        val current = _uiState.value as? ForumAlumniUiState.Ready ?: return
        val delta = if (currentlyLoved) -1 else 1
        val updatedDetail = current.selectedDetail?.let { detail ->
            detail.copy(
                comments = detail.comments.map { item ->
                    if (item.comment.id == commentId) {
                        item.copy(
                            comment = item.comment.copy(
                                reactionCount = (item.comment.reactionCount + delta).coerceAtLeast(0)
                            ),
                            lovedByMe = !currentlyLoved
                        )
                    } else item
                }
            )
        }
        _uiState.value = current.copy(selectedDetail = updatedDetail)
    }

    private fun applyOptimisticComment(threadId: String, comment: ForumCommentItem) {
        val current = _uiState.value as? ForumAlumniUiState.Ready ?: return
        val updatedThreads = current.threads.map { item ->
            if (item.thread.id == threadId) {
                item.copy(thread = item.thread.copy(commentCount = item.thread.commentCount + 1))
            } else item
        }
        val updatedDetail = current.selectedDetail?.let { detail ->
            if (detail.item.thread.id == threadId) {
                detail.copy(
                    item = detail.item.copy(
                        thread = detail.item.thread.copy(commentCount = detail.item.thread.commentCount + 1)
                    ),
                    comments = detail.comments + comment
                )
            } else detail
        }
        _uiState.value = current.copy(threads = updatedThreads, selectedDetail = updatedDetail)
    }

    override fun onCleared() {
        stopRealtime()
        super.onCleared()
    }
}
