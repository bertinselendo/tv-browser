package app.tvbrowser.browser

import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import app.tvbrowser.R

class TvWebChromeClient(
    private val onCreateWindowInSamePane: (WebView, Boolean, Boolean, android.os.Message) -> Boolean,
) : WebChromeClient() {

    override fun onPermissionRequest(request: PermissionRequest) {
        request.deny()
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: android.os.Message,
    ): Boolean = onCreateWindowInSamePane(view, isDialog, isUserGesture, resultMsg)

    override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
        callback?.onCustomViewHidden()
    }
}
