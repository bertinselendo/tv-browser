package app.tvbrowser.ui

import android.content.Context
import android.view.KeyEvent
import android.webkit.WebView
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import app.tvbrowser.R
import app.tvbrowser.browser.PaneWebView
import app.tvbrowser.browser.TvWebChromeClient
import app.tvbrowser.browser.TvWebViewClient
import app.tvbrowser.browser.UrlNormalizer
import app.tvbrowser.browser.WebViewFactory
import app.tvbrowser.browser.Zoom

class PaneSlot(
    context: Context,
    val paneIndex: Int,
    delegate: Delegate,
) {

    var delegate: Delegate = delegate

    interface Delegate {
        fun onPaneUrlChanged(pane: Int, url: String)
        fun onPaneActivated(pane: Int)
        fun onEnterWebContent(pane: Int)
        fun onLeaveWebContent(pane: Int)
        fun onLongOkInWebContent(pane: Int)
        fun getZoomPercent(): Int
        fun onRenderProcessGone(pane: Int): Boolean
    }

    val root: FrameLayout
    private val webContainer: FrameLayout
    private val errorOverlay: View
    private val errorTitle: TextView
    private val btnRetry: Button
    private var webView: PaneWebView? = null
    private var pendingUrl: String? = null
    private var lastLoadedUrl: String = UrlNormalizer.EMPTY_ASSET
    var isWebContentFocused: Boolean = false

    init {
        root = LayoutInflater.from(context).inflate(R.layout.view_pane, null, false) as FrameLayout
        webContainer = root.findViewById(R.id.webContainer)
        errorOverlay = root.findViewById(R.id.errorOverlay)
        errorTitle = root.findViewById(R.id.errorTitle)
        btnRetry = root.findViewById(R.id.btnRetry)
        root.contentDescription = context.getString(R.string.pane_frame, paneIndex + 1)
        root.isFocusable = true

        root.id = View.generateViewId()

        root.setOnClickListener {
            delegate.onPaneActivated(paneIndex)
            enterWebContent()
        }

        root.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    delegate.onPaneActivated(paneIndex)
                    enterWebContent()
                    true
                }
                else -> false
            }
        }

        btnRetry.setOnClickListener {
            hideError()
            reload()
        }
    }

    fun setActive(active: Boolean) {
        root.isSelected = active
    }

    fun ensureWebView(context: Context): PaneWebView {
        webView?.let { return it }
        val wv = WebViewFactory.create(context, paneIndex)
        wv.webViewClient = TvWebViewClient(
            object : TvWebViewClient.Callbacks {
                override fun onPageStarted(url: String?) {
                    hideError()
                }

                override fun onPageFinished(url: String?) {
                    val normalized = url?.takeIf { it.isNotBlank() } ?: lastLoadedUrl
                    if (!normalized.startsWith("file://")) {
                        lastLoadedUrl = normalized
                        delegate.onPaneUrlChanged(paneIndex, normalized)
                    }
                    Zoom.applyAbsolute(wv, delegate.getZoomPercent())
                }

                override fun onSslError() {
                    errorTitle.setText(R.string.error_ssl)
                    showError()
                }

                override fun onNetworkError() {
                    errorTitle.setText(R.string.error_network)
                    showError()
                }

                override fun onRenderProcessGone(): Boolean {
                    return delegate.onRenderProcessGone(paneIndex)
                }
            },
        )
        wv.webChromeClient = TvWebChromeClient { view, _, _, resultMsg ->
            val transport = resultMsg.obj as WebView.WebViewTransport
            transport.webView = wv
            resultMsg.sendToTarget()
            true
        }
        wv.setDownloadListener { _, _, _, _, _ ->
            Toast.makeText(context, R.string.download_not_supported, Toast.LENGTH_SHORT).show()
        }
        wv.setOnKeyListener { _, keyCode, event ->
            handleWebViewKeys(wv, keyCode, event)
        }
        webContainer.addView(
            wv,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        webView = wv
        val url = pendingUrl ?: lastLoadedUrl
        loadUrlInternal(wv, url)
        pendingUrl = null
        return wv
    }

    fun getWebView(): PaneWebView? = webView

    fun pause() {
        webView?.onPause()
        webView?.pauseTimers()
    }

    fun resume() {
        webView?.onResume()
        webView?.resumeTimers()
    }

    fun destroyWebView() {
        webView?.let { wv ->
            webContainer.removeView(wv)
            wv.stopLoading()
            wv.destroy()
        }
        webView = null
    }

    fun loadUrl(url: String, context: Context) {
        lastLoadedUrl = url
        hideError()
        val wv = webView
        if (wv == null) {
            pendingUrl = url
            return
        }
        loadUrlInternal(wv, url)
    }

    fun applyZoom(percent: Int) {
        webView?.let { Zoom.applyAbsolute(it, percent) }
    }

    fun reload() {
        val wv = webView ?: return
        if (lastLoadedUrl.isBlank()) return
        loadUrlInternal(wv, lastLoadedUrl)
    }

    fun goBack() {
        webView?.takeIf { it.canGoBack() }?.goBack()
    }

    fun goForward() {
        webView?.takeIf { it.canGoForward() }?.goForward()
    }

    fun currentUrlForBar(): String = UrlNormalizer.displayUrl(lastLoadedUrl)

    fun loadedUrl(): String = lastLoadedUrl

    fun restoreStoredUrl(url: String) {
        lastLoadedUrl = if (url.isBlank()) UrlNormalizer.EMPTY_ASSET else url
    }

    private fun loadUrlInternal(wv: PaneWebView, url: String) {
        wv.setInitialScale(delegate.getZoomPercent())
        wv.loadUrl(url)
    }

    private fun enterWebContent() {
        val wv = ensureWebView(root.context)
        isWebContentFocused = true
        wv.requestFocus()
        delegate.onEnterWebContent(paneIndex)
    }

    fun exitWebContent() {
        if (!isWebContentFocused) return
        isWebContentFocused = false
        delegate.onLeaveWebContent(paneIndex)
        root.requestFocus()
    }

    private fun handleWebViewKeys(wv: PaneWebView, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
            exitWebContent()
            return true
        }
        return false
    }

    private fun showError() {
        errorOverlay.visibility = View.VISIBLE
        btnRetry.requestFocus()
    }

    private fun hideError() {
        errorOverlay.visibility = View.GONE
    }
}
