package eu.kanade.tachiyomi.util.display

import java.util.WeakHashMap

/** Downloads/preloads alone must never trigger a panel refresh. Identity includes the chapter. */
internal class ReaderPageRefreshGate<T : Any> {
    private val decoded = WeakHashMap<T, Boolean>()
    private var selected: T? = null
    private var pending = false
    val ready: Boolean get() = pending && decoded[selected] == true

    fun select(page: T, sectionChanged: Boolean = false) {
        if (selected !== page || sectionChanged) {
            selected = page
            pending = true
        }
    }

    fun clearSelection() {
        selected = null
        pending = false
    }

    fun decoded(page: T) {
        decoded[page] = true
    }
    fun released(page: T) {
        decoded.remove(page)
    }
    fun takeReady(): T? = if (ready) selected.also { pending = false } else null
}
