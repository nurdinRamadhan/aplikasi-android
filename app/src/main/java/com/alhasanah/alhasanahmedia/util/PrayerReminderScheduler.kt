package com.alhasanah.alhasanahmedia.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alhasanah.alhasanahmedia.MainActivity
import com.alhasanah.alhasanahmedia.R
import com.alhasanah.alhasanahmedia.data.model.prayer.PrayerScheduleEntry
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.prayerReminderDataStore by preferencesDataStore(name = "prayer_reminder_preferences")

enum class PrayerReminderMode(val label: String) {
    SILENT("Notifikasi"),
    VIBRATE("Getar"),
    RINGTONE("Dering"),
    ADHAN("Adzan")
}

data class PrayerReminderSettings(
    val enabled: Boolean = false,
    val mode: PrayerReminderMode = PrayerReminderMode.SILENT,
    val minutesBefore: Int = 0,
    val enabledPrayers: Set<String> = setOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")
)

class PrayerReminderScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val zoneId = ZoneId.of("Asia/Jakarta")
    private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val settingsFlow: Flow<PrayerReminderSettings> =
        appContext.prayerReminderDataStore.data.map { preferences ->
            PrayerReminderSettings(
                enabled = preferences[ENABLED] ?: false,
                mode = runCatching { PrayerReminderMode.valueOf(preferences[MODE] ?: PrayerReminderMode.SILENT.name) }
                    .getOrDefault(PrayerReminderMode.SILENT),
                minutesBefore = preferences[MINUTES_BEFORE] ?: 0,
                enabledPrayers = preferences[ENABLED_PRAYERS]
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.toSet()
                    ?: setOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")
            )
        }

    suspend fun saveSettings(settings: PrayerReminderSettings) {
        appContext.prayerReminderDataStore.edit { preferences ->
            preferences[ENABLED] = settings.enabled
            preferences[MODE] = settings.mode.name
            preferences[MINUTES_BEFORE] = settings.minutesBefore
            preferences[ENABLED_PRAYERS] = settings.enabledPrayers.joinToString(",")
        }
    }

    fun rescheduleToday(schedule: PrayerScheduleEntry, settings: PrayerReminderSettings, locationLabel: String) {
        cancelAll()
        schedulerScope.launch { saveScheduleSnapshot(schedule, locationLabel) }
        if (!settings.enabled) return

        prayerTimes(schedule)
            .filter { (name, _) -> name in settings.enabledPrayers }
            .forEach { (name, time) ->
                val trigger = parseToday(time)?.minusMinutes(settings.minutesBefore.toLong()) ?: return@forEach
                if (trigger.isBefore(LocalDateTime.now(zoneId).plusSeconds(20))) return@forEach

                val intent = Intent(appContext, PrayerReminderReceiver::class.java).apply {
                    putExtra(PrayerReminderReceiver.EXTRA_PRAYER_NAME, name)
                    putExtra(PrayerReminderReceiver.EXTRA_PRAYER_TIME, time)
                    putExtra(PrayerReminderReceiver.EXTRA_LOCATION, locationLabel)
                    putExtra(PrayerReminderReceiver.EXTRA_MODE, settings.mode.name)
                    putExtra(PrayerReminderReceiver.EXTRA_OFFSET, settings.minutesBefore)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    appContext,
                    requestCodeFor(name),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmClockInfo = AlarmManager.AlarmClockInfo(
                    trigger.atZone(zoneId).toInstant().toEpochMilli(),
                    openAppPendingIntent()
                )
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            }
    }

    suspend fun rescheduleSavedSnapshot() {
        val preferences = appContext.prayerReminderDataStore.data.first()
        val settings = PrayerReminderSettings(
            enabled = preferences[ENABLED] ?: false,
            mode = runCatching { PrayerReminderMode.valueOf(preferences[MODE] ?: PrayerReminderMode.SILENT.name) }
                .getOrDefault(PrayerReminderMode.SILENT),
            minutesBefore = preferences[MINUTES_BEFORE] ?: 0,
            enabledPrayers = preferences[ENABLED_PRAYERS]
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.toSet()
                ?: setOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")
        )
        if (!settings.enabled) {
            cancelAll()
            return
        }

        val schedule = PrayerScheduleEntry(
            subuh = preferences[SNAPSHOT_SUBUH].orEmpty(),
            dzuhur = preferences[SNAPSHOT_DZUHUR].orEmpty(),
            ashar = preferences[SNAPSHOT_ASHAR].orEmpty(),
            maghrib = preferences[SNAPSHOT_MAGHRIB].orEmpty(),
            isya = preferences[SNAPSHOT_ISYA].orEmpty()
        )
        if (prayerTimes(schedule).all { it.second.isBlank() }) return
        rescheduleToday(schedule, settings, preferences[SNAPSHOT_LOCATION].orEmpty())
    }

    fun cancelAll() {
        listOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya").forEach { name ->
            val intent = Intent(appContext, PrayerReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                requestCodeFor(name),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let { alarmManager.cancel(it) }
        }
    }

    private fun prayerTimes(schedule: PrayerScheduleEntry): List<Pair<String, String>> =
        listOf(
            "Subuh" to schedule.subuh,
            "Dzuhur" to schedule.dzuhur,
            "Ashar" to schedule.ashar,
            "Maghrib" to schedule.maghrib,
            "Isya" to schedule.isya
        )

    private fun parseToday(value: String): LocalDateTime? =
        runCatching {
            LocalDateTime.of(LocalDate.now(zoneId), LocalTime.parse(value.take(5), TIME_FORMATTER))
        }.getOrNull()

    private fun requestCodeFor(name: String): Int = 6200 + name.hashCode().mod(1000)

    private fun openAppPendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            appContext,
            9901,
            Intent(appContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private suspend fun saveScheduleSnapshot(schedule: PrayerScheduleEntry, locationLabel: String) {
        appContext.prayerReminderDataStore.edit { preferences ->
            preferences[SNAPSHOT_SUBUH] = schedule.subuh
            preferences[SNAPSHOT_DZUHUR] = schedule.dzuhur
            preferences[SNAPSHOT_ASHAR] = schedule.ashar
            preferences[SNAPSHOT_MAGHRIB] = schedule.maghrib
            preferences[SNAPSHOT_ISYA] = schedule.isya
            preferences[SNAPSHOT_LOCATION] = locationLabel
        }
    }

    private companion object {
        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val ENABLED = booleanPreferencesKey("enabled")
        val MODE = stringPreferencesKey("mode")
        val MINUTES_BEFORE = intPreferencesKey("minutes_before")
        val ENABLED_PRAYERS = stringPreferencesKey("enabled_prayers")
        val SNAPSHOT_SUBUH = stringPreferencesKey("snapshot_subuh")
        val SNAPSHOT_DZUHUR = stringPreferencesKey("snapshot_dzuhur")
        val SNAPSHOT_ASHAR = stringPreferencesKey("snapshot_ashar")
        val SNAPSHOT_MAGHRIB = stringPreferencesKey("snapshot_maghrib")
        val SNAPSHOT_ISYA = stringPreferencesKey("snapshot_isya")
        val SNAPSHOT_LOCATION = stringPreferencesKey("snapshot_location")
    }
}

class PrayerReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULE_ACTIONS) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                PrayerReminderScheduler(context).rescheduleSavedSnapshot()
            }
            pendingResult.finish()
        }
    }

    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED
        )
    }
}

class PrayerReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Sholat"
        val prayerTime = intent.getStringExtra(EXTRA_PRAYER_TIME).orEmpty()
        val location = intent.getStringExtra(EXTRA_LOCATION).orEmpty()
        val mode = runCatching { PrayerReminderMode.valueOf(intent.getStringExtra(EXTRA_MODE).orEmpty()) }
            .getOrDefault(PrayerReminderMode.SILENT)
        val offset = intent.getIntExtra(EXTRA_OFFSET, 0)
        val channelId = "prayer_reminder_${mode.name.lowercase()}"
        createChannel(context, channelId, mode)
        if (mode == PrayerReminderMode.VIBRATE) vibrate(context)

        val openIntent = PendingIntent.getActivity(
            context,
            9911,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (offset > 0) "$prayerName dalam $offset menit" else "Waktu $prayerName"
        val body = listOf(prayerTime, location)
            .filter { it.isNotBlank() }
            .joinToString(" • ")
            .ifBlank { "Pengingat waktu sholat" }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching {
            NotificationManagerCompat.from(context).notify(7400 + prayerName.hashCode().mod(1000), notification)
        }
    }

    private fun createChannel(context: Context, channelId: String, mode: PrayerReminderMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(channelId) != null) return
        val channel = NotificationChannel(
            channelId,
            "Pengingat Sholat - ${mode.label}",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Pengingat waktu sholat sesuai pengaturan pengguna"
            when (mode) {
                PrayerReminderMode.SILENT -> {
                    setSound(null, null)
                    enableVibration(false)
                }
                PrayerReminderMode.VIBRATE -> {
                    setSound(null, null)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 450, 180, 450)
                }
                PrayerReminderMode.RINGTONE,
                PrayerReminderMode.ADHAN -> {
                    val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    val attributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setSound(sound, attributes)
                    enableVibration(mode == PrayerReminderMode.ADHAN)
                }
            }
        }
        manager.createNotificationChannel(channel)
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 450, 180, 450), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 450, 180, 450), -1)
        }
    }

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_PRAYER_TIME = "extra_prayer_time"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_OFFSET = "extra_offset"
    }
}
