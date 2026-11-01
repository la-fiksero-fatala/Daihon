package eu.kanade.presentation.util

import eu.kanade.presentation.more.settings.screen.SearchableSettings
import tachiyomi.presentation.core.components.PaperNavigationArea
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen

/** Typed screen metadata keeps preferences correct in optimized builds too. */
internal fun VoyagerScreen.paperNavigationArea(): PaperNavigationArea = when (this) {
    is SearchableSettings -> PaperNavigationArea.Settings
    is Screen -> pageNavigationArea
    else -> PaperNavigationArea.Other
}
