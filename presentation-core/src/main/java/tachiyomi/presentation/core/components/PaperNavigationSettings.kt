package tachiyomi.presentation.core.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/** Library visibility belongs to its own Hidden/Bar/Buttons setting. */
enum class PaperNavigationArea { Library, Details, Chapters, Browse, Updates, History, Settings, Dialogs, Other }

@Immutable
data class PaperNavigationSettings(
    val hidden: Set<String> = emptySet(),
    val heightDp: Int = 72,
) {
    val height get() = heightDp.coerceIn(48, 112).dp
    fun visible(area: PaperNavigationArea) = area == PaperNavigationArea.Library || area.name !in hidden
}

val LocalPaperNavigationSettings = staticCompositionLocalOf { PaperNavigationSettings() }
val LocalPaperNavigationArea = staticCompositionLocalOf { PaperNavigationArea.Other }
