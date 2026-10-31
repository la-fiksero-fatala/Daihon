package tachiyomi.presentation.core.components

data class PaperItem(val index: Int, val offset: Int, val size: Int)

data class PaperAnchor(val index: Int, val offset: Int = 0)

/** First unshown row, or an overlapping slice when one item is taller than the viewport. */
fun nextPaperAnchor(
    items: List<PaperItem>,
    first: PaperAnchor,
    viewportEnd: Int,
    total: Int,
): PaperAnchor? {
    if (total <= 0 || viewportEnd <= 0) return null
    val visible = items.filter { it.index < total && it.offset < viewportEnd }
    val leading = visible.firstOrNull() ?: return null
    if (leading.index == first.index && leading.size > viewportEnd && leading.offset + leading.size > viewportEnd) {
        return PaperAnchor(first.index, first.offset + paperPageStep(viewportEnd))
    }
    val partial = visible.firstOrNull { it.offset + it.size > viewportEnd }
    val next = partial?.index ?: ((visible.lastOrNull()?.index ?: return null) + 1)
    return next.takeIf { it > first.index && it < total }?.let(::PaperAnchor)
}

fun paperPageStep(viewport: Int): Int = (viewport * .8f).toInt().coerceAtLeast(1)

fun paperPageCount(maximumOffset: Int, step: Int): Int =
    ((maximumOffset.coerceAtLeast(0).toLong() + step.coerceAtLeast(1) - 1) / step.coerceAtLeast(1) + 1)
        .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

fun paperPageIndex(input: String, count: Int): Int? =
    input.toIntOrNull()?.takeIf { it in 1..count }?.minus(1)
