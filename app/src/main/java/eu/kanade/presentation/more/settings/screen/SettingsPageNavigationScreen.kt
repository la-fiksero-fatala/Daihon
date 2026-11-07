package eu.kanade.presentation.more.settings.screen

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.library.LibraryTab
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.PaperNavigationArea
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

object SettingsPageNavigationScreen : SearchableSettings {
    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.page_navigation_settings

    @Composable
    override fun getPreferences(): List<Preference> {
        val preferences = LocalContext.current.appGraph.uiPreferences
        val hidden by preferences.hiddenPageNavigation.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val root = remember(navigator) { generateSequence(navigator) { it.parent }.last() }
        val activityScope = (LocalActivity.current as? LifecycleOwner)?.lifecycleScope
        return listOf(
            Preference.PreferenceItem.SliderPreference(
                preference = preferences.pageNavigationHeight,
                title = stringResource(MR.strings.page_navigation_height),
                valueRange = 48..112,
                valueText = { stringResource(MR.strings.page_navigation_dp, it) },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.pref_category_library),
                subtitle = stringResource(MR.strings.page_navigation_library_settings),
                onClick = {
                    activityScope?.launch {
                        root.popUntilRoot()
                        HomeScreen.openTab(HomeScreen.Tab.Library())
                        LibraryTab.openDisplaySettings()
                    }
                },
            ),
        ) + listOf(
            PaperNavigationArea.Details to MR.strings.daihon_details,
            PaperNavigationArea.Chapters to MR.strings.chapters,
            PaperNavigationArea.Browse to MR.strings.label_sources,
            PaperNavigationArea.Updates to MR.strings.label_recent_updates,
            PaperNavigationArea.History to MR.strings.label_recent_manga,
            PaperNavigationArea.Settings to MR.strings.label_settings,
            PaperNavigationArea.Dialogs to MR.strings.page_navigation_dialogs,
            PaperNavigationArea.Other to MR.strings.page_navigation_other,
        ).map { (area, titleRes) ->
            val title = stringResource(titleRes)
            Preference.PreferenceItem.CustomPreference(title) {
                SwitchPreferenceWidget(
                    title = title,
                    checked = area.name !in hidden,
                    onCheckedChanged = { show ->
                        preferences.hiddenPageNavigation.set(
                            if (show) {
                                preferences.hiddenPageNavigation.get() - area.name
                            } else {
                                preferences.hiddenPageNavigation.get() + area.name
                            },
                        )
                    },
                )
            }
        }
    }
}
