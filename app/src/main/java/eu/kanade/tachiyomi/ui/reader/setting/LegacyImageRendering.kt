package eu.kanade.tachiyomi.ui.reader.setting

import androidx.compose.runtime.staticCompositionLocalOf
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

enum class LegacyUpscaler(val title: StringResource) {
    System(MR.strings.reader_scaler_system),
    CatmullRom(MR.strings.reader_scaler_catmull_rom),
    Mitchell(MR.strings.reader_scaler_mitchell),
    Lanczos3(MR.strings.reader_scaler_lanczos3),
}

enum class LegacyImageFilter(val title: StringResource) {
    None(MR.strings.reader_filter_none),
    Sharpen(MR.strings.reader_filter_sharpen),
    Contrast(MR.strings.reader_filter_contrast),
    Monochrome(MR.strings.reader_filter_monochrome),
}

data class LegacyImageRendering(
    val upscaler: LegacyUpscaler = LegacyUpscaler.CatmullRom,
    val filter: LegacyImageFilter = LegacyImageFilter.None,
)

val LocalLegacyImageRendering = staticCompositionLocalOf { LegacyImageRendering() }
