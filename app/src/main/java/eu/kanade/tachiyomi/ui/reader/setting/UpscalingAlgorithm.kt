package eu.kanade.tachiyomi.ui.reader.setting

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

/** Downscaling keeps the renderer's area sampler, independent of this choice. */
enum class UpscalingAlgorithm(val title: StringResource) {
    CatmullRom(MR.strings.reader_upscaler_default),
    Anime4KDoG(MR.strings.reader_upscaler_anime4k),
}
