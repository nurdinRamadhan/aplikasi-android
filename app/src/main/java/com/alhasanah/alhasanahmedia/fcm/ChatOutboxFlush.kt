package com.alhasanah.alhasanahmedia.fcm

import android.util.Log
import com.alhasanah.alhasanahmedia.data.repository.AuthRepository
import com.alhasanah.alhasanahmedia.data.repository.ChatOutboxStore
import com.alhasanah.alhasanahmedia.data.repository.ChatRepository
import kotlinx.coroutines.flow.firstOrNull

object ChatOutboxFlush {
    suspend fun flushConversation(
        conversationId: String,
        authRepository: AuthRepository,
        chatRepository: ChatRepository,
        outboxStore: ChatOutboxStore
    ) {
        val userId = authRepository.getCurrentUser().firstOrNull()?.id ?: return
        val queued = outboxStore.getConversation(conversationId)
        for (message in queued) {
            runCatching {
                chatRepository.sendMessage(userId, message.conversationId, message.content)
                outboxStore.remove(message.id)
            }.onFailure { error ->
                Log.d(TAG, "Outbox flush paused for conversation $conversationId", error)
                outboxStore.enqueue(message.copy(lastError = error.localizedMessage))
                return
            }
        }
    }

    private const val TAG = "ChatOutboxFlush"
}
