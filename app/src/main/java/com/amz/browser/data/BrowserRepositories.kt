package com.amz.browser.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.amz.browser.search.SearchEngine
import com.amz.browser.ui.theme.AnimationSpeed
import com.amz.browser.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.amzDataStore by preferencesDataStore(name = "amz_browser_prefs")

@Serializable
data class BookmarkEntry(val title: String, val url: String, val createdAt: Long)

@Serializable
data class HistoryEntry(val title: String, val url: String, val visitedAt: Long)

@Serializable
data class PersistedTab(val id: String, val title: String, val url: String)

data class AppSettings(
    val userName: String = "",
    val setupDone: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val accentHex: Long = 0xFF0A84FFL,
    val glassBlur: Boolean = true,
    val gyroTilt: Boolean = true,
    val animSpeed: AnimationSpeed = AnimationSpeed.FLUID,
    val adShield: Boolean = true,
    val httpsOnly: Boolean = true,
    val searchEngine: SearchEngine = SearchEngine.GOOGLE,
    val textZoom: Int = 100,
    val cardResultsBeta: Boolean = false
)

data class ShieldStats(
    val adsBlocked: Int = 0,
    val popupsStopped: Int = 0,
    val redirectsStopped: Int = 0
)

class SettingsRepo(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val SETUP_DONE = booleanPreferencesKey("setup_done")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_HEX = longPreferencesKey("accent_hex")
        val GLASS_BLUR = booleanPreferencesKey("glass_blur")
        val GYRO_TILT = booleanPreferencesKey("gyro_tilt")
        val ANIM_SPEED = stringPreferencesKey("anim_speed")
        val AD_SHIELD = booleanPreferencesKey("ad_shield")
        val HTTPS_ONLY = booleanPreferencesKey("https_only")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val TEXT_ZOOM = intPreferencesKey("text_zoom")
        val CARD_BETA = booleanPreferencesKey("card_results_beta")
        val ADS_BLOCKED = intPreferencesKey("ads_blocked")
        val POPUPS_STOPPED = intPreferencesKey("popups_stopped")
        val REDIRECTS_STOPPED = intPreferencesKey("redirects_stopped")
        val BOOKMARKS_JSON = stringPreferencesKey("bookmarks_json")
        val HISTORY_JSON = stringPreferencesKey("history_json")
        val TABS_JSON = stringPreferencesKey("tabs_json")
    }

    val settings: Flow<AppSettings> = context.amzDataStore.data.map { p ->
        AppSettings(
            userName = p[USER_NAME].orEmpty(),
            setupDone = p[SETUP_DONE] ?: false,
            themeMode = runCatching { ThemeMode.valueOf(p[THEME_MODE] ?: "AUTO") }.getOrDefault(ThemeMode.AUTO),
            accentHex = p[ACCENT_HEX] ?: 0xFF0A84FFL,
            glassBlur = p[GLASS_BLUR] ?: true,
            gyroTilt = p[GYRO_TILT] ?: true,
            animSpeed = runCatching { AnimationSpeed.valueOf(p[ANIM_SPEED] ?: "FLUID") }.getOrDefault(AnimationSpeed.FLUID),
            adShield = p[AD_SHIELD] ?: true,
            httpsOnly = p[HTTPS_ONLY] ?: true,
            searchEngine = runCatching { SearchEngine.valueOf(p[SEARCH_ENGINE] ?: "GOOGLE") }.getOrDefault(SearchEngine.GOOGLE),
            textZoom = p[TEXT_ZOOM] ?: 100,
            cardResultsBeta = p[CARD_BETA] ?: false
        )
    }.distinctUntilChanged()

    val shieldStats: Flow<ShieldStats> = context.amzDataStore.data.map { p ->
        ShieldStats(
            adsBlocked = p[ADS_BLOCKED] ?: 0,
            popupsStopped = p[POPUPS_STOPPED] ?: 0,
            redirectsStopped = p[REDIRECTS_STOPPED] ?: 0
        )
    }.distinctUntilChanged()

    val bookmarks: Flow<List<BookmarkEntry>> = context.amzDataStore.data.map { prefs ->
        prefs[BOOKMARKS_JSON]?.let { runCatching { json.decodeFromString<List<BookmarkEntry>>(it) }.getOrNull() }
            ?: emptyList()
    }.distinctUntilChanged()

    val history: Flow<List<HistoryEntry>> = context.amzDataStore.data.map { prefs ->
        prefs[HISTORY_JSON]?.let { runCatching { json.decodeFromString<List<HistoryEntry>>(it) }.getOrNull() }
            ?: emptyList()
    }.distinctUntilChanged()

    val persistedTabs: Flow<List<PersistedTab>> = context.amzDataStore.data.map { prefs ->
        prefs[TABS_JSON]?.let { runCatching { json.decodeFromString<List<PersistedTab>>(it) }.getOrNull() }
            ?: emptyList()
    }.distinctUntilChanged()

    suspend fun update(transform: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.amzDataStore.edit(transform)
    }

    suspend fun toggleBookmark(title: String, url: String): Boolean {
        var added = false
        context.amzDataStore.edit { prefs ->
            val current = prefs[BOOKMARKS_JSON]
                ?.let { runCatching { json.decodeFromString<List<BookmarkEntry>>(it) }.getOrNull() }
                .orEmpty()
            val exists = current.any { it.url == url }
            val next = if (exists) {
                added = false
                current.filterNot { it.url == url }
            } else {
                added = true
                listOf(BookmarkEntry(title, url, System.currentTimeMillis())) + current
            }
            prefs[BOOKMARKS_JSON] = json.encodeToString(next)
        }
        return added
    }

    suspend fun clearBookmarks() {
        context.amzDataStore.edit { it[BOOKMARKS_JSON] = "[]" }
    }

    suspend fun recordHistory(title: String, url: String) {
        context.amzDataStore.edit { prefs ->
            val current = prefs[HISTORY_JSON]
                ?.let { runCatching { json.decodeFromString<List<HistoryEntry>>(it) }.getOrNull() }
                .orEmpty()
                .filterNot { it.url == url }
            val updated = (listOf(HistoryEntry(title, url, System.currentTimeMillis())) + current).take(500)
            prefs[HISTORY_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun clearHistory() {
        context.amzDataStore.edit { it[HISTORY_JSON] = "[]" }
    }

    suspend fun saveTabs(tabs: List<PersistedTab>) {
        context.amzDataStore.edit { prefs ->
            prefs[TABS_JSON] = json.encodeToString(tabs)
        }
    }

    suspend fun incrementShieldCounters(ads: Int = 0, popups: Int = 0, redirects: Int = 0) {
        context.amzDataStore.edit { prefs ->
            prefs[ADS_BLOCKED] = (prefs[ADS_BLOCKED] ?: 0) + ads
            prefs[POPUPS_STOPPED] = (prefs[POPUPS_STOPPED] ?: 0) + popups
            prefs[REDIRECTS_STOPPED] = (prefs[REDIRECTS_STOPPED] ?: 0) + redirects
        }
    }
}
