package com.alhasanah.alhasanahmedia.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.announcementDataStore: DataStore<Preferences> by preferencesDataStore(name = "announcement_prefs")

class AnnouncementPreferences(private val context: Context) {

    companion object {
        private const val KEY_LAST_SHOWN_DATE = "announcement_last_shown_date"
    }

    private val lastShownDateKey = stringPreferencesKey(KEY_LAST_SHOWN_DATE)

    /**
     * Check if announcements have been shown today.
     * Returns true if the last shown date is today.
     */
    suspend fun hasShownToday(): Boolean {
        val lastDate = context.announcementDataStore.data.map { it[lastShownDateKey] }.first()
        val today = LocalDate.now().toString()
        return lastDate == today
    }

    /**
     * Mark announcements as shown for today.
     */
    suspend fun markShownToday() {
        context.announcementDataStore.edit {
            it[lastShownDateKey] = LocalDate.now().toString()
        }
    }
}
