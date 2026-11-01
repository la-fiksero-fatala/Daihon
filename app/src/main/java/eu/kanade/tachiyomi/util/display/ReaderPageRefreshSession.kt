package eu.kanade.tachiyomi.util.display

import android.view.View
import android.view.ViewTreeObserver
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

/** Wait for a decoded, selected page and a settled draw, including the first page. */
internal class ReaderPageRefreshSession(
    private val view: View,
    private val canRefresh: () -> Boolean,
    private val refresh: () -> Unit,
) : AutoCloseable, ViewTreeObserver.OnDrawListener {
    private val gate = ReaderPageRefreshGate<ReaderPage>()
    private val observer = view.viewTreeObserver
    private var closed = false
    private val settled = Runnable {
        if (!closed && view.isAttachedToWindow && view.hasWindowFocus() && canRefresh() && gate.takeReady() != null) {
            refresh()
        }
    }

    init {
        observer.addOnDrawListener(this)
    }

    fun selected(page: ReaderPage, sectionChanged: Boolean = false) {
        gate.select(page, sectionChanged)
        view.invalidate()
    }

    fun clearSelection() {
        gate.clearSelection()
        view.removeCallbacks(settled)
    }

    fun decoded(page: ReaderPage) {
        gate.decoded(page)
        view.invalidate()
    }

    fun released(page: ReaderPage) {
        gate.released(page)
    }

    override fun onDraw() {
        if (closed || !gate.ready || !canRefresh()) return
        view.removeCallbacks(settled)
        view.postDelayed(settled, 200)
    }

    override fun close() {
        closed = true
        view.removeCallbacks(settled)
        if (observer.isAlive) observer.removeOnDrawListener(this)
    }
}
