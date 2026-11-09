package app.tvbrowser.data

enum class LayoutMode(val storageKey: String) {
    SINGLE("single"),
    COLUMNS("columns"),
    ROWS("rows"),
    GRID("grid"),
    ;

    companion object {
        fun fromStorage(value: String?): LayoutMode =
            entries.firstOrNull { it.storageKey == value } ?: SINGLE
    }
}
