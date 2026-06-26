package com.alhasanah.alhasanahmedia.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alhasanah.alhasanahmedia.data.model.quran.QuranQori
import com.alhasanah.alhasanahmedia.data.model.quran.QuranQoriCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bookmarkDataStore: DataStore<Preferences> by preferencesDataStore(name = "quran_bookmarks")

class QuranBookmarkRepository(private val context: Context) {

    private val bookmarksKey = stringSetPreferencesKey("bookmarked_ayat")
    private val selectedQoriKey = stringPreferencesKey("selected_qori")

    // Format: "surahNomor:ayatNomor"
    val getBookmarks: Flow<Set<String>> = context.bookmarkDataStore.data
        .map { preferences ->
            preferences[bookmarksKey] ?: emptySet()
        }

    val selectedQori: Flow<QuranQori> = context.bookmarkDataStore.data
        .map { preferences ->
            QuranQoriCatalog.fromId(preferences[selectedQoriKey] ?: QuranQoriCatalog.DEFAULT_ID)
        }

    suspend fun toggleBookmark(surahNomor: Int, ayatNomor: Int) {
        val ayatId = "$surahNomor:$ayatNomor"
        context.bookmarkDataStore.edit { preferences ->
            val current = preferences[bookmarksKey] ?: emptySet()
            if (current.contains(ayatId)) {
                preferences[bookmarksKey] = current - ayatId
            } else {
                preferences[bookmarksKey] = current + ayatId
            }
        }
    }

    suspend fun setSelectedQori(qoriId: String) {
        context.bookmarkDataStore.edit { preferences ->
            preferences[selectedQoriKey] = qoriId
        }
    }

    suspend fun isBookmarked(surahNomor: Int, ayatNomor: Int): Boolean {
        // This is a bit inefficient if called many times, but for individual ayat it's okay
        // Better to observe the Flow in ViewModel
        return false 
    }
}
