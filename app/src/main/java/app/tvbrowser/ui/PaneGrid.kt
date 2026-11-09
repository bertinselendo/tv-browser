package app.tvbrowser.ui

import android.content.ComponentCallbacks2
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import app.tvbrowser.data.LayoutMode

class PaneGrid @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    private val gapPx = (4 * resources.displayMetrics.density).toInt()
    private var layoutMode: LayoutMode = LayoutMode.SINGLE
    private var activePane: Int = 0
    private lateinit var delegate: PaneSlot.Delegate
    private val slots = mutableListOf<PaneSlot>()

    var webContentPane: Int? = null
        private set

    fun attach(delegate: PaneSlot.Delegate) {
        this.delegate = delegate
        if (slots.isEmpty()) {
            repeat(4) { index ->
                val slot = PaneSlot(context, index, delegate)
                slots.add(slot)
                addView(slot.root, LayoutParams(0, 0))
            }
        } else {
            slots.forEach { it.delegate = delegate }
        }
    }

    fun paneSlot(index: Int): PaneSlot = slots[index]

    fun paneRoot(index: Int): View = slots[index].root

    fun activePaneRoot(): View = slots[activePane].root

    fun setLayoutMode(mode: LayoutMode) {
        layoutMode = mode
        updateVisibility()
        requestLayout()
    }

    fun setActivePane(index: Int) {
        activePane = index.coerceIn(0, 3)
        slots.forEachIndexed { i, slot -> slot.setActive(i == activePane) }
    }

    fun visiblePaneIndices(): List<Int> = when (layoutMode) {
        LayoutMode.SINGLE -> listOf(0)
        LayoutMode.COLUMNS, LayoutMode.ROWS -> listOf(0, 1)
        LayoutMode.GRID -> listOf(0, 1, 2, 3)
    }

    fun forEachVisible(block: (PaneSlot) -> Unit) {
        visiblePaneIndices().forEach { block(slots[it]) }
    }

    fun pauseHidden() {
        slots.forEachIndexed { index, slot ->
            if (index in visiblePaneIndices()) slot.resume() else slot.pause()
        }
    }

    fun onTrimMemory(level: Int) {
        if (level < ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) return
        val visible = visiblePaneIndices().toSet()
        slots.forEachIndexed { index, slot ->
            if (index !in visible) slot.destroyWebView()
        }
    }

    fun onWebContentEntered(pane: Int) {
        webContentPane = pane
    }

    fun onWebContentLeft() {
        webContentPane = null
    }

    fun exitWebContentIfNeeded(): Boolean {
        val pane = webContentPane ?: return false
        slots[pane].exitWebContent()
        webContentPane = null
        return true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (slots.isEmpty()) return
        val w = right - left
        val h = bottom - top
        when (layoutMode) {
            LayoutMode.SINGLE -> layoutPane(0, 0, 0, w, h)
            LayoutMode.COLUMNS -> {
                val half = (w - gapPx) / 2
                layoutPane(0, 0, 0, half, h)
                layoutPane(1, half + gapPx, 0, w, h)
            }
            LayoutMode.ROWS -> {
                val half = (h - gapPx) / 2
                layoutPane(0, 0, 0, w, half)
                layoutPane(1, 0, half + gapPx, w, h)
            }
            LayoutMode.GRID -> {
                val halfW = (w - gapPx) / 2
                val halfH = (h - gapPx) / 2
                layoutPane(0, 0, 0, halfW, halfH)
                layoutPane(1, halfW + gapPx, 0, w, halfH)
                layoutPane(2, 0, halfH + gapPx, halfW, h)
                layoutPane(3, halfW + gapPx, halfH + gapPx, w, h)
            }
        }
        updateFocusNeighbors()
    }

    private fun layoutPane(index: Int, l: Int, t: Int, r: Int, b: Int) {
        if (slots[index].root.visibility != View.VISIBLE) return
        slots[index].root.layout(l, t, r, b)
    }

    private fun updateVisibility() {
        if (slots.isEmpty()) return
        val visible = visiblePaneIndices()
        slots.forEachIndexed { index, slot ->
            slot.root.visibility = if (index in visible) View.VISIBLE else View.GONE
        }
        if (activePane !in visible) {
            activePane = visible.first()
            setActivePane(activePane)
        }
        pauseHidden()
    }

    private fun updateFocusNeighbors() {
        slots.forEach { slot ->
            slot.root.nextFocusLeftId = View.NO_ID
            slot.root.nextFocusRightId = View.NO_ID
            slot.root.nextFocusUpId = View.NO_ID
            slot.root.nextFocusDownId = View.NO_ID
        }
        when (layoutMode) {
            LayoutMode.COLUMNS -> {
                slots[0].root.nextFocusRightId = slots[1].root.id
                slots[1].root.nextFocusLeftId = slots[0].root.id
            }
            LayoutMode.ROWS -> {
                slots[0].root.nextFocusDownId = slots[1].root.id
                slots[1].root.nextFocusUpId = slots[0].root.id
            }
            LayoutMode.GRID -> {
                slots[0].root.nextFocusRightId = slots[1].root.id
                slots[0].root.nextFocusDownId = slots[2].root.id
                slots[1].root.nextFocusLeftId = slots[0].root.id
                slots[1].root.nextFocusDownId = slots[3].root.id
                slots[2].root.nextFocusUpId = slots[0].root.id
                slots[2].root.nextFocusRightId = slots[3].root.id
                slots[3].root.nextFocusUpId = slots[1].root.id
                slots[3].root.nextFocusLeftId = slots[2].root.id
            }
            LayoutMode.SINGLE -> Unit
        }
    }
}
