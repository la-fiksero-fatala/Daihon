# Daihon

Daihon is a public Mihon derivative designed exclusively for e-paper displays.
It builds on the paginated library and instant navigation work contributed to Mihon.

The interface uses one high-contrast light appearance and stationary page navigation.
Panels and dialogs have clear outlines; icons and unselected manga entries stay unframed.
App animations stay disabled independently of Android settings. Swipes commit on release, and page
keys and volume keys navigate the active list or dialog. Android system interfaces and external
websites retain their own behavior.

- Library layouts retain fixed empty slots, account for available vertical space, and keep their
  page when sorting. Categories use a paginated chooser. Density, row count, and applicable
  vertical spacing remain adjustable.
- Chapters open first, with Details separate on phones and alongside chapters on tablets.
  Start/Resume and page navigation stay at the bottom.
- Paged images support physical-pixel edge padding for displays with covered margins.
- Normal reading turns pages instantly. Long strips advance in sections with 20% overlap,
  including while zoomed. Zoom and pan commit on release; animated images remain still.
- Lists, forms, search results, settings, and license text use discrete pages.
  Numeric choices use direct entry and step controls.
  Category, download, and migration ordering use explicit controls.

Mihon's source, extension, tracking, and backup formats are retained. Existing tracker callback
schemes remain supported. Analytics and remote crash reporting are absent from every build. Update links target this repository.

## Building

Open this Android Studio project using JDK 21 and an installed Android SDK.
Run `./gradlew spotlessCheck testDebugUnitTest :data:verifySqlDelightMigration :app:assembleDebug`.
Build production APKs with `./gradlew :app:assembleRelease`. The app is named **Daihon**
and uses `app.daihon`. Debug builds use `app.daihon.dev`.

Versions follow `branch-YYYYMMDD-HHmmss-rREVISION`, using the last commit's UTC timestamp
and Git commit count. Clone with full history so Android's internal version code is reliable.
Settings migrations use a separate schema version.

For signed production builds, configure the ignored `keystore.properties` file with
`storeFile`, `storePassword`, `keyAlias`, and `keyPassword`. Alternatively, set
`DAIHON_KEYSTORE_PATH` and `DAIHON_KEYSTORE_PASSWORD` for a keystore with alias `daihon`.
Without a signing configuration, release APKs are unsigned.

Installable production APKs are available as artifacts on successful GitHub Actions runs.
Push and manual builds use the repository's `DAIHON_KEYSTORE_BASE64` and
`DAIHON_KEYSTORE_PASSWORD` secrets. Pull-request builds produce unsigned APKs.
Artifacts are named after the rolling version. Publishing a GitHub Release is a separate manual action.

Earlier Preview installations use `app.daihon.debug`. They can remain installed alongside
production while library and settings are transferred using backup and restore.

Runtime checks use fictional local titles on phone and tablet layouts, with Android animations
enabled and dark mode enabled to check the fixed product policies. Live tracker accounts and
every third-party extension require further device testing.

## Attribution

Based on [Mihon](https://github.com/mihonapp/mihon) and its contributors' work.
Distributed under the [Apache License 2.0](LICENSE). Existing copyright notices and
third-party licences remain applicable. The app's upstream support links support the Mihon team.
