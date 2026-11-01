package eu.kanade.tachiyomi.util.image

import okio.BufferedSource
import java.io.EOFException

/** Encoded dimensions, before any crop, page split or viewport scaling. */
data class ImageDimensions(val width: Int, val height: Int) {
    val area: Long get() = width.toLong() * height
    override fun toString() = "$width × $height"

    companion object {
        fun median(values: List<ImageDimensions>): ImageDimensions? =
            values.sortedBy { it.area }.getOrNull((values.size - 1) / 2)

        /** Read only enough bytes to locate common image headers; leave the caller's source untouched. */
        fun read(source: BufferedSource): ImageDimensions? = try {
            val input = source.peek()
            val signature = input.readByteString(12)
            val result = when {
                signature.hex().startsWith("89504e470d0a1a0a") -> {
                    input.skip(4) // IHDR
                    ImageDimensions(input.readInt(), input.readInt())
                }
                signature.substring(0, 3).utf8() == "GIF" -> {
                    val bytes = signature.toByteArray()
                    ImageDimensions(
                        (bytes[6].toInt() and 255) or ((bytes[7].toInt() and 255) shl 8),
                        (bytes[8].toInt() and 255) or ((bytes[9].toInt() and 255) shl 8),
                    )
                }
                signature.substring(0, 2).hex() == "ffd8" -> jpeg(source.peek())
                signature.substring(0, 4).utf8() == "RIFF" && signature.substring(8, 12).utf8() == "WEBP" -> {
                    val kind = input.readUtf8(4)
                    input.skip(4) // chunk length
                    when (kind) {
                        "VP8X" -> {
                            input.skip(4)
                            fun uint24(): Int = (input.readByte().toInt() and 255) or
                                ((input.readByte().toInt() and 255) shl 8) or
                                ((input.readByte().toInt() and 255) shl 16)
                            ImageDimensions(uint24() + 1, uint24() + 1)
                        }
                        "VP8L" -> {
                            if (input.readByte().toInt() and 255 != 47) {
                                null
                            } else {
                                val packed = input.readIntLe()
                                ImageDimensions((packed and 16383) + 1, ((packed ushr 14) and 16383) + 1)
                            }
                        }
                        "VP8 " -> {
                            input.skip(3)
                            if (input.readByteString(3).hex() != "9d012a") {
                                null
                            } else {
                                ImageDimensions(
                                    input.readShortLe().toInt() and 16383,
                                    input.readShortLe().toInt() and 16383,
                                )
                            }
                        }
                        else -> null
                    }
                }
                else -> null
            }
            result?.takeIf { it.width > 0 && it.height > 0 }
        } catch (_: EOFException) {
            null
        }

        private fun jpeg(input: BufferedSource): ImageDimensions? {
            input.skip(2)
            // Bound malformed files and unusually large metadata, without buffering the image body.
            var scanned = 2L
            while (scanned < 4 * 1024 * 1024) {
                if (input.readByte().toInt() and 255 != 255) return null
                var marker: Int
                do {
                    marker = input.readByte().toInt() and 255
                    if (++scanned >= 4 * 1024 * 1024) return null
                } while (marker == 255)
                if (marker == 0xDA || marker == 0xD9) return null
                if (marker == 0x01 || marker in 0xD0..0xD8) continue
                val length = input.readShort().toInt() and 65535
                if (length < 2) return null
                if (marker in setOf(0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF)) {
                    if (length < 7) return null
                    input.skip(1)
                    val height = input.readShort().toInt() and 65535
                    val width = input.readShort().toInt() and 65535
                    return ImageDimensions(width, height)
                }
                input.skip((length - 2).toLong())
                scanned += length + 1
            }
            return null
        }
    }
}
