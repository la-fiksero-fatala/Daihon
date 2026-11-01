package eu.kanade.tachiyomi.util.display

import android.view.View
import android.view.ViewTreeObserver

/** Counts settled screen updates, rather than individual draw frames or key-down/key-up events. */
internal class DisplayRefreshCadence(private val interval: Int) {
    private var updates = 0
    var pending = true
        private set

    fun contentChanged() {
        pending = true
    }

    fun screenChanged(): Boolean {
        if (!pending) return false
        pending = false
        if (interval <= 0) return false
        updates++
        if (updates < interval) return false
        updates = 0
        return true
    }
}

/** Scoped to a resumed activity and cancelled/recreated whenever either profile changes. */
class DisplayRefreshSession(
    private val view: View,
    private val hardware: DisplayHardware,
    private val mode: DisplayMode,
    private val interval: Int,
    private val onFailure: () -> Unit,
) : AutoCloseable, ViewTreeObserver.OnDrawListener {
    private val cadence = DisplayRefreshCadence(interval)
    private var restore: (() -> Unit)? = null
    private var closed = false
    private val observer = view.viewTreeObserver
    private val settled = Runnable {
        if (!closed && view.isAttachedToWindow && view.hasWindowFocus() && cadence.screenChanged()) {
            if (!hardware.refresh(view)) onFailure()
        }
    }

    fun start() {
        restore = hardware.apply(hardware.effectiveMode(mode))
        if (restore == null) onFailure()
        if (hardware.canRefresh && interval > 0) observer.addOnDrawListener(this)
    }

    fun contentChanged() {
        cadence.contentChanged()
    }

    override fun onDraw() {
        if (closed || !cadence.pending) return
        view.removeCallbacks(settled)
        view.postDelayed(settled, 200)
    }

    override fun close() {
        closed = true
        view.removeCallbacks(settled)
        if (observer.isAlive) observer.removeOnDrawListener(this)
        restore?.invoke()
    }
}
