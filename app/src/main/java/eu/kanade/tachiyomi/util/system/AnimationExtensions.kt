package eu.kanade.tachiyomi.util.system

import android.content.Context

/** Daihon never follows the system animation scale. */
val Context.animatorDurationScale: Float
    get() = 0f
