package com.alhasanah.alhasanahmedia.ui.alumni

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alhasanah.alhasanahmedia.data.model.AlumniAccess
import com.alhasanah.alhasanahmedia.data.model.AlumniDirectoryItem
import com.alhasanah.alhasanahmedia.data.model.ChatConversationItem
import com.alhasanah.alhasanahmedia.data.model.ChatDetail
import com.alhasanah.alhasanahmedia.data.model.ChatMessageDto
import com.alhasanah.alhasanahmedia.data.model.ChatMessageItem
import com.alhasanah.alhasanahmedia.data.model.ChatOutboxMessage
import com.alhasanah.alhasanahmedia.data.model.ChatPresenceDto
import com.alhasanah.alhasanahmedia.data.model.ChatTypingPayload
import com.alhasanah.alhasanahmedia.data.repository.AlumniRepository
import com.alhasanah.alhasanahmedia.data.repository.AuthRepository
import com.alhasanah.alhasanahmedia.data.repository.ChatOutboxStore
import com.alhasanah.alhasanahmedia.data.repository.ChatRepository
import com.alhasanah.alhasanahmedia.ui.auth.AuthenticationState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

sealed class AlumniChatUiState {
    object Loading : AlumniChatUiState()
    object LoginRequired : AlumniChatUiState()
    data class Locked(val message: String) : AlumniChatUiState()
    data class Ready(
        val access: AlumniAccess,
        val conversations: List<ChatConversationItem> = emptyList(),
        val directory: List<AlumniDirectoryItem> = emptyList(),
        val selectedDetail: ChatDetail? = null,
        val isRefreshing: Boolean = false,
        val isSending: Boolean = false,
        val isBackingUpKey: Boolean = false,
        val isRestoringKey: Boolean = false,
        val isLoadingOlder: Boolean = false,
        val hasMoreMessages: Boolean = true,
        val typingNames: List<String> = emptyList(),
        val onlineNames: List<String> = emptyList(),
        val presenceByUserId: Map<String, ChatPresenceDto> = emptyMap(),
        val failedDraft: String? = null,
        val transientMessage: String? = null
    ) : AlumniChatUiState()
    data class Error(val message: String) : AlumniChatUiState()
}

