package app.tvbrowser.browser

import android.net.Uri

object UrlNormalizer {
    const val EMPTY_ASSET = "file:///android_asset/empty_pane.html"

    fun normalize(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("file://")) return trimmed
        val withScheme = when {
            trimmed.contains("://") -> trimmed
            trimmed.startsWith("localhost") || trimmed.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+.*")) ->
                "http://$trimmed"
            else -> "https://$trimmed"
        }
        return runCatching { Uri.parse(withScheme).toString() }.getOrNull()
    }

    fun displayUrl(stored: String): String =
        if (stored.isBlank() || stored == EMPTY_ASSET) "" else stored
}
