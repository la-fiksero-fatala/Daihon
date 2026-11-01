package eu.kanade.tachiyomi.util.display

/** Invoke the requested reader refresh directly, without device or capability detection. */
internal object BooxPageRefresh {
    fun refresh(): Boolean = runCatching {
        val helper = Class.forName("android.onyx.ViewUpdateHelper")
        val mode = helper.getField("UI_GC_MODE").getInt(null)
        helper.getMethod("repaintEverything", Int::class.javaPrimitiveType!!).invoke(null, mode) != false
    }.getOrDefault(false)
}
