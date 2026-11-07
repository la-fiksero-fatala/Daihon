@file:Suppress("UNUSED", "KotlinConstantConditions")

package eu.kanade.tachiyomi.util.system

import eu.kanade.tachiyomi.BuildConfig

val updaterEnabled: Boolean
    inline get() = !BuildConfig.DEBUG && !isBenchmarkBuildType

val isDebugBuildType: Boolean
    inline get() = BuildConfig.BUILD_TYPE == "debug"

val isReleaseBuildType: Boolean
    inline get() = BuildConfig.BUILD_TYPE == "release"

val isBenchmarkBuildType: Boolean
    inline get() = BuildConfig.BUILD_TYPE.contains("nonMinified") || BuildConfig.BUILD_TYPE.contains("benchmark")
