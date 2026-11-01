package eu.kanade.tachiyomi.util.image

import ca.mpreg.imagedecoder.ImageDecoder
import okio.BufferedSource
import java.io.FilterInputStream

/** Native fallback covers formats such as AVIF without allocating a decoded bitmap. */
fun readImageDimensions(source: BufferedSource): ImageDimensions? {
    ImageDimensions.read(source)?.let { return it }
    return runCatching {
        val input = object : FilterInputStream(source.peek().inputStream()) {
            override fun available() = 0 // Streaming header, not a complete-file byte count.
        }
        ImageDecoder.open(input, complete = false).use { ImageDimensions(it.width, it.height) }
    }.getOrNull()?.takeIf { it.width > 0 && it.height > 0 }
}
