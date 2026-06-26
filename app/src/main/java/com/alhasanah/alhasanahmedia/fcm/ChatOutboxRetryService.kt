package com.alhasanah.alhasanahmedia.fcm

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.alhasanah.alhasanahmedia.data.repository.AuthRepository
import com.alhasanah.alhasanahmedia.data.repository.ChatOutboxStore
import com.alhasanah.alhasanahmedia.data.repository.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ChatOutboxRetryService : JobService(), KoinComponent {
    private val authRepository: AuthRepository by inject()
    private val chatRepository: ChatRepository by inject()
    private val outboxStore: ChatOutboxStore by inject()
    private var scope: CoroutineScope? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val jobScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = jobScope
        jobScope.launch {
            val needsReschedule = runCatching {
                outboxStore.getConversations().forEach { conversationId ->
                    ChatOutboxFlush.flushConversation(conversationId, authRepository, chatRepository, outboxStore)
                }
                outboxStore.getAll().isNotEmpty()
            }.getOrElse { error ->
                Log.e(TAG, "Chat outbox retry failed", error)
                true
            }
            jobFinished(params, needsReschedule)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        scope?.cancel()
        scope = null
        return true
    }

    companion object {
        private const val TAG = "ChatOutboxRetry"
        private const val JOB_ID = 4107

        fun schedule(context: Context) {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val component = ComponentName(context, ChatOutboxRetryService::class.java)
            val jobInfo = JobInfo.Builder(JOB_ID, component)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setBackoffCriteria(30_000L, JobInfo.BACKOFF_POLICY_LINEAR)
                .setMinimumLatency(1_000L)
                .build()
            scheduler.schedule(jobInfo)
        }
    }
}
