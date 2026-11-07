package eu.kanade.tachiyomi.util.image

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LightBorderCropTest {
    @Test
    fun `lowering the threshold ignores a light watermark while retaining darker art`() {
        val pixels = ByteArray(5 * 4 * 4) { 0xff.toByte() }
        fun gray(x: Int, y: Int, shade: Int) {
            val index = (y * 5 + x) * 4
            pixels.fill(shade.toByte(), index, index + 3)
        }

        gray(0, 2, 225)
        gray(2, 1, 40)
        gray(3, 2, 150)

        lightBorderBounds(pixels, 5, 4, 255) shouldBe LightBorderBounds(0, 1, 4, 3)
        lightBorderBounds(pixels, 5, 4, 220) shouldBe LightBorderBounds(2, 1, 4, 3)
    }

    @Test
    fun `empty and fully transparent margins cannot crop away the whole page`() {
        lightBorderBounds(ByteArray(16), 2, 2, 255) shouldBe null
        lightBorderBounds(ByteArray(16) { 0xff.toByte() }, 2, 2, 255) shouldBe null
        lightBorderBounds(ByteArray(16) { 0x7f }, 2, 2, 0) shouldBe null
    }
}
