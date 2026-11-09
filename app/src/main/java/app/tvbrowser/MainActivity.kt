package app.tvbrowser

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.tvbrowser.browser.UrlNormalizer
import app.tvbrowser.browser.Zoom
import app.tvbrowser.data.LayoutMode
import app.tvbrowser.databinding.ActivityBrowserBinding
import app.tvbrowser.ui.PaneGrid
import app.tvbrowser.ui.PaneSlot
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: BrowserViewModel by viewModels()
    private lateinit var binding: ActivityBrowserBinding
    private var fullscreen = false
    private val handler = Handler(Looper.getMainLooper())
    private var longPressDownAt = 0L
    private var longPressTriggered = false
    private var pendingLongPressPane: Int? = null
    private var lastRendered: app.tvbrowser.data.BrowserState? = null

    private val paneDelegate = object : PaneSlot.Delegate {
        override fun onPaneUrlChanged(pane: Int, url: String) {
            viewModel.updatePaneUrl(pane, url)
            if (pane == viewModel.uiState.value.activePane) {
                binding.urlInput.setText(UrlNormalizer.displayUrl(url))
            }
        }

        override fun onPaneActivated(pane: Int) {
            viewModel.setActivePane(pane)
        }

        override fun onEnterWebContent(pane: Int) {
            binding.paneGrid.onWebContentEntered(pane)
        }

        override fun onLeaveWebContent(pane: Int) {
            binding.paneGrid.onWebContentLeft()
        }

        override fun onLongOkInWebContent(pane: Int) {
            viewModel.setActivePane(pane)
            binding.urlInput.requestFocus()
        }

        override fun getZoomPercent(): Int = viewModel.uiState.value.zoomPercent

        override fun onRenderProcessGone(pane: Int): Boolean {
            val url = viewModel.paneUrl(pane)
            binding.paneGrid.paneSlot(pane).destroyWebView()
            binding.paneGrid.paneSlot(pane).loadUrl(url, this@MainActivity)
            return true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.paneGrid.attach(paneDelegate)
        wireToolbar()
        wireToolbarFocusDown()

        onBackPressedDispatcher.addCallback(this) {
            if (fullscreen) {
                toggleFullscreen()
                return@addCallback
            }
            if (binding.paneGrid.exitWebContentIfNeeded()) return@addCallback
            finish()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: app.tvbrowser.data.BrowserState) {
        val prev = lastRendered
        if (prev?.zoomPercent != state.zoomPercent) {
            binding.btnZoomLevel.text = getString(R.string.zoom_percent, state.zoomPercent)
            binding.paneGrid.forEachVisible { it.applyZoom(state.zoomPercent) }
        }
        if (prev?.layoutMode != state.layoutMode) {
            binding.paneGrid.setLayoutMode(state.layoutMode)
            syncVisiblePanes(state)
        }
        if (prev?.activePane != state.activePane) {
            binding.paneGrid.setActivePane(state.activePane)
            binding.urlInput.setText(viewModel.activeUrlForBar())
        }
        if (prev == null) {
            syncVisiblePanes(state)
        } else if (prev.urls != state.urls) {
            state.urls.forEachIndexed { index, url ->
                val slot = binding.paneGrid.paneSlot(index)
                val target = if (url.isBlank()) UrlNormalizer.EMPTY_ASSET else url
                slot.restoreStoredUrl(target)
                if (index in binding.paneGrid.visiblePaneIndices() && slot.loadedUrl() != target) {
                    slot.ensureWebView(this)
                    slot.loadUrl(target, this)
                }
            }
        }
        if (prev?.layoutMode != state.layoutMode || prev?.activePane != state.activePane) {
            wireToolbarFocusDown()
        }
        lastRendered = state
    }

    private fun syncVisiblePanes(state: app.tvbrowser.data.BrowserState) {
        binding.paneGrid.visiblePaneIndices().forEach { index ->
            val slot = binding.paneGrid.paneSlot(index)
            val target = state.urls[index].ifBlank { UrlNormalizer.EMPTY_ASSET }
            slot.restoreStoredUrl(target)
            slot.ensureWebView(this)
            if (slot.getWebView() == null || slot.loadedUrl() != target) {
                slot.loadUrl(target, this)
            }
            slot.applyZoom(state.zoomPercent)
        }
    }

    private fun wireToolbar() {
        binding.btnBack.setOnClickListener {
            binding.paneGrid.paneSlot(viewModel.uiState.value.activePane).goBack()
        }
        binding.btnForward.setOnClickListener {
            binding.paneGrid.paneSlot(viewModel.uiState.value.activePane).goForward()
        }
        binding.btnReload.setOnClickListener {
            binding.paneGrid.paneSlot(viewModel.uiState.value.activePane).reload()
        }
        binding.btnZoomOut.setOnClickListener {
            viewModel.adjustZoom(-Zoom.STEP)
            applyZoomToVisible()
        }
        binding.btnZoomIn.setOnClickListener {
            viewModel.adjustZoom(Zoom.STEP)
            applyZoomToVisible()
        }
        binding.btnZoomLevel.setOnClickListener { viewModel.resetZoom(); applyZoomToVisible() }
        binding.btnFullscreen.setOnClickListener { toggleFullscreen() }
        binding.btnLayoutColumns.setOnClickListener { viewModel.toggleLayoutMode(LayoutMode.COLUMNS) }
        binding.btnLayoutRows.setOnClickListener { viewModel.toggleLayoutMode(LayoutMode.ROWS) }
        binding.btnLayoutGrid.setOnClickListener { viewModel.toggleLayoutMode(LayoutMode.GRID) }

        binding.urlInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                submitUrl()
                true
            } else {
                false
            }
        }
    }

    private fun wireToolbarFocusDown() {
        val downTarget = binding.paneGrid.activePaneRoot().id
        listOf(
            binding.btnBack,
            binding.btnForward,
            binding.btnReload,
            binding.urlInput,
            binding.btnZoomOut,
            binding.btnZoomLevel,
            binding.btnZoomIn,
            binding.btnFullscreen,
            binding.btnLayoutColumns,
            binding.btnLayoutRows,
            binding.btnLayoutGrid,
        ).forEach { it.nextFocusDownId = downTarget }

        binding.paneGrid.activePaneRoot().nextFocusUpId = binding.urlInput.id
    }

    private fun submitUrl() {
        val text = binding.urlInput.text?.toString().orEmpty()
        if (viewModel.loadUrlOnActivePane(text)) {
            val url = viewModel.paneUrl(viewModel.uiState.value.activePane)
            binding.paneGrid.paneSlot(viewModel.uiState.value.activePane).loadUrl(url, this)
        }
    }

    private fun applyZoomToVisible() {
        val percent = viewModel.uiState.value.zoomPercent
        binding.btnZoomLevel.text = getString(R.string.zoom_percent, percent)
        binding.paneGrid.forEachVisible { it.applyZoom(percent) }
    }

    private fun toggleFullscreen() {
        fullscreen = !fullscreen
        binding.toolbarScroll.isVisible = !fullscreen
        if (fullscreen) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            binding.btnFullscreen.contentDescription = getString(R.string.exit_fullscreen)
        } else {
            window.insetsController?.show(WindowInsets.Type.systemBars())
            binding.btnFullscreen.contentDescription = getString(R.string.fullscreen)
            binding.urlInput.requestFocus()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val webPane = binding.paneGrid.webContentPane
        if (webPane != null && event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (event.repeatCount == 0) {
                        longPressDownAt = System.currentTimeMillis()
                        longPressTriggered = false
                        pendingLongPressPane = webPane
                        handler.postDelayed(longPressRunnable, 600)
                    }
                    return true
                }
                KeyEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPressRunnable)
                    if (!longPressTriggered) {
                        binding.paneGrid.paneSlot(webPane).getWebView()?.dispatchKeyEvent(
                            KeyEvent(event.downTime, event.eventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0),
                        )
                        binding.paneGrid.paneSlot(webPane).getWebView()?.dispatchKeyEvent(event)
                    }
                    pendingLongPressPane = null
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private val longPressRunnable = Runnable {
        val pane = pendingLongPressPane ?: return@Runnable
        longPressTriggered = true
        paneDelegate.onLongOkInWebContent(pane)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        binding.paneGrid.onTrimMemory(level)
    }

    override fun onPause() {
        binding.paneGrid.pauseHidden()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.paneGrid.forEachVisible { it.resume() }
    }
}
