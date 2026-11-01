package tachiyomi.presentation.core.components

data class PaperItem(val index: Int, val offset: Int, val size: Int)

data class PaperAnchor(val index: Int, val offset: Int = 0)

/** First unshown complete row; oversized content advances by one full usable viewport. */
fun nextPaperAnchor(
    items: List<PaperItem>,
    first: PaperAnchor,
    viewportEnd: Int,
    total: Int,
    viewportStart: Int = 0,
): PaperAnchor? {
    if (total <= 0 || viewportEnd <= viewportStart) return null
    val visible = items.filter { it.index < total && it.offset < viewportEnd && it.offset + it.size > viewportStart }
    val rows = visible.groupBy { it.offset }.toSortedMap().values
    val leading = rows.firstOrNull() ?: return null
    val leadingEnd = leading.maxOf { it.offset + it.size }
    if (leading.minOf { it.index } == first.index && leadingEnd > viewportEnd &&
        leading.maxOf { it.size } > viewportEnd - viewportStart
    ) {
        return PaperAnchor(first.index, first.offset + viewportEnd - viewportStart)
    }
    val partial = rows.firstOrNull { row -> row.any { it.offset + it.size > viewportEnd } }
    val next = partial?.minOf { it.index } ?: (visible.maxOf { it.index } + 1)
    return next.takeIf { it > first.index && it < total }?.let(::PaperAnchor)
}

fun paperPageStep(viewport: Int): Int = viewport.coerceAtLeast(1)

fun paperPageCount(maximumOffset: Int, step: Int): Int =
    ((maximumOffset.coerceAtLeast(0).toLong() + step.coerceAtLeast(1) - 1) / step.coerceAtLeast(1) + 1)
        .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

fun paperPageIndex(input: String, count: Int): Int? =
    input.toIntOrNull()?.takeIf { it in 1..count }?.minus(1)

/** Uniform rows keep the same positions across pages without leaving a fractional empty row. */
fun paperRowSpacing(viewport: Int, rowHeight: Int, total: Int): Float {
    if (rowHeight <= 0 || total <= 1) return 0f
    val rows = viewport / rowHeight
    if (total < rows) return 0f
    return if (rows > 1) ((viewport - rows * rowHeight) / (rows - 1)).toFloat() else 0f
}

/** Defer a section heading when none of its following rows fits on this page. */
fun paperOrphanHeader(items: List<PaperItem>, headers: List<Int>, first: PaperAnchor, end: Int): PaperItem? =
    items.firstOrNull { item ->
        item.index in headers && item.index > first.index && item.offset < end &&
            items.none { it.index > item.index && it.index !in headers && it.offset + it.size <= end }
    }

/** A lazy list confirms the third page from real boundaries, without measuring every item. */
fun paperHasThirdPage(startedAtTop: Boolean, previousPages: Int, hasNext: Boolean): Boolean =
    startedAtTop && (previousPages >= 2 || (previousPages == 1 && hasNext))
