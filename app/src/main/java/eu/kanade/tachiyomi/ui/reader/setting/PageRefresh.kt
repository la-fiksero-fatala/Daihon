package eu.kanade.tachiyomi.ui.reader.setting

import tachiyomi.i18n.MR

enum class PageRefresh(val title: dev.icerock.moko.resources.StringResource) {
    None(MR.strings.page_refresh_none),
    BlackAndWhite(MR.strings.page_refresh_flash),
    System(MR.strings.page_refresh_system),
}
