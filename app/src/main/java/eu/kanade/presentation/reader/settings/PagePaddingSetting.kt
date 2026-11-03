package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.ui.reader.viewer.readerPagePaddingLimit
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.NumberValuePicker
import tachiyomi.presentation.core.components.material.PaperAlertDialog
import tachiyomi.presentation.core.components.material.PaperButton
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun PagePaddingSetting() {
    val context = LocalContext.current
    val preferences = remember { context.appGraph.readerPreferences }
    val preference = preferences.pagePadding
    val dynamicPreference by preferences.dynamicPagePadding.collectAsState()
    val webGpu by context.appGraph.basePreferences.highQualityRenderer.collectAsState()
    val dynamic = dynamicPreference && !webGpu
    val minimum by preferences.minimumPagePadding.collectAsState()
    val maximum by preferences.maximumPagePadding.collectAsState()
    val padding by preference.collectAsState()
    val window = LocalWindowInfo.current
    var open by remember { mutableStateOf(false) }
    var limit by remember { mutableIntStateOf(0) }
    val title = stringResource(if (dynamic) MR.strings.pref_target_page_padding else MR.strings.pref_page_padding)
    TextPreferenceWidget(
        title = title,
        subtitle = stringResource(MR.strings.page_padding_pixels, padding),
        onPreferenceClick = {
            val size = window.containerSize
            limit = readerPagePaddingLimit(size.width, size.height)
            open = true
        },
    )
    var dynamicOpen by remember { mutableStateOf(false) }
    TextPreferenceWidget(
        title = stringResource(MR.strings.pref_dynamic_page_padding),
        subtitle = when {
            webGpu -> stringResource(MR.strings.dynamic_page_padding_standard_reader)
            dynamic -> stringResource(MR.strings.dynamic_page_padding_range, minimum, maximum)
            else -> stringResource(MR.strings.off)
        },
        enabled = !webGpu,
        onPreferenceClick = {
            val size = window.containerSize
            limit = readerPagePaddingLimit(size.width, size.height)
            dynamicOpen = true
        },
    )
    if (dynamicOpen) {
        var enabled by remember { mutableStateOf(dynamic) }
        var min by remember { mutableIntStateOf(minimum.coerceIn(0, limit)) }
        var max by remember { mutableIntStateOf(maximum.coerceIn(min, limit)) }
        var minValid by remember { mutableStateOf(true) }
        var maxValid by remember { mutableStateOf(true) }
        val values = remember(limit) { (0..limit).toList() }
        PaperAlertDialog(
            onDismissRequest = { dynamicOpen = false },
            title = { Text(stringResource(MR.strings.pref_dynamic_page_padding)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CheckboxItem(stringResource(MR.strings.pref_dynamic_page_padding_enable), enabled) {
                        enabled =
                            !enabled
                    }
                    if (enabled) {
                        Text(stringResource(MR.strings.pref_minimum_page_padding))
                        NumberValuePicker(
                            items = values,
                            startIndex = min,
                            onSelectionChanged = { min = values[it] },
                            onValidityChanged = { minValid = it },
                        )
                        Text(stringResource(MR.strings.pref_maximum_page_padding))
                        NumberValuePicker(
                            items = values,
                            startIndex = max,
                            onSelectionChanged = { max = values[it] },
                            onValidityChanged = { maxValid = it },
                        )
                    }
                    Text(stringResource(MR.strings.dynamic_page_padding_help))
                }
            },
            confirmButton = {
                PaperButton(enabled = !enabled || (minValid && maxValid && min <= max), onClick = {
                    if (enabled) {
                        preferences.minimumPagePadding.set(min)
                        preferences.maximumPagePadding.set(max)
                        preference.set(padding.coerceIn(min, max))
                    }
                    preferences.dynamicPagePadding.set(enabled)
                    dynamicOpen = false
                }) { Text(stringResource(MR.strings.action_ok)) }
            },
            dismissButton = {
                PaperButton(onClick = { dynamicOpen = false }) { Text(stringResource(MR.strings.action_cancel)) }
            },
        )
    }
    if (open) {
        val lower = if (dynamic) minimum.coerceIn(0, limit) else 0
        val upper = if (dynamic) maximum.coerceIn(lower, limit) else limit
        var selected by remember { mutableIntStateOf(padding.coerceIn(lower, upper)) }
        var valid by remember { mutableStateOf(true) }
        val values = remember(lower, upper) { (lower..upper).toList() }
        PaperAlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                NumberValuePicker(
                    items = values,
                    startIndex = selected - lower,
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
