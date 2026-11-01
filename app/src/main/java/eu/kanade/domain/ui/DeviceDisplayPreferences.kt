package eu.kanade.domain.ui

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.util.display.DisplayDevice
import eu.kanade.tachiyomi.util.display.DisplayMode
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.getEnum

@Inject
@SingleIn(AppScope::class)
class DeviceDisplayPreferences(preferenceStore: PreferenceStore) {
    val device = preferenceStore.getEnum("display_device", DisplayDevice.Automatic)
    val navigationMode = preferenceStore.getEnum("display_navigation_mode", DisplayMode.System)
    val readerMode = preferenceStore.getEnum("display_reader_mode", DisplayMode.System)
    val navigationRefresh = preferenceStore.getInt("display_navigation_refresh", 0)
    val readerRefresh = preferenceStore.getInt("display_reader_refresh", 0)
}
