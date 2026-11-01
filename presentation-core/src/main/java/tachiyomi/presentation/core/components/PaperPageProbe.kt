package tachiyomi.presentation.core.components

import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal data class PaperPageProbeKey(val count: Int, val viewport: IntSize)
internal data class PaperPageProbeResult(val key: PaperPageProbeKey, val hasThirdPage: Boolean)

/** Check only the first two layouts while drawing is withheld, then restore the original position. */
internal suspend fun probePaperThirdPage(
    original: PaperAnchor,
    top: suspend () -> Unit,
    next: () -> PaperAnchor?,
    move: suspend (PaperAnchor) -> Unit,
    restore: suspend (PaperAnchor) -> Unit,
    settle: suspend () -> Unit = {},
): Boolean = try {
    top()
    settle()
    val second = next()
    if (second == null) {
        false
    } else {
        move(second)
        settle()
        next() != null
    }
} finally {
    withContext(NonCancellable) { restore(original) }
}