class AlumniChatViewModel(
    private val authRepository: AuthRepository,
    private val alumniRepository: AlumniRepository,
    private val chatRepository: ChatRepository,
    private val outboxStore: ChatOutboxStore,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlumniChatUiState>(AlumniChatUiState.Loading)
    val uiState: StateFlow<AlumniChatUiState> = _uiState.asStateFlow()

    private var access: AlumniAccess? = null
    private var realtimeChannel: RealtimeChannel? = null
    private var typingChannel: RealtimeChannel? = null
    private var realtimeJob: Job? = null
    private var realtimeJobs: List<Job> = emptyList()
    private var refreshJob: Job? = null
    private var typingJob: Job? = null
    private var presenceJob: Job? = null
    private val typingUsers = mutableMapOf<String, String>()
    private var pendingOpenConversationId: String? = null
    private var pendingDirectUserId: String? = null
    private var requestedConversationId: String? = null

    init {
        observeAuth()
    }

    fun refresh() {
        val currentAccess = access ?: return
        loadConversations(currentAccess, keepCurrent = true)
    }

    fun openInitialConversation(conversationId: String?) {
        if (conversationId.isNullOrBlank()) return
        val current = _uiState.value as? AlumniChatUiState.Ready
        if (current != null) {
            openConversation(conversationId)
        } else {
            pendingOpenConversationId = conversationId
        }
    }

    fun openInitialDirectChat(otherUserId: String?) {
        if (otherUserId.isNullOrBlank()) return
        val current = _uiState.value as? AlumniChatUiState.Ready
        if (current != null) {
            startDirectChat(otherUserId)
        } else {
            pendingDirectUserId = otherUserId
        }
    }

    fun clearMessage() {
        val current = _uiState.value as? AlumniChatUiState.Ready ?: return
        _uiState.value = current.copy(transientMessage = null)
    }

    fun createEncryptedKeyBackup(passphrase: String) {
        val currentAccess = access ?: return
        val current = _uiState.value as? AlumniChatUiState.Ready ?: return
        if (passphrase.length < 12) {
            _uiState.value = current.copy(transientMessage = "Passphrase backup minimal 12 karakter.")
            return
        }
        _uiState.value = current.copy(isBackingUpKey = true)
        viewModelScope.launch {
            val chars = passphrase.toCharArray()
            runCatching {
                chatRepository.createEncryptedKeyBackup(currentAccess.userId, chars)
            }.onSuccess {
                val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onSuccess
                _uiState.value = state.copy(
                    isBackingUpKey = false,
                    transientMessage = "Backup kunci E2EE berhasil dibuat."
                )
            }.onFailure { error ->
                Log.e("AlumniChatVM", "Gagal membuat backup kunci E2EE", error)
                val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onFailure
                _uiState.value = state.copy(
                    isBackingUpKey = false,
                    transientMessage = error.localizedMessage ?: "Gagal membuat backup kunci E2EE."
                )
            }
            chars.fill('\u0000')
        }
    }

    fun restoreEncryptedKeyBackup(passphrase: String) {
        val currentAccess = access ?: return
        val current = _uiState.value as? AlumniChatUiState.Ready ?: return
        if (passphrase.length < 12) {
            _uiState.value = current.copy(transientMessage = "Passphrase backup minimal 12 karakter.")
            return
        }
        _uiState.value = current.copy(isRestoringKey = true)
        viewModelScope.launch {
            val chars = passphrase.toCharArray()
            runCatching {
                chatRepository.restoreEncryptedKeyBackup(currentAccess.userId, chars)
            }.onSuccess {
                val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onSuccess
                _uiState.value = state.copy(
                    isRestoringKey = false,
                    transientMessage = "Kunci E2EE berhasil dipulihkan."
                )
                state.selectedDetail?.conversation?.conversation?.id?.let { openConversation(it) }
            }.onFailure { error ->
                Log.e("AlumniChatVM", "Gagal memulihkan backup kunci E2EE", error)
                val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onFailure
                _uiState.value = state.copy(
                    isRestoringKey = false,
                    transientMessage = error.localizedMessage ?: "Gagal memulihkan backup kunci E2EE."
                )
            }
            chars.fill('\u0000')
        }
    }

    fun revokeCurrentDeviceKey() {
        val currentAccess = access ?: return
        viewModelScope.launch {
            runCatching {
                chatRepository.revokeCurrentDeviceKey(currentAccess.userId)
            }.onSuccess {
                val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onSuccess
                _uiState.value = state.copy(
                    selectedDetail = null,
                    transientMessage = "Kunci perangkat ini dicabut. Buat atau pulihkan backup untuk membaca chat terenkripsi."
                )
            }.onFailure { error ->
                Log.e("AlumniChatVM", "Gagal mencabut kunci perangkat", error)
                val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onFailure
                _uiState.value = state.copy(
                    transientMessage = error.localizedMessage ?: "Gagal mencabut kunci perangkat."
                )
            }
        }
    }

    fun openConversation(conversationId: String) {
        val currentAccess = access ?: return
        requestedConversationId = conversationId
        viewModelScope.launch {
            chatRepository.getCachedMessages(currentAccess.userId, conversationId)?.let { cachedDetail ->
                val current = _uiState.value as? AlumniChatUiState.Ready
                if (current != null && requestedConversationId == conversationId) {
                    _uiState.value = current.copy(
                        selectedDetail = mergeOutbox(cachedDetail, currentAccess),
                        typingNames = emptyList(),
                        hasMoreMessages = cachedDetail.messages.size >= 50
                    )
                    subscribeTyping(cachedDetail.conversation.conversation.id)
                }
            }
            runCatching {
                val detail = chatRepository.getMessages(currentAccess.userId, conversationId)
                chatRepository.markRead(currentAccess.userId, conversationId)
                detail
            }.onSuccess { detail ->
                val current = _uiState.value as? AlumniChatUiState.Ready ?: return@onSuccess
                if (requestedConversationId != conversationId) return@onSuccess
                _uiState.value = current.copy(
                    selectedDetail = mergeOutbox(detail, currentAccess),
                    typingNames = emptyList(),
                    hasMoreMessages = detail.messages.size >= 50
                )
                subscribeTyping(detail.conversation.conversation.id)
            }.onFailure { error ->
                Log.e("AlumniChatVM", "Gagal membuka chat", error)
                setMessage(error.localizedMessage ?: "Gagal membuka percakapan.")
            }
        }
    }

    fun closeConversation() {
        requestedConversationId = null
        pendingOpenConversationId = null
        pendingDirectUserId = null
        typingUsers.clear()
        stopTypingChannel()
        val current = _uiState.value as? AlumniChatUiState.Ready ?: return
        _uiState.value = current.copy(selectedDetail = null, typingNames = emptyList())
    }

    fun startDirectChat(otherUserId: String) {
        val currentAccess = access ?: return
        if (otherUserId == currentAccess.userId) return
        viewModelScope.launch {
            runCatching {
                chatRepository.getOrCreateDirectConversation(currentAccess.userId, otherUserId)
            }.onSuccess { conversationId ->
                val current = _uiState.value as? AlumniChatUiState.Ready
                if (current != null) {
                    openConversation(conversationId)
                    loadConversations(currentAccess, keepCurrent = true, openConversationId = conversationId)
                } else {
                    pendingOpenConversationId = conversationId
                    loadConversations(currentAccess, keepCurrent = true, openConversationId = conversationId)
                }
            }.onFailure { error ->
                Log.e("AlumniChatVM", "Gagal membuat chat", error)
                setMessage(error.localizedMessage ?: "Gagal memulai chat.")
            }
        }
    }

    fun sendMessage(content: String) {
        val currentAccess = access ?: return
        val current = _uiState.value as? AlumniChatUiState.Ready ?: return
        val detail = current.selectedDetail ?: return
        val clean = content.trim()
        if (clean.isBlank()) return

        val optimistic = ChatMessageItem(
            message = ChatMessageDto(
                id = "local-${UUID.randomUUID()}",
                conversationId = detail.conversation.conversation.id,
                senderId = currentAccess.userId,
                content = clean,
                createdAt = Instant.now().toString()
            ),
            sender = currentAccess.alumniData,
            isMine = true,
            deliveryState = "sending"
        )
        _uiState.value = current.copy(
            isSending = true,
            selectedDetail = detail.copy(messages = detail.messages + optimistic)
        )

        viewModelScope.launch {
            sendTyping(false)
            runCatching {
                chatRepository.sendMessage(currentAccess.userId, detail.conversation.conversation.id, clean)
                chatRepository.getMessages(currentAccess.userId, detail.conversation.conversation.id)
            }.onSuccess { latest ->
                val conversations = chatRepository.getConversations(currentAccess.userId)
                val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onSuccess
                _uiState.value = state.copy(
                    conversations = conversations,
                    selectedDetail = mergeOutbox(latest, currentAccess),
                    isSending = false
                )
            }.onFailure { error ->
                Log.e("AlumniChatVM", "Gagal mengirim pesan", error)
                val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onFailure
                val queued = ChatOutboxMessage(
                    id = optimistic.message.id,
                    conversationId = detail.conversation.conversation.id,
                    content = clean,
                    createdAt = optimistic.message.createdAt,
                    lastError = error.localizedMessage
                )
                outboxStore.enqueue(queued)
                val refreshedDetail = state.selectedDetail?.let { mergeOutbox(it, currentAccess) }
                _uiState.value = state.copy(
                    isSending = false,
                    selectedDetail = refreshedDetail,
                    failedDraft = clean,
                    transientMessage = "Pesan belum terkirim. Tersimpan untuk dicoba lagi."
                )
            }
        }
    }

    fun retryFailedMessage() {
        val draft = (_uiState.value as? AlumniChatUiState.Ready)?.failedDraft ?: return
        clearFailedDraft()
        sendMessage(draft)
    }

    fun clearFailedDraft() {
        val current = _uiState.value as? AlumniChatUiState.Ready ?: return
        _uiState.value = current.copy(failedDraft = null)
    }

    fun loadOlderMessages() {
        val currentAccess = access ?: return
        val current = _uiState.value as? AlumniChatUiState.Ready ?: return
        val detail = current.selectedDetail ?: return
        if (current.isLoadingOlder || !current.hasMoreMessages) return
        val oldestServerMessage = detail.messages
            .filterNot { it.message.id.startsWith("local-") }
            .minByOrNull { it.message.createdAt }
            ?: return

        _uiState.value = current.copy(isLoadingOlder = true)
        viewModelScope.launch {
            runCatching {
                chatRepository.getMessages(
                    userId = currentAccess.userId,
                    conversationId = detail.conversation.conversation.id,
                    before = oldestServerMessage.message.createdAt,
                    limit = 50
                )
            }.onSuccess { older ->
                val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onSuccess
                val existing = state.selectedDetail ?: return@onSuccess
                val mergedMessages = (older.messages + existing.messages)
                    .distinctBy { it.message.id }
                    .sortedBy { it.message.createdAt }
                _uiState.value = state.copy(
                    selectedDetail = existing.copy(messages = mergedMessages),
                    isLoadingOlder = false,
                    hasMoreMessages = older.messages.size >= 50
                )
            }.onFailure { error ->
                val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onFailure
                _uiState.value = state.copy(
                    isLoadingOlder = false,
                    transientMessage = error.localizedMessage ?: "Gagal memuat pesan lama."
                )
            }
        }
    }

    fun retryOutboxMessage(outboxId: String) {
        val currentAccess = access ?: return
        val queued = outboxStore.getAll().firstOrNull { it.id == outboxId } ?: return
        viewModelScope.launch {
            runCatching {
                chatRepository.sendMessage(currentAccess.userId, queued.conversationId, queued.content)
                outboxStore.remove(queued.id)
                chatRepository.getMessages(currentAccess.userId, queued.conversationId)
            }.onSuccess { detail ->
                val conversations = chatRepository.getConversations(currentAccess.userId)
                val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onSuccess
                _uiState.value = state.copy(
                    conversations = conversations,
                    selectedDetail = mergeOutbox(detail, currentAccess),
                    failedDraft = null,
                    transientMessage = "Pesan terkirim."
                )
            }.onFailure { error ->
                outboxStore.enqueue(queued.copy(lastError = error.localizedMessage))
                val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onFailure
                _uiState.value = state.copy(transientMessage = error.localizedMessage ?: "Pesan masih gagal dikirim.")
            }
        }
    }

    fun archiveSelectedConversation() {
        val currentAccess = access ?: return
        val current = _uiState.value as? AlumniChatUiState.Ready ?: return
        val conversationId = current.selectedDetail?.conversation?.conversation?.id ?: return
        viewModelScope.launch {
            runCatching { chatRepository.archiveConversation(currentAccess.userId, conversationId, true) }
                .onSuccess {
                    closeConversation()
                    loadConversations(currentAccess, keepCurrent = true)
                }
                .onFailure { setMessage(it.localizedMessage ?: "Gagal mengarsipkan chat.") }
        }
    }

    fun toggleMuteSelectedConversation() {
        val currentAccess = access ?: return
        val current = _uiState.value as? AlumniChatUiState.Ready ?: return
        val selected = current.selectedDetail?.conversation ?: return
        val isMuted = selected.myParticipant?.mutedUntil != null
        viewModelScope.launch {
            runCatching {
                chatRepository.muteConversation(currentAccess.userId, selected.conversation.id, !isMuted)
                chatRepository.getMessages(currentAccess.userId, selected.conversation.id)
            }.onSuccess { detail ->
                val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onSuccess
                _uiState.value = state.copy(
                    selectedDetail = mergeOutbox(detail, currentAccess),
                    transientMessage = if (isMuted) "Notifikasi chat diaktifkan." else "Chat dibisukan."
                )
            }.onFailure { setMessage(it.localizedMessage ?: "Gagal mengubah mute chat.") }
        }
    }

    fun deleteMessage(messageId: String) {
        val currentAccess = access ?: return
        val current = _uiState.value as? AlumniChatUiState.Ready ?: return
        val conversationId = current.selectedDetail?.conversation?.conversation?.id ?: return
        viewModelScope.launch {
            runCatching {
                chatRepository.deleteOwnMessage(currentAccess.userId, messageId)
                chatRepository.getMessages(currentAccess.userId, conversationId)
            }.onSuccess { detail ->
                val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onSuccess
                _uiState.value = state.copy(selectedDetail = mergeOutbox(detail, currentAccess), transientMessage = "Pesan dihapus.")
            }.onFailure { setMessage(it.localizedMessage ?: "Gagal menghapus pesan.") }
        }
    }

    fun reportMessage(messageId: String, reason: String, note: String?) {
        val currentAccess = access ?: return
        val current = _uiState.value as? AlumniChatUiState.Ready ?: return
        val conversationId = current.selectedDetail?.conversation?.conversation?.id ?: return
        viewModelScope.launch {
            runCatching {
                chatRepository.reportMessage(
                    userId = currentAccess.userId,
                    messageId = messageId,
                    conversationId = conversationId,
                    reason = reason,
                    note = note
                )
            }.onSuccess {
                setMessage("Laporan chat terkirim. Admin akan meninjau pesan ini.")
            }.onFailure {
                setMessage(it.localizedMessage ?: "Gagal melaporkan pesan.")
            }
        }
    }

    fun blockCurrentChatUser() {
        val currentAccess = access ?: return
        val current = _uiState.value as? AlumniChatUiState.Ready ?: return
        val otherId = current.selectedDetail?.conversation?.otherParticipant?.id ?: return
        viewModelScope.launch {
            runCatching {
                chatRepository.blockUser(currentAccess.userId, otherId)
            }.onSuccess {
                closeConversation()
                loadConversations(currentAccess, keepCurrent = true)
                setMessage("Alumni diblokir dari chat.")
            }.onFailure {
                setMessage(it.localizedMessage ?: "Gagal memblokir alumni.")
            }
        }
    }

    fun onTypingChanged(text: String) {
        val hasText = text.isNotBlank()
        typingJob?.cancel()
        viewModelScope.launch { sendTyping(hasText) }
        if (hasText) {
            typingJob = viewModelScope.launch {
                delay(1800)
                sendTyping(false)
            }
        }
    }

    private fun observeAuth() {
        viewModelScope.launch {
            authRepository.getAuthState().collect { state ->
                when (state) {
                    AuthenticationState.NotAuthenticated -> {
                        stopRealtime()
                        stopTypingChannel()
                        stopPresence()
                        access = null
                        _uiState.value = AlumniChatUiState.LoginRequired
                    }
                    is AuthenticationState.Authenticated -> {
                        if (access?.userId == state.userId && _uiState.value is AlumniChatUiState.Ready) {
                            return@collect
                        }
                        loadAccess(state.userId)
                    }
                }
            }
        }
    }

    private fun loadAccess(userId: String) {
        viewModelScope.launch {
            val cachedAccess = alumniRepository.getCachedAccess(userId)
            if (cachedAccess?.canOpenForum == true) {
                access = cachedAccess
                val cachedConversations = chatRepository.getCachedConversations(userId).orEmpty()
                if (cachedConversations.isNotEmpty()) {
                    _uiState.value = AlumniChatUiState.Ready(
                        access = cachedAccess,
                        conversations = cachedConversations,
                        directory = emptyList(),
                        isRefreshing = true
                    )
                } else {
                    _uiState.value = AlumniChatUiState.Loading
                }
            } else {
                _uiState.value = AlumniChatUiState.Loading
            }
            runCatching { alumniRepository.getAccess(userId) }
                .onSuccess { loaded ->
                    access = loaded
                    if (!loaded.isActive || !loaded.canOpenForum) {
                        stopPresence()
                        _uiState.value = AlumniChatUiState.Locked("Chat alumni hanya untuk alumni terverifikasi.")
                    } else {
                        startRealtime(loaded)
                        startPresence(loaded)
                        loadConversations(loaded)
                    }
                }
                .onFailure { error ->
                    Log.e("AlumniChatVM", "Gagal memeriksa akses chat", error)
                    _uiState.value = AlumniChatUiState.Error(error.localizedMessage ?: "Gagal memuat chat alumni.")
                }
        }
    }

    private fun loadConversations(
        currentAccess: AlumniAccess,
        keepCurrent: Boolean = false,
        openConversationId: String? = null
    ) {
        viewModelScope.launch {
            val before = _uiState.value
            if (keepCurrent && before is AlumniChatUiState.Ready) {
                _uiState.value = before.copy(isRefreshing = true)
            }
            runCatching {
                val explicitTargetId = openConversationId ?: pendingOpenConversationId
                if (explicitTargetId != null) requestedConversationId = explicitTargetId
                val selectedId = explicitTargetId ?: requestedConversationId
                coroutineScope {
                    val conversationsDeferred = async { chatRepository.getConversations(currentAccess.userId) }
                    val directoryDeferred = async {
                        alumniRepository.getAlumniDirectoryItems()
                            .filter { it.alumni.id != currentAccess.userId }
                    }
                    val detailDeferred = selectedId?.let {
                        async { chatRepository.getMessages(currentAccess.userId, it) }
                    }

                    val conversations = conversationsDeferred.await()
                    val directory = directoryDeferred.await()
                    val detail = detailDeferred?.await()
                    val presenceIds = (conversations.mapNotNull { it.otherParticipant?.id } + directory.map { it.alumni.id }).distinct()
                    val presence = chatRepository.getPresence(presenceIds)
                    Triple(conversations, directory, detail) to presence
                }
            }.onSuccess { (loaded, presence) ->
                val (conversations, directory, detail) = loaded
                val targetId = openConversationId ?: pendingOpenConversationId ?: requestedConversationId
                pendingOpenConversationId = null
                val directUserId = pendingDirectUserId
                pendingDirectUserId = null
                val selectedDetail = if (!targetId.isNullOrBlank() && requestedConversationId == targetId) {
                    (detail?.takeIf { it.conversation.conversation.id == targetId }
                        ?: runCatching { chatRepository.getMessages(currentAccess.userId, targetId) }.getOrNull())
                        ?.let { mergeOutbox(it, currentAccess) }
                } else {
                    null
                }
                _uiState.value = AlumniChatUiState.Ready(
                    access = currentAccess,
                    conversations = conversations,
                    directory = directory,
                    selectedDetail = selectedDetail,
                    hasMoreMessages = (selectedDetail?.messages?.size ?: 0) >= 50,
                    presenceByUserId = presence
                )
                val latestDetail = (_uiState.value as? AlumniChatUiState.Ready)?.selectedDetail
                latestDetail?.let {
                    chatRepository.markRead(currentAccess.userId, it.conversation.conversation.id)
                    subscribeTyping(it.conversation.conversation.id)
                }
                if (!directUserId.isNullOrBlank() && targetId.isNullOrBlank()) {
                    startDirectChat(directUserId)
                }
            }.onFailure { error ->
                Log.e("AlumniChatVM", "Gagal memuat chat", error)
                if (keepCurrent && before is AlumniChatUiState.Ready) {
                    _uiState.value = before.copy(isRefreshing = false, transientMessage = error.localizedMessage ?: "Gagal memuat ulang chat.")
                } else {
                    _uiState.value = AlumniChatUiState.Error(error.localizedMessage ?: "Gagal memuat chat alumni.")
                }
            }
        }
    }

    private fun startRealtime(currentAccess: AlumniAccess) {
        if (realtimeChannel != null) return
        val channel = supabaseClient.channel("alumni-chat-db:${currentAccess.userId}:${System.identityHashCode(this)}")
        realtimeChannel = channel
        val conversationJob = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "chat_conversations" }
            .onEach { scheduleChatRefresh(currentAccess, detailsOnly = false) }
            .launchIn(viewModelScope)
        val participantJob = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "chat_participants" }
            .onEach { scheduleChatRefresh(currentAccess, detailsOnly = false) }
            .launchIn(viewModelScope)
        val messageJob = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "chat_messages" }
            .onEach { scheduleChatRefresh(currentAccess, detailsOnly = true) }
            .launchIn(viewModelScope)
        val blockJob = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "chat_blocks" }
            .onEach { scheduleChatRefresh(currentAccess, detailsOnly = false) }
            .launchIn(viewModelScope)
        val presenceJob = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "chat_user_presence" }
            .onEach { refreshPresence(currentAccess) }
            .launchIn(viewModelScope)
        realtimeJobs = listOf(conversationJob, participantJob, messageJob, blockJob, presenceJob)

        viewModelScope.launch {
            runCatching {
                supabaseClient.realtime.setAuth()
                channel.subscribe()
            }.onFailure { Log.e("AlumniChatVM", "Gagal subscribe realtime chat", it) }
        }
    }

    private fun scheduleChatRefresh(currentAccess: AlumniAccess, detailsOnly: Boolean) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            delay(180)
            val current = _uiState.value as? AlumniChatUiState.Ready
            val selectedId = current?.selectedDetail?.conversation?.conversation?.id
            if (detailsOnly && selectedId != null) {
                runCatching {
                    val detail = chatRepository.getMessages(currentAccess.userId, selectedId)
                    val conversations = chatRepository.getConversations(currentAccess.userId)
                    chatRepository.markRead(currentAccess.userId, selectedId)
                    detail to conversations
                }.onSuccess { (detail, conversations) ->
                    val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onSuccess
                    if (requestedConversationId != selectedId || state.selectedDetail?.conversation?.conversation?.id != selectedId) {
                        _uiState.value = state.copy(conversations = conversations, isRefreshing = false)
                        return@onSuccess
                    }
                    _uiState.value = state.copy(
                        selectedDetail = mergeOutbox(detail, currentAccess),
                        conversations = conversations,
                        isRefreshing = false
                    )
                }
            } else {
                loadConversations(currentAccess, keepCurrent = true)
            }
        }
    }

    private fun subscribeTyping(conversationId: String) {
        stopTypingChannel()
        val channel = supabaseClient.channel("alumni-chat-typing:$conversationId") {
            isPrivate = true
            broadcast {
                acknowledgeBroadcasts = true
            }
        }
        typingChannel = channel
        channel.broadcastFlow<ChatTypingPayload>("typing")
            .onEach { payload ->
                val currentAccess = access ?: return@onEach
                if (payload.userId == currentAccess.userId || payload.conversationId != conversationId) return@onEach
                if (payload.isTyping) typingUsers[payload.userId] = payload.name else typingUsers.remove(payload.userId)
                val state = _uiState.value as? AlumniChatUiState.Ready ?: return@onEach
                _uiState.value = state.copy(
                    typingNames = typingUsers.values.distinct(),
                    onlineNames = typingUsers.values.distinct()
                )
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            runCatching {
                supabaseClient.realtime.setAuth()
                channel.subscribe()
            }.onFailure { Log.e("AlumniChatVM", "Gagal subscribe typing", it) }
        }
    }

    private suspend fun sendTyping(isTyping: Boolean) {
        val currentAccess = access ?: return
        val detail = (_uiState.value as? AlumniChatUiState.Ready)?.selectedDetail ?: return
        val channel = typingChannel ?: return
        runCatching {
            channel.broadcast(
                event = "typing",
                message = ChatTypingPayload(
                    conversationId = detail.conversation.conversation.id,
                    userId = currentAccess.userId,
                    name = currentAccess.alumniData?.fullName ?: currentAccess.profileName ?: "Alumni",
                    isTyping = isTyping
                )
            )
        }
    }

    private fun mergeOutbox(detail: ChatDetail, currentAccess: AlumniAccess): ChatDetail {
        val queued = outboxStore.getConversation(detail.conversation.conversation.id).map { outbox ->
            ChatMessageItem(
                message = ChatMessageDto(
                    id = outbox.id,
                    conversationId = outbox.conversationId,
                    senderId = currentAccess.userId,
                    content = outbox.content,
                    createdAt = outbox.createdAt
                ),
                sender = currentAccess.alumniData,
                isMine = true,
                deliveryState = "failed"
            )
        }
        val serverMessages = detail.messages.filterNot { item ->
            item.message.id.startsWith("local-") && queued.any { it.message.id == item.message.id }
        }
        return detail.copy(
            messages = (serverMessages + queued)
                .distinctBy { it.message.id }
                .sortedBy { it.message.createdAt }
        )
    }

    private fun refreshPresence(currentAccess: AlumniAccess) {
        viewModelScope.launch {
            val state = _uiState.value as? AlumniChatUiState.Ready ?: return@launch
            val ids = (state.conversations.mapNotNull { it.otherParticipant?.id } + state.directory.map { it.alumni.id }).distinct()
            runCatching { chatRepository.getPresence(ids) }.onSuccess { presence ->
                val latest = _uiState.value as? AlumniChatUiState.Ready ?: return@onSuccess
                if (access?.userId == currentAccess.userId) {
                    _uiState.value = latest.copy(presenceByUserId = presence)
                }
            }
        }
    }

    private fun startPresence(currentAccess: AlumniAccess) {
        if (presenceJob != null) return
        presenceJob = viewModelScope.launch {
            while (true) {
                runCatching { chatRepository.setPresence(currentAccess.userId, true) }
                delay(30_000)
            }
        }
    }

    private fun setMessage(message: String) {
        val current = _uiState.value as? AlumniChatUiState.Ready ?: return
        _uiState.value = current.copy(transientMessage = message)
    }

    private fun stopRealtime() {
        val channel = realtimeChannel
        realtimeJob?.cancel()
        realtimeJobs.forEach { it.cancel() }
        refreshJob?.cancel()
        realtimeJob = null
        realtimeJobs = emptyList()
        refreshJob = null
        realtimeChannel = null
        if (channel != null) viewModelScope.launch { runCatching { supabaseClient.realtime.removeChannel(channel) } }
    }

    private fun stopTypingChannel() {
        val channel = typingChannel
        typingJob?.cancel()
        typingJob = null
        typingChannel = null
        typingUsers.clear()
        if (channel != null) viewModelScope.launch { runCatching { supabaseClient.realtime.removeChannel(channel) } }
    }

    private fun stopPresence() {
        val currentAccess = access
        presenceJob?.cancel()
        presenceJob = null
        if (currentAccess != null) {
            viewModelScope.launch { runCatching { chatRepository.setPresence(currentAccess.userId, false) } }
        }
    }

    override fun onCleared() {
        stopPresence()
        stopRealtime()
        stopTypingChannel()
        super.onCleared()
    }
}
