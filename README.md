# Daihon

Daihon is a Mihon derivative designed exclusively for e-paper displays.
It builds on the paginated library and instant navigation work contributed to Mihon.

The interface is being converted to one high-contrast light appearance, discrete page navigation,
and no app animations. The project retains Mihon's source, extension, tracking, and backup support.

## Building

Open this Android Studio project using JDK 21 and an installed Android SDK.
Run `./gradlew spotlessCheck testDebugUnitTest :app:assembleDebug -Pdist=local`.
For a separately installable preview, run `./gradlew :app:assembleNightly -Pdist=local`.

The application ID is `app.daihon`; preview builds use `app.daihon.debug`.
Telemetry is excluded. Update links target this repository.

## Attribution

Based on [Mihon](https://github.com/mihonapp/mihon) and its contributors' work.
Distributed under the [Apache License 2.0](LICENSE). Existing copyright notices and
third-party licences remain applicable.
