package eu.kanade.presentation.more.settings.screen

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.reader.settings.PageRefreshSetting
import eu.kanade.tachiyomi.ui.reader.setting.PageRefresh
import eu.kanade.tachiyomi.util.display.DisplayDevice
import eu.kanade.tachiyomi.util.display.DisplayHardwareProbe
import eu.kanade.tachiyomi.util.display.DisplayMode
import eu.kanade.tachiyomi.util.system.toast
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun deviceDisplayGroup(): Preference.PreferenceGroup {
    val context = LocalContext.current
    val preferences = remember { context.appGraph.deviceDisplayPreferences }
    val device by preferences.device.collectAsState()
    val hardware = remember(device) { DisplayHardwareProbe().detect(context, device) }
    val view = LocalView.current
    val detected = "${Build.MANUFACTURER} ${Build.MODEL}"
    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.display_device),
        preferenceItems = listOf(
            Preference.PreferenceItem.ListPreference(
                preference = preferences.device,
                title = stringResource(MR.strings.display_device_profile),
                entries = DisplayDevice.entries.associateWith { deviceTitle(it) },
            ),
            Preference.PreferenceItem.InfoPreference(
                stringResource(MR.strings.display_device_detected, detected, deviceTitle(hardware.device)),
            ),
            Preference.PreferenceItem.InfoPreference(
                stringResource(
                    if (hardware.device ==
                        DisplayDevice.System
                    ) {
                        MR.strings.display_api_unavailable
                    } else {
                        MR.strings.display_api_detected
                    },
                ),
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(MR.strings.display_refresh_now),
                visible = hardware.canRefresh,
                onClick = {
                    view.postDelayed({
                        if (view.isAttachedToWindow &&
                            !hardware.refresh(view)
                        ) {
                            context.toast(MR.strings.display_apply_failed)
                        }
                    }, 200)
                },
            ),
        ),
    )
}

@Composable
internal fun displayProfileGroup(reader: Boolean): Preference.PreferenceGroup {
    val context = LocalContext.current
    val preferences = remember { context.appGraph.deviceDisplayPreferences }
    val device by preferences.device.collectAsState()
    val hardware = remember(device) { DisplayHardwareProbe().detect(context, device) }
    val modePreference = if (reader) preferences.readerMode else preferences.navigationMode
    val mode by modePreference.collectAsState()
    val pageRefresh by context.appGraph.readerPreferences.pageRefresh.collectAsState()
    val displayModeTitle = stringResource(MR.strings.display_mode)
    val refreshTitle = stringResource(MR.strings.display_full_refresh)
    return Preference.PreferenceGroup(
        title = stringResource(
            if (reader) MR.strings.display_profile_reader else MR.strings.display_profile_navigation,
        ),
        preferenceItems = listOf(
            if (hardware.modes.size > 1) {
                Preference.PreferenceItem.BasicListPreference(
                    title = displayModeTitle,
                    value = hardware.effectiveMode(mode).name,
                    entries = hardware.modes.associate { it.name to modeTitle(it) },
                    onValueChanged = { modePreference.set(DisplayMode.valueOf(it)) },
                )
            } else {
                Preference.PreferenceItem.CustomPreference(displayModeTitle) {
                    TextPreferenceWidget(
                        title = displayModeTitle,
                        subtitle = modeTitle(DisplayMode.System),
                        enabled = false,
                    )
                }
            },
            if (reader && pageRefresh == PageRefresh.System) {
                Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.page_refresh_system_help))
            } else if (hardware.canRefresh) {
                Preference.PreferenceItem.ListPreference(
                    title = refreshTitle,
                    preference = if (reader) preferences.readerRefresh else preferences.navigationRefresh,
                    entries = listOf(0, 1, 5, 10, 20).associateWith {
                        when (it) {
                            0 -> stringResource(MR.strings.display_system_managed)
                            1 -> stringResource(MR.strings.display_refresh_each_update)
                            else -> stringResource(MR.strings.display_refresh_interval, it)
                        }
                    },
                )
            } else {
                Preference.PreferenceItem.CustomPreference(refreshTitle) {
                    TextPreferenceWidget(
                        title = refreshTitle,
                        subtitle = stringResource(MR.strings.display_system_managed),
                        enabled = false,
                    )
                }
            },
            if (reader) {
                Preference.PreferenceItem.CustomPreference(stringResource(MR.strings.page_refresh)) {
                    PageRefreshSetting()
                }
            } else {
                Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.display_profile_help))
            },
        ),
    )
}

@Composable
internal fun deviceTitle(device: DisplayDevice): String = when (device) {
    DisplayDevice.Automatic -> stringResource(MR.strings.display_automatic)
    DisplayDevice.System -> stringResource(MR.strings.display_other_device)
    DisplayDevice.Onyx -> "Onyx BOOX"
    DisplayDevice.Rockchip -> "Meebook / Boyue / Rockchip"
    DisplayDevice.Ntx -> "Tolino / Nook / NTX"
}

@Composable
private fun modeTitle(mode: DisplayMode): String = stringResource(
    when (mode) {
        DisplayMode.System -> MR.strings.display_system_managed
        DisplayMode.Quality -> MR.strings.display_mode_quality
        DisplayMode.Balanced -> MR.strings.display_mode_balanced
        DisplayMode.Regal -> MR.strings.display_mode_regal
        DisplayMode.Fast -> MR.strings.display_mode_fast
        DisplayMode.Fastest -> MR.strings.display_mode_fastest
    },
)
