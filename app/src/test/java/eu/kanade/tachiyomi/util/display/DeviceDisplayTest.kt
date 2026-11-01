package eu.kanade.tachiyomi.util.display

import eu.kanade.domain.ui.DeviceDisplayPreferences
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class DeviceDisplayTest {
    @BeforeEach
    fun resetFirmware() {
        Firmware.mode = 7
        Firmware.allowChanges = true
        Firmware.regal = false
        Firmware.ignoreChanges = false
    }

    @Test
    fun `navigation and reader profiles have separate keys and safe defaults`() {
        val store = InMemoryPreferenceStore()
        val preferences = DeviceDisplayPreferences(store)
        preferences.device.get() shouldBe DisplayDevice.Automatic
        preferences.navigationMode.get() shouldBe DisplayMode.System
        preferences.readerRefresh.get() shouldBe 0
        preferences.navigationMode.set(DisplayMode.Fast)
        preferences.readerMode.set(DisplayMode.Quality)
        preferences.navigationRefresh.set(5)
        preferences.readerRefresh.set(1)

        (preferences.navigationMode.key() == preferences.readerMode.key()) shouldBe false
        (preferences.navigationRefresh.key() == preferences.readerRefresh.key()) shouldBe false
        preferences.navigationMode.get() shouldBe DisplayMode.Fast
        preferences.readerMode.get() shouldBe DisplayMode.Quality
        preferences.navigationRefresh.get() shouldBe 5
        preferences.readerRefresh.get() shouldBe 1
    }

    @Test
    fun `missing firmware APIs expose only system managed mode`() {
        val hardware = DisplayHardwareProbe { null }.onyx()
        hardware shouldBe null
        DisplayHardware().modes shouldBe setOf(DisplayMode.System)
        DisplayHardware().effectiveMode(DisplayMode.Fastest) shouldBe DisplayMode.System
    }

    @Test
    fun `probe uses actual firmware fields and gates Regal on its capability query`() {
        val hardware = probe().onyx()!!
        hardware.modes shouldBe setOf(DisplayMode.System, DisplayMode.Quality, DisplayMode.Fast)
        hardware.canRefresh shouldBe true
        Firmware.regal = true
        probe().onyx()!!.modes shouldBe
            setOf(DisplayMode.System, DisplayMode.Quality, DisplayMode.Fast, DisplayMode.Regal)
        // No UPDATE_MODE_X field exists on this firmware, so Fastest must remain unavailable.
        hardware.effectiveMode(DisplayMode.Fastest) shouldBe DisplayMode.System
    }

    @Test
    fun `profile override restores the previous device mode`() {
        val restore = probe().onyx()!!.apply(DisplayMode.Fast)!!
        Firmware.mode shouldBe 3
        restore()
        Firmware.mode shouldBe 7
    }

    @Test
    fun `leaving a profile preserves changes made through system controls`() {
        val restore = probe().onyx()!!.apply(DisplayMode.Fast)!!
        Firmware.mode = 20
        restore()
        Firmware.mode shouldBe 20
    }

    @Test
    fun `system managed and unsupported modes do not write to the firmware`() {
        val hardware = probe().onyx()!!
        hardware.apply(DisplayMode.System)!!.invoke()
        hardware.apply(hardware.effectiveMode(DisplayMode.Fastest))!!.invoke()
        Firmware.mode shouldBe 7
    }

    @Test
    fun `firmware rejection is reported without changing the original mode`() {
        Firmware.allowChanges = false
        probe().onyx()!!.apply(DisplayMode.Fast) shouldBe null
        Firmware.mode shouldBe 7
    }

    @Test
    fun `a silently ignored firmware setting is reported as a failure`() {
        Firmware.ignoreChanges = true
        probe().onyx()!!.apply(DisplayMode.Fast) shouldBe null
        Firmware.mode shouldBe 7
    }

    @Test
    fun `full refresh cadence counts settled changes and restarts after each refresh`() {
        val cadence = DisplayRefreshCadence(3)
        List(7) {
            cadence.contentChanged()
            cadence.screenChanged()
        } shouldBe listOf(false, false, true, false, false, true, false)
    }

    @Test
    fun `system managed never requests full refreshes and every update requests one`() {
        List(30) { DisplayRefreshCadence(0).screenChanged() }.any { it } shouldBe false
        val cadence = DisplayRefreshCadence(1)
        List(3) {
            cadence.contentChanged()
            cadence.screenChanged()
        } shouldBe listOf(true, true, true)
        DisplayRefreshCadence(-1).screenChanged() shouldBe false
    }

    @Test
    fun `full refresh redraws cannot start a refresh loop`() {
        val cadence = DisplayRefreshCadence(1)
        cadence.screenChanged() shouldBe true
        List(20) { cadence.screenChanged() }.any { it } shouldBe false
        cadence.contentChanged()
        cadence.screenChanged() shouldBe true
        cadence.screenChanged() shouldBe false
    }

    @Test
    fun `multiple frames and input events in one settled update count once`() {
        val cadence = DisplayRefreshCadence(2)
        repeat(10) { cadence.contentChanged() }
        cadence.screenChanged() shouldBe false
        cadence.screenChanged() shouldBe false
        cadence.contentChanged()
        cadence.screenChanged() shouldBe true
    }

    private fun probe() = DisplayHardwareProbe {
        when (it) {
            "android.onyx.optimization.EInkHelper", "android.onyx.hardware.DeviceController" -> Firmware::class.java
            "android.onyx.optimization.Constant" -> Modes::class.java
            "android.onyx.ViewUpdateHelper" -> Panel::class.java
            else -> null
        }
    }

    class Firmware {
        companion object {
            var mode = 7
            var allowChanges = true
            var regal = false
            var ignoreChanges = false

            @JvmStatic fun getAppScopeRefreshMode() = mode

            @JvmStatic fun setAppScopeRefreshMode(value: Int): Boolean {
                if (allowChanges && !ignoreChanges) mode = value
                return allowChanges
            }

            @JvmStatic fun supportRegal() = regal
        }
    }

    class Modes {
        companion object {
            @JvmField val UPDATE_MODE_DEFAULT = 7

            @JvmField val UPDATE_MODE_A2 = 3

            @JvmField val UPDATE_MODE_REGAL = 4
        }
    }

    class Panel {
        companion object {
            @JvmField val UI_GC_MODE = 1

            @JvmStatic fun repaintEverything(mode: Int) = mode == UI_GC_MODE
        }
    }
}
