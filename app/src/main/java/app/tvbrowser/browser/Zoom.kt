package app.tvbrowser.browser

@Suppress("DEPRECATION")
object Zoom {
    const val MIN = 50
    const val MAX = 200
    const val STEP = 10

    fun coerce(percent: Int): Int = percent.coerceIn(MIN, MAX)

    fun step(percent: Int, delta: Int): Int = coerce(percent + delta)

    fun applyAbsolute(webView: PaneWebView, targetPercent: Int) {
        webView.setInitialScale(targetPercent)
        webView.post {
            val current = webView.scale
            if (current <= 0f) {
                webView.viewTreeObserver.addOnGlobalLayoutListener(
                    object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            webView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            applyAbsolute(webView, targetPercent)
                        }
                    },
                )
                return@post
            }
            val ratio = (targetPercent / 100f) / current
            if (kotlin.math.abs(1f - ratio) > 0.02f) {
                webView.applyingProgrammaticZoom = true
                webView.zoomBy(ratio)
                webView.post { webView.applyingProgrammaticZoom = false }
            }
        }
    }
}
