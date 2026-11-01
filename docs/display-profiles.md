# Device display profiles

Onboarding and Settings → Display share the same device selection and independent
navigation and reader profiles. Defaults leave display modes and refreshes under
firmware control. Existing installations can use Settings or replay onboarding.

Detection checks firmware methods and constants without changing a mode. Selecting
a device family does not force unsupported calls or reveal unavailable controls.
Unknown hardware and blocked interfaces fall back to system-managed behavior.

Supported interfaces:

- Onyx: app-scoped mode getter/setter in `android.onyx.optimization.EInkHelper`,
  mode constants exposed by the firmware, and full refresh through
  `android.onyx.ViewUpdateHelper`. Regal additionally requires a positive capability
  query. The relevant firmware entry points are also used by the
  [Onyx device SDK](https://github.com/onyx-intl/OnyxAndroidDemo).
- Rockchip: `sendOneFullFrame()` on the device display service. Display mode remains
  system-managed; this interface does not enumerate modes.
- NTX: the vendor View invalidation overload and exposed full/grayscale constants.
  Display mode remains system-managed. See the
  [KOReader adapters](https://github.com/koreader/android-luajit-launcher/tree/master/app/src/main/java/org/koreader/launcher/device/epd)
  for implementations using these firmware interfaces.

This is capability detection, not a claim of testing every model or of discovering
all proprietary modes. The BOOX Go 7 is the initial physical target. Emulator
validation cannot establish waveform quality or ghosting behavior.

A profile applies while its activity is resumed. It restores the previous app mode
when leaving, unless device controls changed the mode in the meantime. No global
system-mode setters, hidden-API exemptions, extra permissions, or vendor SDK
modules are required.

Full-refresh intervals count settled screen updates after input or reader content
changes, including newly decoded visible page images. Draws are grouped until
200 ms without a new draw. These are screen updates, not exactly page turns.
Refresh-induced redraws cannot start another refresh: the pending change is consumed
before requesting a panel update. Pausing cancels pending requests. A frequency of
System managed adds no draw observer. Hardware failures are caught and reported
once per active profile.
