package tachiyomi.presentation.core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

internal val LocalPaperContentBreaks = staticCompositionLocalOf<PaperContentBreaks?> { null }
internal class PaperContentBreaks(val offset: () -> Int) {
    var viewport: LayoutCoordinates? = null
    val pieces = mutableStateMapOf<Any, List<PaperSpan>>()
    val trackers = mutableSetOf<PaperTextBreaks>()
    fun refresh() {
        trackers.forEach { it.refresh() }
    }
}

/** Register actual text lines, rather than guessing from font size or cutting through a line. */
class PaperTextBreaks internal constructor(private val registry: PaperContentBreaks?) {
    private var coordinates: LayoutCoordinates? = null
    private var layout: TextLayoutResult? = null
    val modifier = Modifier.onGloballyPositioned {
        coordinates = it
        refresh()
    }
    fun onTextLayout(result: TextLayoutResult) {
        layout = result
        refresh()
    }
    internal fun refresh() {
        val registry = registry ?: return
        val root = registry.viewport?.takeIf { it.isAttached } ?: return
        val child = coordinates?.takeIf { it.isAttached } ?: return
        val top = root.localPositionOf(child, Offset.Zero).y.roundToInt() + registry.offset()
        registry.pieces[this] = layout?.let { result ->
            (0 until result.lineCount).map {
                PaperSpan(top + floor(result.getLineTop(it)).toInt(), top + ceil(result.getLineBottom(it)).toInt())
            }
        } ?: listOf(PaperSpan(top, top + child.size.height))
    }
}

@Composable
fun rememberPaperTextBreaks(): PaperTextBreaks {
    val registry = LocalPaperContentBreaks.current
    val tracker = remember(registry) { PaperTextBreaks(registry) }
    DisposableEffect(registry, tracker) {
        registry?.trackers?.add(tracker)
        onDispose {
            registry?.trackers?.remove(tracker)
            registry?.pieces?.remove(tracker)
        }
    }
    return tracker
}

/** Chips and other nested controls remain whole even inside a tall form section. */
@Composable
fun Modifier.paperPageItem(): Modifier = then(rememberPaperTextBreaks().modifier)
