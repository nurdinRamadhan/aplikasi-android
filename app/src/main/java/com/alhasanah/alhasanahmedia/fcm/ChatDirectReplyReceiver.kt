package com.alhasanah.alhasanahmedia.fcm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.alhasanah.alhasanahmedia.data.model.ChatOutboxMessage
import com.alhasanah.alhasanahmedia.data.repository.AuthRepository
import com.alhasanah.alhasanahmedia.data.repository.ChatOutboxStore
import com.alhasanah.alhasanahmedia.data.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.util.UUID

class ChatDirectReplyReceiver : BroadcastReceiver(), KoinComponent {
    private val authRepository: AuthRepository by inject()
    private val chatRepository: ChatRepository by inject()
    private val outboxStore: ChatOutboxStore by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                handleReply(context, intent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleReply(context: Context, intent: Intent) {
        val conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)?.takeIf { it.isNotBlank() } ?: return
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, conversationId.hashCode())
        val reply = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_TEXT_REPLY)
            ?.toString()
            ?.trim()
            .orEmpty()
        if (reply.isBlank()) return

        val userId = authRepository.getCurrentUser().firstOrNull()?.id
        if (userId.isNullOrBlank()) {
            Log.w(TAG, "Direct reply ignored: user is not authenticated")
            return
        }

        runCatching {
            chatRepository.sendMessage(userId, conversationId, reply)
            ChatOutboxFlush.flushConversation(conversationId, authRepository, chatRepository, outboxStore)
        }.onSuccess {
            context.cancelNotification(notificationId)
        }.onFailure { error ->
            Log.e(TAG, "Direct reply queued because E2EE send failed", error)
            outboxStore.enqueue(
                ChatOutboxMessage(
                    id = "local-${UUID.randomUUID()}",
                    conversationId = conversationId,
                    content = reply,
                    createdAt = Instant.now().toString(),
                    lastError = error.localizedMessage
                )
            )
            ChatOutboxRetryService.schedule(context)
        }
    }

    private fun Context.cancelNotification(notificationId: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)
    }

    companion object {
        const val ACTION_DIRECT_REPLY = "com.alhasanah.alhasanahmedia.action.CHAT_DIRECT_REPLY"
        const val KEY_TEXT_REPLY = "chat_direct_reply_text"
        const val EXTRA_CONVERSATION_ID = "extra_conversation_id"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        private const val TAG = "ChatDirectReply"
    }
}
