# Daihon

Daihon is a public Mihon derivative designed exclusively for e-paper displays.
It builds on the paginated library and instant navigation work contributed to Mihon.

The interface uses one high-contrast light appearance, black borders, and stationary page navigation.
App animations stay disabled independently of Android settings. Swipes commit on release, and page
keys and volume keys navigate the active list or dialog. Android system interfaces and external
websites retain their own behavior.

- Library layouts retain fixed empty slots, account for available vertical space, and keep their
  page when sorting. Categories use a paginated chooser. Density, row count, and applicable
  vertical spacing remain adjustable.
- Chapters open first, with Details separate on phones and alongside chapters on tablets.
  Start/Resume and page navigation stay at the bottom.
- Normal reading turns pages instantly. Long strips advance in sections with 20% overlap,
  including while zoomed. Zoom and pan commit on release; animated images remain still.
- Lists, forms, search results, settings, choice dialogs, and license text use discrete pages.
  Category, download, and migration ordering use explicit controls.

Mihon's source, extension, tracking, and backup formats are retained. Existing tracker callback
schemes remain supported. Telemetry is excluded, and update links target this repository.

## Building

Open this Android Studio project using JDK 21 and an installed Android SDK.
Run `./gradlew spotlessCheck testDebugUnitTest :data:verifySqlDelightMigration :app:assembleDebug -Pdist=local`.
For a separately installable preview, run `./gradlew :app:assembleNightly -Pdist=local`.

The application ID is `app.daihon`; Preview builds use `app.daihon.debug` and the launcher label
**Daihon Preview**. Debug builds use `app.daihon.dev`. The build workflow produces a universal
Preview APK as an artifact.

Runtime checks use fictional local titles on phone and tablet layouts, with Android animations
enabled and dark mode enabled to check the fixed product policies. Live tracker accounts and
every third-party extension require further device testing.

## Attribution

Based on [Mihon](https://github.com/mihonapp/mihon) and its contributors' work.
Distributed under the [Apache License 2.0](LICENSE). Existing copyright notices and
third-party licences remain applicable. The app's upstream support links support the Mihon team.
