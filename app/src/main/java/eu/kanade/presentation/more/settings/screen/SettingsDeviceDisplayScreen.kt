package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import eu.kanade.presentation.more.settings.Preference
import tachiyomi.i18n.MR

object SettingsDeviceDisplayScreen : SearchableSettings {
    @Composable
    @ReadOnlyComposable
    override fun getTitleRes() = MR.strings.display_device_settings

    @Composable
    override fun getPreferences(): List<Preference> = listOf(
        deviceDisplayGroup(),
        displayProfileGroup(reader = false),
        displayProfileGroup(reader = true),
    )
}
