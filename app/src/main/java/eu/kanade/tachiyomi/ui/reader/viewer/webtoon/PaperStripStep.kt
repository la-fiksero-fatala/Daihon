package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

/** Leave one fifth of the visible content in place, including when the strip is zoomed. */
internal fun paperStripStep(viewportHeight: Int, scale: Float): Int {
    require(scale.isFinite() && scale > 0f)
    return (viewportHeight.coerceAtLeast(0) / scale * .8f).toInt().coerceAtLeast(1)
}
