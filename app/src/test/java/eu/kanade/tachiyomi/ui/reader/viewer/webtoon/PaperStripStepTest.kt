package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PaperStripStepTest {
    @Test
    fun `zoomed sections retain the same visible overlap instead of skipping content`() {
        paperStripStep(1000, 1f) shouldBe 800
        paperStripStep(1000, 2f) shouldBe 400
        paperStripStep(1000, .5f) shouldBe 1600
        paperStripStep(820, 2f) shouldBe 328
    }

    @Test
    fun `a viewport awaiting layout still permits a subsequent navigation step`() {
        paperStripStep(0, 1f) shouldBe 1
        paperStripStep(1, 3f) shouldBe 1
    }
}
