package eu.kanade.tachiyomi.ui.reader.viewer

/**
 * Settings every long strip viewer uses.
 */
data class LongStripSettings(
    val cropBorders: Boolean,
    val cropBrightnessThreshold: Int? = null,
    val zoomOutDisabled: Boolean,
)
