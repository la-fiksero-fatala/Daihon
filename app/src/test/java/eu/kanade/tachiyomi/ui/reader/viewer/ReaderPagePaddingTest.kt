package eu.kanade.tachiyomi.ui.reader.viewer

import eu.kanade.tachiyomi.util.image.ImageDimensions
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
    private val dynamic = ReaderPaddingSettings(target = 15, dynamic = true, minimum = 5, maximum = 40)

    @Test
    fun `nearby smaller and larger pages fit at exactly native scale`() {
        readerPageLayout(1264, 1660, dynamic, ImageDimensions(1000, 1600)) shouldBe
            ReaderPageLayout(30, 30, 30, 30, native = true)
        readerPageLayout(1264, 1660, dynamic, ImageDimensions(1000, 1640)) shouldBe
            ReaderPageLayout(10, 10, 10, 10, native = true)
    }

    @Test
    fun `tiny pages enlarge and oversized pages shrink inside the target viewport`() {
        val target = ReaderPageLayout(15, 15, 15, 15)
        readerPageLayout(1264, 1660, dynamic, ImageDimensions(1000, 1400)) shouldBe target
        readerPageLayout(1264, 1660, dynamic, ImageDimensions(1800, 2400)) shouldBe target
        readerPageLayout(1264, 1660, dynamic, ImageDimensions(1300, 1600)) shouldBe target
    }

    @Test
    fun `odd pixel gaps do not introduce a fractional fitting scale`() {
        readerPageLayout(1264, 1660, dynamic, ImageDimensions(1001, 1599)) shouldBe
            ReaderPageLayout(30, 30, 30, 31, native = true)
        readerPageLayout(1265, 1660, dynamic, ImageDimensions(1204, 1501)) shouldBe
            ReaderPageLayout(30, 30, 31, 30, native = true)
        readerPageLayout(1264, 1660, dynamic.copy(maximum = 30), ImageDimensions(1001, 1599)).native shouldBe false
    }

    @Test
    fun `native decisions respect both axes and inclusive padding limits`() {
        for (width in listOf(600, 1264, 1660)) {
            for (height in listOf(900, 1660)) {
                for (imageWidth in listOf(width - 80, width - 61, width - 10, width, width + 1)) {
                    for (imageHeight in listOf(height - 80, height - 61, height - 10, height + 1)) {
                        val layout = readerPageLayout(width, height, dynamic, ImageDimensions(imageWidth, imageHeight))
                        if (layout.native) {
                            val viewportWidth = width - layout.left - layout.right
                            val viewportHeight = height - layout.top - layout.bottom
                            minOf(
                                viewportWidth.toDouble() / imageWidth,
                                viewportHeight.toDouble() / imageHeight,
                            ) shouldBe
                                1.0
                            minOf(layout.left, layout.top) shouldBe minOf(width - imageWidth, height - imageHeight) / 2
                            (minOf(layout.left, layout.top) >= dynamic.minimum) shouldBe true
                            (maxOf(layout.right, layout.bottom) <= dynamic.maximum) shouldBe true
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `disabled missing and invalid dimensions preserve bounded fixed padding`() {
        readerPageLayout(1264, 1660, dynamic.copy(dynamic = false), ImageDimensions(1000, 1600)) shouldBe
            ReaderPageLayout(15, 15, 15, 15)
        readerPageLayout(1264, 1660, dynamic, null).native shouldBe false
        readerPageLayout(0, 1660, dynamic, ImageDimensions(1, 1)).native shouldBe false
        readerPageLayout(1264, 1660, dynamic, ImageDimensions(0, 1600)).native shouldBe false
        readerPageLayout(320, 480, ReaderPaddingSettings(Int.MAX_VALUE, true, Int.MAX_VALUE, -1), null) shouldBe
            ReaderPageLayout(80, 80, 80, 80)
    }
}
