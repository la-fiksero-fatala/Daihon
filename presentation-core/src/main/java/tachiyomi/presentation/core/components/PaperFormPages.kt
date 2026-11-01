package tachiyomi.presentation.core.components

data class PaperSpan(val top: Int, val bottom: Int) {
    val height get() = bottom - top
}
data class PaperSlice(val start: Int, val end: Int, val headerHeight: Int = 0)

/** Greedy complete-control/line pages. The final slice never backfills earlier content. */
fun paperFormPages(
    blocks: List<PaperSpan>,
    details: List<PaperSpan>,
    headings: List<PaperSpan>,
    viewport: Int,
): List<PaperSlice> {
    val contentEnd = blocks.maxOfOrNull { it.bottom } ?: 0
    if (contentEnd <= 0 || viewport <= 0) return listOf(PaperSlice(0, contentEnd))
    // Measured text lines/chips provide their own breaks. Their containing section
    // must not be deferred as one control merely because it fits a whole viewport.
    val controls = blocks.filter { block ->
        details.none { it.top >= block.top && it.bottom <= block.bottom && it.height < block.height }
    }
    val units = (controls + details).filter { it.height > 0 && it.bottom <= contentEnd }
    val headers = headings.sortedBy { it.top }
    val pages = mutableListOf<PaperSlice>()
    var start = 0
    while (start < contentEnd) {
        val header = headers.lastOrNull { it.top <= start }
        val reserved = if (header != null && header.top < start) header.height.coerceIn(0, viewport - 1) else 0
        val usable = (viewport - reserved).coerceAtLeast(1)
        var end = (start.toLong() + usable).coerceAtMost(contentEnd.toLong()).toInt()
        val crossing = units.filter { it.height <= usable && it.top > start && it.top < end && it.bottom > end }
        crossing.minOfOrNull { it.top }?.let { end = it }
        val trailingHeader = headers.lastOrNull { it.top > start && it.top < end && it.bottom <= end }
        if (trailingHeader != null) {
            val following = units.filter { it.top >= trailingHeader.bottom }.minByOrNull { it.top }
            if (following != null && following.height <= usable && following.bottom > end) end = trailingHeader.top
        }
        if (end <= start) end = (start.toLong() + usable).coerceAtMost(contentEnd.toLong()).toInt()
        pages.add(PaperSlice(start, end, reserved))
        start = end
    }
    return pages
}
