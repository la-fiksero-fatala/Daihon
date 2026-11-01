package eu.kanade.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import mihon.app.di.appGraph
import tachiyomi.presentation.core.components.LocalPaperNavigationSettings
import tachiyomi.presentation.core.components.PaperNavigationSettings
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun PaperNavigationPreferences(content: @Composable () -> Unit) {
    if (LocalInspectionMode.current) {
        content()
        return
    }
    val preferences = LocalContext.current.appGraph.uiPreferences
    val hidden by preferences.hiddenPageNavigation.collectAsState()
    val height by preferences.pageNavigationHeight.collectAsState()
    CompositionLocalProvider(
        LocalPaperNavigationSettings provides PaperNavigationSettings(hidden, height),
        content = content,
    )
}
