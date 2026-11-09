package app.tvbrowser.browser

import android.content.Context
import android.webkit.WebView

class PaneWebView(
    context: Context,
    val paneIndex: Int,
) : WebView(context) {

    var applyingProgrammaticZoom: Boolean = false

    init {
        @Suppress("DEPRECATION")
        settings.setEnableSmoothTransition(true)
    }
}
