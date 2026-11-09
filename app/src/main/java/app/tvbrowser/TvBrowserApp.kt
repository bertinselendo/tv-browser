package app.tvbrowser

import android.app.Application
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

class TvBrowserApp : Application() {

    override fun onCreate() {
        super.onCreate()
        if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING)) {
            WebViewCompat.startSafeBrowsing(this) { }
        }
    }
}
