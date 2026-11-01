package eu.kanade.presentation.manga.components

import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.ui.manga.ChapterList

/** Row positions include gap notices; chapter numbers may be fractional or repeated by scanlators. */
internal class ChapterPageNumbers private constructor(
    private val byRow: Map<Int, Double>,
    private val maximum: Double,
) {
    val choices = byRow.values.distinct().sorted()

    fun label(first: Int, last: Int): String? {
        val shown = (first..last).mapNotNull(byRow::get)
        if (shown.isEmpty()) return null
        val from = formatChapterNumber(shown.first())
        val to = formatChapterNumber(shown.last())
        return "${if (from == to) from else "$from–$to"} / ${formatChapterNumber(maximum)}"
    }

    fun numberAtRow(row: Int): Double = byRow.entries.firstOrNull { it.key >= row }?.value ?: byRow.values.last()
    fun rowForNumber(number: Double): Int = byRow.entries.first { it.value == number }.key

    companion object {
        fun from(chapters: List<ChapterList>, maximum: Double? = null): ChapterPageNumbers? {
            val byRow = chapters.mapIndexedNotNull { index, item ->
                (item as? ChapterList.Item)?.let { index to it.chapter.chapterNumber }
            }.toMap()
            if (byRow.isEmpty() || byRow.values.any { !it.isFinite() || it < 0 }) return null
            return ChapterPageNumbers(byRow, maxOf(maximum?.takeIf { it.isFinite() } ?: 0.0, byRow.values.max()))
        }
    }
}
