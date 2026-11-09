package app.tvbrowser

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.tvbrowser.browser.UrlNormalizer
import app.tvbrowser.browser.Zoom
import app.tvbrowser.data.BrowserRepository
import app.tvbrowser.data.BrowserState
import app.tvbrowser.data.LayoutMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BrowserRepository(application)

    private val _uiState = MutableStateFlow(BrowserState())
    val uiState: StateFlow<BrowserState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.state.take(1).collect { stored ->
                _uiState.value = stored.copy(
                    urls = stored.urls.map { url ->
                        if (url.isBlank()) UrlNormalizer.EMPTY_ASSET else url
                    },
                )
            }
        }
    }

    fun activeUrlForBar(): String {
        val pane = _uiState.value.activePane
        return UrlNormalizer.displayUrl(_uiState.value.urls[pane])
    }

    fun setActivePane(pane: Int) {
        _uiState.update { it.copy(activePane = pane.coerceIn(0, 3)) }
        persist { repository.setActivePane(pane) }
    }

    fun toggleLayoutMode(requested: LayoutMode) {
        val current = _uiState.value.layoutMode
        val next = if (current == requested) LayoutMode.SINGLE else requested
        _uiState.update { it.copy(layoutMode = next) }
        persist { repository.setLayoutMode(next) }
    }

    fun adjustZoom(delta: Int) {
        val next = Zoom.step(_uiState.value.zoomPercent, delta)
        _uiState.update { it.copy(zoomPercent = next) }
        persist { repository.setZoomPercent(next) }
    }

    fun resetZoom() {
        _uiState.update { it.copy(zoomPercent = 100) }
        persist { repository.setZoomPercent(100) }
    }

    fun loadUrlOnActivePane(raw: String): Boolean {
        val normalized = UrlNormalizer.normalize(raw) ?: return false
        val pane = _uiState.value.activePane
        updatePaneUrl(pane, normalized)
        return true
    }

    fun updatePaneUrl(pane: Int, url: String) {
        _uiState.update { state ->
            val urls = state.urls.toMutableList()
            urls[pane] = url
            state.copy(urls = urls)
        }
        persist {
            repository.setPaneUrl(pane, if (url == UrlNormalizer.EMPTY_ASSET) "" else url)
        }
    }

    fun paneUrl(pane: Int): String {
        val url = _uiState.value.urls[pane]
        return if (url.isBlank()) UrlNormalizer.EMPTY_ASSET else url
    }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
