package eu.kanade.tachiyomi.util.display

import android.content.Context
import android.view.View
import java.lang.reflect.Method

enum class DisplayDevice { Automatic, System, Onyx, Rockchip, Ntx }

enum class DisplayMode { System, Quality, Balanced, Regal, Fast, Fastest }

/** Capabilities come from the firmware, never from the presence of an SDK bundled in the APK. */
class DisplayHardware(
    val device: DisplayDevice = DisplayDevice.System,
    private val modeValues: Map<DisplayMode, Int> = emptyMap(),
    private val readMode: (() -> Int)? = null,
    private val writeMode: ((Int) -> Boolean)? = null,
    private val fullRefresh: ((View) -> Boolean)? = null,
) {
    val modes: Set<DisplayMode> = setOf(DisplayMode.System) + modeValues.keys
    val canRefresh: Boolean get() = fullRefresh != null
    fun effectiveMode(requested: DisplayMode): DisplayMode = requested.takeIf { it in modes } ?: DisplayMode.System

    /** Restore only our own override, preserving changes made in the device's controls meanwhile. */
    fun apply(mode: DisplayMode): (() -> Unit)? {
        val value = modeValues[mode] ?: return {}
        val previous = runCatching { readMode?.invoke() }.getOrNull() ?: return null
        val applied = runCatching {
            writeMode?.invoke(value) == true && readMode?.invoke() == value
        }.getOrDefault(false)
        if (!applied) {
            runCatching { writeMode?.invoke(previous) }
            return null
        }
        return {
            runCatching {
                if (readMode?.invoke() == value) writeMode?.invoke(previous)
            }
        }
    }

    fun refresh(view: View): Boolean = runCatching { fullRefresh?.invoke(view) == true }.getOrDefault(false)
}

/** Firmware entry points used by Onyx's device SDK and KOReader's Rockchip/NTX adapters.
 * Onyx: https://github.com/onyx-intl/OnyxAndroidDemo
 * Rockchip/NTX: https://github.com/koreader/android-luajit-launcher/tree/master/app/src/main/java/org/koreader/launcher/device/epd
 * No hidden-API exemptions, global system-mode setters, or additional permissions are used.
 */
class DisplayHardwareProbe(
    private val resolve: (String) -> Class<*>? = { runCatching { Class.forName(it) }.getOrNull() },
) {
    fun detect(context: Context, requested: DisplayDevice): DisplayHardware = when (requested) {
        DisplayDevice.Automatic -> onyx() ?: rockchip(context) ?: ntx() ?: DisplayHardware()
        DisplayDevice.Onyx -> onyx() ?: DisplayHardware()
        DisplayDevice.Rockchip -> rockchip(context) ?: DisplayHardware()
        DisplayDevice.Ntx -> ntx() ?: DisplayHardware()
        DisplayDevice.System -> DisplayHardware()
    }

    internal fun onyx(): DisplayHardware? {
        val helper = resolve("android.onyx.optimization.EInkHelper")
        val constants = resolve("android.onyx.optimization.Constant")
        val viewHelper = resolve("android.onyx.ViewUpdateHelper")
        val read = helper.method("getAppScopeRefreshMode")
        val write = helper.method("setAppScopeRefreshMode", Int::class.javaPrimitiveType!!)
        val initial = runCatching { read?.invoke(null) as? Int }.getOrNull()
        val regal = runCatching {
            resolve("android.onyx.hardware.DeviceController").method("supportRegal")?.invoke(null) == true
        }.getOrDefault(false)
        val modes = if (initial != null && write != null) {
            onyxFields.mapNotNull { (mode, field) ->
                if (mode == DisplayMode.Regal && !regal) return@mapNotNull null
                constants.intField(field)?.let { mode to it }
            }.toMap()
        } else {
            emptyMap()
        }
        val refresh = viewHelper.method("repaintEverything", Int::class.javaPrimitiveType!!)
        val gc = viewHelper.intField("UI_GC_MODE")
        if (modes.isEmpty() && (refresh == null || gc == null)) return null
        return DisplayHardware(
            device = DisplayDevice.Onyx,
            modeValues = modes,
            readMode = { read!!.invoke(null) as Int },
            writeMode = { write!!.invoke(null, it) != false },
            fullRefresh = if (refresh != null && gc != null) ({ refresh.invoke(null, gc) != false }) else null,
        )
    }

    private fun rockchip(context: Context): DisplayHardware? {
        val type = resolve("android.os.EinkManager") ?: return null
        val refresh = type.method("sendOneFullFrame") ?: return null
        val service = runCatching { context.getSystemService("eink") }.getOrNull() ?: return null
        if (!type.isInstance(service)) return null
        // This API uses the mode selected in the device's system controls; it cannot enumerate modes.
        return DisplayHardware(DisplayDevice.Rockchip, fullRefresh = { refresh.invoke(service) != false })
    }

    private fun ntx(): DisplayHardware? {
        val type = resolve("android.view.View") ?: return null
        val intType = Int::class.javaPrimitiveType!!
        val refresh =
            type.method(
                "postInvalidateDelayed",
                Long::class.javaPrimitiveType!!,
                intType,
                intType,
                intType,
                intType,
                intType,
            )
                ?: return null
        val full = type.intField("EINK_UPDATE_MODE_FULL") ?: return null
        val grayscale = type.intField("EINK_WAVEFORM_MODE_GC16") ?: return null
        return DisplayHardware(
            DisplayDevice.Ntx,
            fullRefresh = { view ->
                refresh.invoke(view, 0L, 0, 0, view.width, view.height, full or grayscale)
                true
            },
        )
    }

    private fun Class<*>?.method(name: String, vararg parameters: Class<*>): Method? =
        runCatching { this?.getMethod(name, *parameters) }.getOrNull()

    private fun Class<*>?.intField(name: String): Int? = runCatching { this?.getField(name)?.getInt(null) }.getOrNull()

    companion object {
        private val onyxFields = mapOf(
            DisplayMode.Quality to "UPDATE_MODE_DEFAULT",
            DisplayMode.Balanced to "UPDATE_MODE_DU",
            DisplayMode.Regal to "UPDATE_MODE_REGAL",
            DisplayMode.Fast to "UPDATE_MODE_A2",
            DisplayMode.Fastest to "UPDATE_MODE_X",
        )
    }
}
