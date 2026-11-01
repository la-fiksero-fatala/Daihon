package eu.kanade.presentation.reader.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.ui.reader.viewer.readerPagePaddingLimit
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.NumberValuePicker
import tachiyomi.presentation.core.components.material.PaperAlertDialog
import tachiyomi.presentation.core.components.material.PaperButton
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun PagePaddingSetting() {
    val context = LocalContext.current
    val preference = remember { context.appGraph.readerPreferences.pagePadding }
    val padding by preference.collectAsState()
    val window = LocalWindowInfo.current
    var open by remember { mutableStateOf(false) }
    var limit by remember { mutableIntStateOf(0) }
    val title = stringResource(MR.strings.pref_page_padding)
    TextPreferenceWidget(
        title = title,
        subtitle = stringResource(MR.strings.page_padding_pixels, padding),
        onPreferenceClick = {
            val size = window.containerSize
            limit = readerPagePaddingLimit(size.width, size.height)
            open = true
        },
    )
    if (open) {
        var selected by remember { mutableIntStateOf(padding.coerceIn(0, limit)) }
        var valid by remember { mutableStateOf(true) }
        val values = remember(limit) { (0..limit).toList() }
        PaperAlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                NumberValuePicker(
                    items = values,
                    startIndex = selected,
                    onSelectionChanged = { selected = values[it] },
                    onValidityChanged = { valid = it },
                )
            },
            confirmButton = {
                PaperButton(enabled = valid, onClick = {
                    preference.set(selected)
                    open = false
                }) { Text(stringResource(MR.strings.action_ok)) }
            },
            dismissButton = {
                PaperButton(onClick = { open = false }) { Text(stringResource(MR.strings.action_cancel)) }
            },
        )
    }
}
