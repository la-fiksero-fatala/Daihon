package tachiyomi.presentation.core.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PaperListContentTest {
    @Test
    fun `headers do not change content totals or item jump targets`() {
        val content = PaperListContent().apply {
            stickyHeader { }
            items(2) { }
            item(contentType = "header") { }
            items(2) { }
        }
        content.itemCount shouldBe 4
        listOf(1, 2, 4, 5).forEachIndexed { item, row ->
            content.itemIndex(row) shouldBe item
            content.listIndex(item) shouldBe row
        }
    }

    @Test
    fun `consecutive and trailing headings do not become numbered entries`() {
        val content = PaperListContent().apply {
            items(6, contentType = { if (it in listOf(0, 1, 3, 5)) "header" else "item" }) { }
        }
        content.itemCount shouldBe 2
        content.itemIndex(2) shouldBe 0
        content.itemIndex(4) shouldBe 1
        content.listIndex(0) shouldBe 2
        content.listIndex(1) shouldBe 4
        content.itemIndex(5) shouldBe 1
    }

    @Test
    fun `ordinary and empty lists keep their existing numbering`() {
        val content = PaperListContent().apply { items(3) { } }
        content.itemCount shouldBe 3
        (0..2).forEach { index ->
            content.itemIndex(index) shouldBe index
            content.listIndex(index) shouldBe index
        }
        val empty = PaperListContent().apply { stickyHeader { } }
        empty.itemCount shouldBe 0
        empty.itemIndex(0) shouldBe 0
        empty.listIndex(0) shouldBe 0
    }
}
