# Reader refresh

The reader offers None, Black and white, and BOOX refresh under Reader → Page refresh.
BOOX refresh is always selectable. There is no manufacturer, device-profile, or
capability check. The refresh command is invoked only when that option is selected.
A failed request displays a message once per reader session.

The command calls `android.onyx.ViewUpdateHelper.repaintEverything(UI_GC_MODE)`.
It runs after the selected manga image is decoded and drawing has settled, including
the first page. Long-strip section changes also request a refresh. Menus and chapter
transition screens do not trigger it. Pending requests are cancelled on reader exit.

Daihon does not set vendor display modes or manage navigation refreshes. Those are
controlled by Android and the device's own settings. Disable the device's automatic
app refreshes when using page-only BOOX refresh to avoid duplicate refreshes.
