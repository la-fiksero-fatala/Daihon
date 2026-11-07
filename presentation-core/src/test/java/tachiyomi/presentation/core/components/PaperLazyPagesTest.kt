package tachiyomi.presentation.core.components

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class PaperLazyPagesTest {
    @Test
    fun `page numbers follow measured boundaries including portions of the same row`() = runBlocking<Unit> {
        val pages = PaperLazyPages()
        val starts = listOf(PaperAnchor(0), PaperAnchor(4), PaperAnchor(4, 500), PaperAnchor(10))
        var position = starts.first()
        fun next() = starts.getOrNull(starts.indexOf(position) + 1)
        suspend fun snap(anchor: PaperAnchor) {
            position = anchor
        }

        pages.navigate(1, { position }, ::next, ::snap, ::snap)
        position shouldBe PaperAnchor(4)
        pages.indexOf(position) shouldBe 1
        pages.goTo(3, { position }, ::next, ::snap, ::snap)
        position shouldBe PaperAnchor(10)
        pages.navigate(-1, { position }, ::next, ::snap, ::snap)
        position shouldBe PaperAnchor(4, 500)
        pages.indexOf(position) shouldBe 2
    }

    @Test
    fun `restoring a position inside a page counts the preceding pages without moving it`() = runBlocking<Unit> {
        val pages = PaperLazyPages()
        val restored = PaperAnchor(8)
        var position = restored
        fun next() = when (position.index) {
            0 -> PaperAnchor(3)
            3 -> PaperAnchor(7)
            7 -> PaperAnchor(11)
            else -> null
        }
        suspend fun snap(anchor: PaperAnchor) {
            position = anchor
        }

        pages.locate({ position }, ::next, ::snap, ::snap)
        position shouldBe restored
        pages.indexOf(position) shouldBe 2
        pages.goTo(99, { position }, ::next, ::snap, ::snap)
        position shouldBe PaperAnchor(11)
        pages.indexOf(position) shouldBe 3
    }
}
