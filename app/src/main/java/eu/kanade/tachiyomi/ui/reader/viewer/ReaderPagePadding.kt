package eu.kanade.tachiyomi.ui.reader.viewer

fun readerPagePaddingLimit(width: Int, height: Int): Int = minOf(width, height).coerceAtLeast(0) / 4

/** Retain at least half the viewport on both axes, including after rotation or restoring preferences. */
fun boundedReaderPagePadding(requested: Int, width: Int, height: Int): Int =
    requested.coerceIn(0, readerPagePaddingLimit(width, height))
