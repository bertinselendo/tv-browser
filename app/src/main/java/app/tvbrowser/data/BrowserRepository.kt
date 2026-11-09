package app.tvbrowser.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.browserDataStore: DataStore<Preferences> by preferencesDataStore(name = "browser")

data class BrowserState(
    val zoomPercent: Int = 100,
    val layoutMode: LayoutMode = LayoutMode.SINGLE,
    val activePane: Int = 0,
    val urls: List<String> = List(4) { "" },
)

class BrowserRepository(private val context: Context) {

    val state: Flow<BrowserState> = context.browserDataStore.data.map { prefs ->
        BrowserState(
            zoomPercent = prefs[KEY_ZOOM] ?: 100,
            layoutMode = LayoutMode.fromStorage(prefs[KEY_LAYOUT]),
            activePane = (prefs[KEY_ACTIVE_PANE] ?: 0).coerceIn(0, 3),
            urls = List(4) { index -> prefs[urlKey(index)] ?: "" },
        )
    }

    suspend fun setZoomPercent(percent: Int) {
        context.browserDataStore.edit { it[KEY_ZOOM] = percent.coerceIn(50, 200) }
    }

    suspend fun setLayoutMode(mode: LayoutMode) {
        context.browserDataStore.edit { it[KEY_LAYOUT] = mode.storageKey }
    }

    suspend fun setActivePane(pane: Int) {
        context.browserDataStore.edit { it[KEY_ACTIVE_PANE] = pane.coerceIn(0, 3) }
    }

    suspend fun setPaneUrl(pane: Int, url: String) {
        context.browserDataStore.edit { it[urlKey(pane)] = url }
    }

    suspend fun replaceUrls(urls: List<String>) {
        require(urls.size == 4)
        context.browserDataStore.edit { prefs ->
            urls.forEachIndexed { index, url -> prefs[urlKey(index)] = url }
        }
    }

    companion object {
        private val KEY_ZOOM = intPreferencesKey("zoom_percent")
        private val KEY_LAYOUT = stringPreferencesKey("layout_mode")
        private val KEY_ACTIVE_PANE = intPreferencesKey("active_pane")

        private fun urlKey(index: Int) = stringPreferencesKey("url_$index")
    }
}
