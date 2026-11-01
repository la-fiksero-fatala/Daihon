package tachiyomi.presentation.core.components

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class PaperPageProbeTest {
    @Test
    fun `three pages are detected before navigation and the original position is restored`() = runBlocking<Unit> {
        var position = PaperAnchor(9, 12)
        val original = position
        probePaperThirdPage(
            original,
            top = { position = PaperAnchor(0) },
            next = { if (position.index < 10) PaperAnchor(position.index + 5) else null },
            move = { position = it },
            restore = { position = it },
        ) shouldBe true
        position shouldBe original
    }

    @Test
    fun `two pages do not expose jump to top`() = runBlocking<Unit> {
        var position = PaperAnchor(0)
        probePaperThirdPage(
            position,
            top = { position = PaperAnchor(0) },
            next = { if (position.index == 0) PaperAnchor(5) else null },
            move = { position = it },
            restore = { position = it },
        ) shouldBe false
        position shouldBe PaperAnchor(0)
    }
}
