package com.alhasanah.alhasanahmedia.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.alhasanah.alhasanahmedia.MainActivity
import com.alhasanah.alhasanahmedia.R
import com.alhasanah.alhasanahmedia.data.repository.AuthRepository
import com.alhasanah.alhasanahmedia.data.repository.ChatOutboxStore
import com.alhasanah.alhasanahmedia.data.repository.ChatRepository
import com.alhasanah.alhasanahmedia.data.repository.NotificationRepository
import com.alhasanah.alhasanahmedia.fcm.ChatDirectReplyReceiver.Companion.ACTION_DIRECT_REPLY
import com.alhasanah.alhasanahmedia.fcm.ChatDirectReplyReceiver.Companion.EXTRA_CONVERSATION_ID
import com.alhasanah.alhasanahmedia.fcm.ChatDirectReplyReceiver.Companion.EXTRA_NOTIFICATION_ID
import com.alhasanah.alhasanahmedia.fcm.ChatDirectReplyReceiver.Companion.KEY_TEXT_REPLY
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val auth: Auth by inject()
    private val authRepository: AuthRepository by inject()
    private val notificationRepository: NotificationRepository by inject()
    private val chatRepository: ChatRepository by inject()
    private val chatOutboxStore: ChatOutboxStore by inject()
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Save token locally — will be registered when user session is available
        savePendingToken(token)
        sendTokenToBackend(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val targetUserId = remoteMessage.data["user_id"]
        val currentUserId = auth.currentUserOrNull()?.id

        // Firebase service bisa aktif sebelum session Supabase selesai dipulihkan.
        // Jika currentUserId belum tersedia, tetap tampilkan notifikasi dari FCM token device ini.
        if (targetUserId == null || currentUserId == null || targetUserId == currentUserId) {
            remoteMessage.notification?.let {
                showNotification(it.title ?: "Alhasanah Media", it.body ?: "", remoteMessage.data)
            } ?: run {
                // If only data payload
                val title = remoteMessage.data["title"] ?: "Alhasanah Media"
                val body = remoteMessage.data["body"] ?: ""
                showNotification(title, body, remoteMessage.data)
            }
        } else {
            Log.d(TAG, "Notification ignored for inactive account")
        }
    }

    private fun savePendingToken(token: String) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING_TOKEN, token)
            .apply()
    }

    private fun sendTokenToBackend(token: String) {
        val currentUser = auth.currentUserOrNull()
        if (currentUser != null) {
            scope.launch {
                try {
                    notificationRepository.registerMyFcmDevice(
                        token = token,
                        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID).orEmpty(),
                        appInstanceId = null
                    )
                    // Clear pending token on success
                    clearPendingToken()
                    Log.d(TAG, "FCM token registered")
                } catch (e: Exception) {
                    Log.e(TAG, "Error registering FCM token, will retry on next login")
                }
            }
        } else {
            Log.d(TAG, "No user logged in, token saved as pending")
        }
    }

    private fun clearPendingToken() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .remove(KEY_PENDING_TOKEN)
            .apply()
    }

    companion object {
        private const val TAG = "MyFCMService"
        private const val GENERAL_CHANNEL_ID = "alhasanah_notif_channel"
        private const val CHAT_CHANNEL_ID = "alhasanah_chat_channel"
        const val PREFS_NAME = "fcm_pending"
        const val KEY_PENDING_TOKEN = "pending_token"

        /** Called on login to register any pending token. */
        fun registerPendingToken(context: Context, notificationRepository: NotificationRepository) {
            val pending = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_PENDING_TOKEN, null)
            if (pending.isNullOrBlank()) return

            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                try {
                    notificationRepository.registerMyFcmDevice(
                        token = pending,
                        deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty(),
                        appInstanceId = null
                    )
                    context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .remove(KEY_PENDING_TOKEN)
                        .apply()
                    Log.d(TAG, "Pending FCM token registered on login")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to register pending token", e)
                }
            }
        }
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val type = data["type"]
        val conversationId = data["conversation_id"]
        val isChatNotification = type == "alumni_chat_message" && !conversationId.isNullOrBlank()
        val safeBody = if (isChatNotification) "Pesan terenkripsi baru" else body
        val channelId = if (type == "alumni_chat_message" && !conversationId.isNullOrBlank()) {
            CHAT_CHANNEL_ID
        } else {
            GENERAL_CHANNEL_ID
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    GENERAL_CHANNEL_ID,
                    "Alhasanah Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi Penting Alhasanah"
                    enableLights(true)
                    enableVibration(true)
                }
            )
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHAT_CHANNEL_ID,
                    "Chat Alumni",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi chat alumni terenkripsi"
                    enableLights(true)
                    enableVibration(true)
                }
            )
        }

        val id = data["id"] // ID related to the notification (e.g., tagihan ID or violation ID)
        val nis = data["nis"] ?: data["santri_nis"] // Santri NIS if applicable
        val threadId = data["thread_id"]
        val commentId = data["comment_id"]
        val reportId = data["report_id"]
        val walletLedgerId = data["ledger_id"] ?: data["wallet_ledger_id"]

        // Build Intent for deep linking or simple open
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Put data for navigation in MainActivity/Compose
            putExtra("notif_type", type)
            putExtra("notif_id", id)
            putExtra("notif_nis", nis)
            putExtra("notif_thread_id", threadId)
            putExtra("notif_comment_id", commentId)
            putExtra("notif_report_id", reportId)
            putExtra("notif_conversation_id", conversationId)
            putExtra("notif_wallet_ledger_id", walletLedgerId)
        }

        // Use type + id for unique notificationId to avoid collision
        val notifKey = "${type ?: ""}_${id ?: ""}_${conversationId ?: ""}"
        val notificationId = notifKey.hashCode()

        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(safeBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        if (isChatNotification) {
            scope.launch {
                ChatOutboxFlush.flushConversation(conversationId, authRepository, chatRepository, chatOutboxStore)
                if (chatOutboxStore.getAll().isNotEmpty()) {
                    ChatOutboxRetryService.schedule(this@MyFirebaseMessagingService)
                }
            }
            notificationBuilder
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setStyle(
                    NotificationCompat.MessagingStyle(
                        Person.Builder()
                            .setName(auth.currentUserOrNull()?.email ?: "Saya")
                            .build()
                    )
                        .setConversationTitle(title)
                        .addMessage(
                            safeBody,
                            System.currentTimeMillis(),
                            Person.Builder().setName(title.ifBlank { "Alumni" }).build()
                        )
                )
                .addAction(buildDirectReplyAction(conversationId, notificationId))
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun buildDirectReplyAction(conversationId: String, notificationId: Int): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Balas pesan")
            .build()

        val replyIntent = Intent(this, ChatDirectReplyReceiver::class.java).apply {
            action = ACTION_DIRECT_REPLY
            putExtra(EXTRA_CONVERSATION_ID, conversationId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val replyPendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId,
            replyIntent,
            flags
        )

        return NotificationCompat.Action.Builder(
            R.drawable.logo,
            "Balas",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(false)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
