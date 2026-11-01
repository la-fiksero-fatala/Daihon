package eu.kanade.tachiyomi.util.image

import io.kotest.matchers.shouldBe
import okio.Buffer
import okio.buffer
import okio.source
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

class ImageDimensionsTest {
    @Test
    fun `median sorts by area and returns the lower actual middle page`() {
        val values = listOf(
            ImageDimensions(1200, 1800),
            ImageDimensions(600, 900),
            ImageDimensions(1000, 1000),
            ImageDimensions(800, 1200),
        )
        ImageDimensions.median(values) shouldBe ImageDimensions(800, 1200)
        ImageDimensions.median(values.take(3)) shouldBe ImageDimensions(1000, 1000)
        ImageDimensions.median(emptyList()) shouldBe null
        ImageDimensions(100_000, 100_000).area shouldBe 10_000_000_000L
    }

    @Test
    fun `PNG probing leaves the original bytes untouched and does not read the image body`() {
        val header = Buffer().write(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10))
            .writeInt(13).writeUtf8("IHDR").writeInt(640).writeInt(960).readByteArray()
        var bytesRead = 0
        val input = object : ByteArrayInputStream(header + ByteArray(1_000_000)) {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val count = super.read(b, off, len)
                if (count > 0) bytesRead += count
                return count
            }
        }
        input.source().buffer().use { source ->
            ImageDimensions.read(source) shouldBe ImageDimensions(640, 960)
            (bytesRead <= 8192) shouldBe true
            source.readByte().toInt() and 255 shouldBe 137
        }
    }

    @Test
    fun `GIF and all three WebP dimension headers are supported`() {
        ImageDimensions.read(Buffer().writeUtf8("GIF89a").writeShortLe(640).writeShortLe(960).writeShort(0)) shouldBe
            ImageDimensions(640, 960)
        fun webp(kind: String) = Buffer().writeUtf8("RIFF").writeIntLe(128).writeUtf8("WEBP")
            .writeUtf8(kind).writeIntLe(32)
        ImageDimensions.read(webp("VP8X").writeInt(0).write(byteArrayOf(127, 2, 0, 191.toByte(), 3, 0))) shouldBe
            ImageDimensions(640, 960)
        ImageDimensions.read(webp("VP8L").writeByte(47).writeIntLe(639 or (959 shl 14))) shouldBe
            ImageDimensions(640, 960)
        ImageDimensions.read(
            webp("VP8 ").write(byteArrayOf(0, 0, 0, 157.toByte(), 1, 42))
                .writeShortLe(640).writeShortLe(960),
        ) shouldBe ImageDimensions(640, 960)
    }

    @Test
    fun `JPEG probes skip long metadata and support progressive frames`() {
        val jpeg = Buffer().writeShort(0xFFD8).writeShort(0xFFE1).writeShort(65535).write(ByteArray(65533))
            .writeShort(0xFFC2).writeShort(17).writeByte(8).writeShort(1800).writeShort(1200).write(ByteArray(20))
        ImageDimensions.read(jpeg) shouldBe ImageDimensions(1200, 1800)
    }

    @Test
    fun `truncated and invalid headers cannot produce a plausible resolution`() {
        ImageDimensions.read(Buffer().writeUtf8("not an image at all")) shouldBe null
        ImageDimensions.read(Buffer().writeShort(0xFFD8).writeShort(0xFFE1).writeShort(1).write(ByteArray(20))) shouldBe
            null
        ImageDimensions.read(Buffer().write(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10))) shouldBe null
    }
}
