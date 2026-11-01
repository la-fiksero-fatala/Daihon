package eu.kanade.presentation.manga.components

import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.ChapterList
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

class ChapterPageNumbersTest {
    private fun chapter(number: Double) = ChapterList.Item(
        chapter = Chapter.create().copy(chapterNumber = number),
        downloadState = Download.State.NOT_DOWNLOADED,
    )

    @Test
    fun `fractional chapter ranges follow display order and series maximum`() {
        val numbers = ChapterPageNumbers.from(listOf(chapter(80.0), chapter(79.5), chapter(79.0)), 85.5)!!
        numbers.label(0, 1) shouldBe "80–79.5 / 85.5"
        numbers.label(2, 2) shouldBe "79 / 85.5"
        ChapterPageNumbers.from(listOf(chapter(1.0), chapter(1.5), chapter(2.0)))!!.label(0, 2) shouldBe "1–2 / 2"
    }

    @Test
    fun `missing rows and duplicate scanlators neither inflate totals nor break decimal jumps`() {
        val numbers = ChapterPageNumbers.from(
            listOf(chapter(2.5), ChapterList.MissingCount("gap", 1), chapter(1.5), chapter(1.5)),
        )!!
        numbers.label(0, 3) shouldBe "2.5–1.5 / 2.5"
        numbers.choices shouldBe listOf(1.5, 2.5)
        numbers.rowForNumber(1.5) shouldBe 2
        numbers.numberAtRow(1) shouldBe 1.5
    }

    @Test
    fun `unnumbered mixed and empty lists retain entry-based navigation`() {
        ChapterPageNumbers.from(emptyList()) shouldBe null
        ChapterPageNumbers.from(listOf(chapter(-1.0), chapter(2.0))) shouldBe null
        ChapterPageNumbers.from(listOf(chapter(Double.NaN))) shouldBe null
    }
}
