package eu.kanade.tachiyomi.ui.reader.viewer

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderPagePaddingTest {
    @Test
    fun `normal padding stays in physical pixels`() {
        boundedReaderPagePadding(24, 1264, 1680) shouldBe 24
        boundedReaderPagePadding(0, 1264, 1680) shouldBe 0
    }

    @Test
    fun `restored values cannot collapse the viewport`() {
        boundedReaderPagePadding(Int.MAX_VALUE, 320, 480) shouldBe 80
        boundedReaderPagePadding(-1, 320, 480) shouldBe 0
        boundedReaderPagePadding(24, 0, 480) shouldBe 0
    }

    @Test
    fun `rotation retains the same bezel margin limit`() {
        readerPagePaddingLimit(1264, 1680) shouldBe readerPagePaddingLimit(1680, 1264)
    }
}
