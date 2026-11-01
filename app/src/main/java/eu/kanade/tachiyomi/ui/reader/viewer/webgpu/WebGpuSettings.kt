package eu.kanade.tachiyomi.ui.reader.viewer.webgpu

import eu.kanade.tachiyomi.ui.reader.setting.UpscalingAlgorithm

/**
 * Settings both WebGPU viewers use, kept in each of their own settings.
 */
data class WebGpuSettings(
    val doubleTapGesturesDisabled: Boolean,
    val upscalingAlgorithm: UpscalingAlgorithm,
    val imageFilters: List<WebGpuShader>,
    val tileFilters: List<WebGpuShader>,
)
