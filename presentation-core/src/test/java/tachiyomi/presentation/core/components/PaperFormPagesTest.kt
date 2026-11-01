package tachiyomi.presentation.core.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PaperFormPagesTest {
    @Test
    fun `controls remain whole and the final page does not backfill`() {
        paperFormPages(List(5) { PaperSpan(it * 80, (it + 1) * 80) }, emptyList(), emptyList(), 250) shouldBe
            listOf(PaperSlice(0, 240), PaperSlice(240, 400))
    }

    @Test
    fun `long paragraphs break at actual line boundaries without overlap`() {
        paperFormPages(
            listOf(PaperSpan(0, 1000)),
            List(10) { PaperSpan(it * 100, (it + 1) * 100) },
            emptyList(),
            350,
        ) shouldBe listOf(PaperSlice(0, 300), PaperSlice(300, 600), PaperSlice(600, 900), PaperSlice(900, 1000))
    }

    @Test
    fun `a description uses the remaining first page even when the whole paragraph fits one viewport`() {
        paperFormPages(
            listOf(PaperSpan(0, 200), PaperSpan(200, 600)),
            List(4) { PaperSpan(200 + it * 100, 300 + it * 100) },
            emptyList(),
            500,
        ) shouldBe listOf(PaperSlice(0, 500), PaperSlice(500, 600))
    }

    @Test
    fun `a repeated section heading reserves space above complete controls`() {
        paperFormPages(
            listOf(PaperSpan(0, 50)) + List(5) { PaperSpan(50 + it * 100, 150 + it * 100) },
            emptyList(),
            listOf(PaperSpan(0, 50)),
            300,
        ) shouldBe listOf(PaperSlice(0, 250), PaperSlice(250, 450, 50), PaperSlice(450, 550, 50))
    }

    @Test
    fun `a heading without a following control moves to the next page`() {
        paperFormPages(
            listOf(PaperSpan(0, 200), PaperSpan(200, 240), PaperSpan(240, 340)),
            emptyList(),
            listOf(PaperSpan(200, 240)),
            300,
        ) shouldBe listOf(PaperSlice(0, 200), PaperSlice(200, 340))
    }

    @Test
    fun `oversized indivisible content still progresses and empty content is one page`() {
        paperFormPages(listOf(PaperSpan(0, 850)), emptyList(), emptyList(), 300) shouldBe
            listOf(PaperSlice(0, 300), PaperSlice(300, 600), PaperSlice(600, 850))
        paperFormPages(emptyList(), emptyList(), emptyList(), 300) shouldBe listOf(PaperSlice(0, 0))
        paperFormPages(listOf(PaperSpan(0, 850)), emptyList(), emptyList(), 0) shouldBe listOf(PaperSlice(0, 850))
    }
}
