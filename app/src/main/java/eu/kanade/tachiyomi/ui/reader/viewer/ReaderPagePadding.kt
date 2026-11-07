package eu.kanade.tachiyomi.ui.reader.viewer

import androidx.compose.runtime.staticCompositionLocalOf
import eu.kanade.tachiyomi.util.image.ImageDimensions

fun readerPagePaddingLimit(width: Int, height: Int): Int = minOf(width, height).coerceAtLeast(0) / 4

/** Retain at least half the viewport on both axes, including after rotation or restoring preferences. */
fun boundedReaderPagePadding(requested: Int, width: Int, height: Int): Int =
    requested.coerceIn(0, readerPagePaddingLimit(width, height))

data class ReaderPaddingSettings(
    val target: Int = 0,
    val dynamic: Boolean = false,
    val minimum: Int = 0,
    val maximum: Int = 64,
)

data class ReaderPageLayout(val left: Int, val top: Int, val right: Int, val bottom: Int, val native: Boolean = false)

/** The fitted axis determines padding; the other axis retains ordinary letterboxing. */
fun readerPageLayout(
    width: Int,
    height: Int,
    settings: ReaderPaddingSettings,
    image: ImageDimensions?,
): ReaderPageLayout {
    val limit = readerPagePaddingLimit(width, height)
    val minimum = settings.minimum.coerceIn(0, limit)
    val maximum = settings.maximum.coerceIn(minimum, limit)
    val target = if (settings.dynamic) {
        settings.target.coerceIn(minimum, maximum)
    } else {
        boundedReaderPagePadding(settings.target, width, height)
    }
    val fallback = ReaderPageLayout(target, target, target, target)
    if (!settings.dynamic || image == null || image.width <= 0 || image.height <= 0 || width <= 0 || height <= 0) {
        return fallback
    }
    val dx = width - image.width
    val dy = height - image.height
    if (dx < 0 || dy < 0) return fallback
    val gap = minOf(dx, dy)
    val near = gap / 2
    val far = gap - near
    if (near < minimum || far > maximum) return fallback
    // Trim an odd extra pixel on the fitted axis so the fitting ratio is exactly 1, never 1 + 1/N.
    return ReaderPageLayout(near, near, if (dx <= dy) far else near, if (dy < dx) far else near, native = true)
}

data class ReaderPaddingInfo(
    val settings: ReaderPaddingSettings,
    val width: Int,
    val height: Int,
    val layout: ReaderPageLayout,
    val expandedZoom: Boolean = false,
) {
    val appliedLayout get() = if (expandedZoom) ReaderPageLayout(0, 0, 0, 0) else layout

    fun matchesViewport(width: Int, height: Int): Boolean {
        val applied = appliedLayout
        return width == this.width - applied.left - applied.right &&
            height == this.height - applied.top - applied.bottom
    }

    fun layoutFor(
        width: Int,
        height: Int,
    ) = readerPageLayout(this.width, this.height, settings, ImageDimensions(width, height))
    fun matchesNativeViewport(width: Int, height: Int, imageWidth: Int, imageHeight: Int): Boolean {
        val expected = layoutFor(imageWidth, imageHeight)
        return expected.native && width == this.width - expected.left - expected.right &&
            height == this.height - expected.top - expected.bottom
    }
}

val LocalReaderPaddingInfo = staticCompositionLocalOf<ReaderPaddingInfo?> { null }
