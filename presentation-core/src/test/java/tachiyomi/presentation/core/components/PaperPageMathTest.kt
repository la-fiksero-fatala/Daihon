package tachiyomi.presentation.core.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PaperPageMathTest {
    @Test
    fun `a partly visible row starts the next page without skipping it`() {
        nextPaperAnchor(
            listOf(PaperItem(0, 0, 100), PaperItem(1, 100, 100), PaperItem(2, 200, 100)),
            PaperAnchor(0),
            250,
            10,
        ) shouldBe PaperAnchor(2)
    }

    @Test
    fun `a row that fits exactly belongs to the current page`() {
        nextPaperAnchor(
            listOf(PaperItem(4, 0, 100), PaperItem(5, 100, 100)),
            PaperAnchor(4),
            200,
            12,
        ) shouldBe PaperAnchor(6)
    }

    @Test
    fun `an incomplete final page has no next target`() {
        nextPaperAnchor(listOf(PaperItem(9, 0, 100)), PaperAnchor(9), 250, 10) shouldBe null
    }

    @Test
    fun `one oversized item advances by overlapping sections`() {
        nextPaperAnchor(listOf(PaperItem(0, 0, 1600)), PaperAnchor(0), 600, 2) shouldBe PaperAnchor(0, 480)
        nextPaperAnchor(listOf(PaperItem(0, -480, 1600)), PaperAnchor(0, 480), 600, 2) shouldBe PaperAnchor(0, 960)
    }

    @Test
    fun `a tall item eventually reaches the next item`() {
        nextPaperAnchor(
            listOf(PaperItem(0, -1200, 1600), PaperItem(1, 400, 100)),
            PaperAnchor(0, 1200),
            600,
            2,
        ) shouldBe null
    }

    @Test
    fun `empty or unmeasured lists have no next target`() {
        nextPaperAnchor(emptyList(), PaperAnchor(0), 600, 0) shouldBe null
        nextPaperAnchor(listOf(PaperItem(0, 0, 100)), PaperAnchor(0), 0, 10) shouldBe null
    }

    @Test
    fun `trailing padding never becomes a data page`() {
        nextPaperAnchor(
            listOf(PaperItem(3, 0, 100), PaperItem(4, 100, 600)),
            PaperAnchor(3),
            300,
            4,
        ) shouldBe null
    }

    @Test
    fun `text pagination includes an incomplete last section and avoids integer overflow`() {
        paperPageCount(0, 480) shouldBe 1
        paperPageCount(960, 480) shouldBe 3
        paperPageCount(961, 480) shouldBe 4
        paperPageCount(Int.MAX_VALUE, 1) shouldBe Int.MAX_VALUE
        paperPageStep(0) shouldBe 1
    }

    @Test
    fun `page input rejects missing negative out of bounds and overflowing numbers`() {
        listOf("", "0", "-1", "4", "1.5", "999999999999999").forEach { paperPageIndex(it, 3) shouldBe null }
        paperPageIndex("1", 3) shouldBe 0
        paperPageIndex("3", 3) shouldBe 2
    }
}
