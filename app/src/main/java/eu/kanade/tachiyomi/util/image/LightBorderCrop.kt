package eu.kanade.tachiyomi.util.image

import android.graphics.Rect
import ca.mpreg.imagedecoder.ImageDecoder
import okio.BufferedSource
import kotlin.math.ceil
import kotlin.math.sqrt

internal data class LightBorderBounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

/** Find content in an RGBA image using the minimum RGB channel after compositing onto white. */
internal fun lightBorderBounds(pixels: ByteArray, width: Int, height: Int, threshold: Int): LightBorderBounds? {
    if (threshold == 0) return null
    var left = width
    var top = height
    var right = 0
    var bottom = 0
    var index = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            val brightness = minOf(
                pixels[index].toInt() and 0xff,
                pixels[index + 1].toInt() and 0xff,
                pixels[index + 2].toInt() and 0xff,
            )
            val alpha = pixels[index + 3].toInt() and 0xff
            index += 4
            if (255 - (255 - brightness) * alpha / 255 < threshold) {
                left = minOf(left, x)
                top = minOf(top, y)
                right = maxOf(right, x + 1)
                bottom = y + 1
            }
        }
    }
    if (left >= right || top >= bottom || (left == 0 && top == 0 && right == width && bottom == height)) return null
    return LightBorderBounds(left, top, right, bottom)
}

/** Probe a bounded image, then crop the original by region so its pixels are kept at native resolution. */
internal fun BufferedSource.findLightBorderCrop(threshold: Int): Rect? {
    if (threshold == 0) return null
    return try {
        peek().inputStream().use { stream ->
            ImageDecoder.open(stream).use { decoder ->
                if (decoder.isHdr) return null
                val sample = ceil(sqrt(decoder.width.toDouble() * decoder.height / 4_000_000)).toInt().coerceAtLeast(1)
                decoder.decode(null, sample).use { frame ->
                    val bounds = lightBorderBounds(frame.toByteArray(), frame.width, frame.height, threshold)
                        ?: return null
                    val guard = if (sample > 1) 1 else 0
                    Rect(
                        ((bounds.left - guard) * sample).coerceAtLeast(0),
                        ((bounds.top - guard) * sample).coerceAtLeast(0),
                        ((bounds.right + guard) * sample).coerceAtMost(decoder.width),
                        ((bounds.bottom + guard) * sample).coerceAtMost(decoder.height),
                    )
                }
            }
        }
    } catch (_: Exception) {
        null
    }
}
