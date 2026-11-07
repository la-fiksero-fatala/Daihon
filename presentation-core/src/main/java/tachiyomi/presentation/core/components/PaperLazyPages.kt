package tachiyomi.presentation.core.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

private val anchorOrder = compareBy(PaperAnchor::index, PaperAnchor::offset)

/** Index pages as their boundaries are measured by the real lazy layout. */
internal class PaperLazyPages {
    private val starts = mutableStateListOf(PaperAnchor(0))
    var seeking by mutableStateOf(false)
        private set

    fun indexOf(position: PaperAnchor): Int {
        val index = starts.binarySearch(position, anchorOrder)
        return if (index >= 0) index else (-index - 2).coerceAtLeast(0)
    }

    suspend fun navigate(
        step: Int,
        position: () -> PaperAnchor,
        next: () -> PaperAnchor?,
        move: suspend (PaperAnchor) -> Unit,
        snap: suspend (PaperAnchor) -> Unit,
    ) {
        if (step > 0) {
            advance(position, next, move, snap)
        } else if (position() != starts.first()) {
            snap(starts[(indexOf(position()) - 1).coerceAtLeast(0)])
        }
    }

    private suspend fun advance(
        position: () -> PaperAnchor,
        next: () -> PaperAnchor?,
        move: suspend (PaperAnchor) -> Unit,
        snap: suspend (PaperAnchor) -> Unit,
    ): Boolean {
        val page = indexOf(position())
        val start = starts[page]
        if (position() != start) snap(start)
        val target = next() ?: return false
        move(target)
        val actual = position()
        if (anchorOrder.compare(actual, start) <= 0) return false
        if (starts.getOrNull(page + 1) != actual) {
            while (starts.size > page + 1) starts.removeAt(starts.lastIndex)
            starts.add(actual)
        }
        return true
    }

    suspend fun goTo(
        page: Int,
        position: () -> PaperAnchor,
        next: () -> PaperAnchor?,
        move: suspend (PaperAnchor) -> Unit,
        snap: suspend (PaperAnchor) -> Unit,
    ) {
        if (page < starts.size) {
            snap(starts[page])
            return
        }

        seeking = true
        try {
            snap(starts.last())
            while (starts.lastIndex < page && advance(position, next, move, snap)) yield()
        } finally {
            seeking = false
        }
    }

    suspend fun locate(
        position: () -> PaperAnchor,
        next: () -> PaperAnchor?,
        move: suspend (PaperAnchor) -> Unit,
        snap: suspend (PaperAnchor) -> Unit,
    ) {
        val original = position()
        if (anchorOrder.compare(original, starts.last()) <= 0) return

        seeking = true
        try {
            snap(starts.last())
            while (anchorOrder.compare(starts.last(), original) < 0 && advance(position, next, move, snap)) yield()
        } finally {
            withContext(NonCancellable) { snap(original) }
            seeking = false
        }
    }
}
