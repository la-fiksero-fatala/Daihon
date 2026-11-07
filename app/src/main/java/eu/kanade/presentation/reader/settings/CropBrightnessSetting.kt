package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsViewModel
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.TextValuePicker
import tachiyomi.presentation.core.components.material.PaperAlertDialog
import tachiyomi.presentation.core.components.material.PaperButton
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun CropBordersSetting(viewModel: ReaderSettingsViewModel, preference: Preference<Boolean>) {
    val enabled by preference.collectAsState()
    val manga by viewModel.mangaFlow.collectAsState()
    val threshold by viewModel.cropBrightnessThreshold.collectAsState()
    val defaultLabel = stringResource(MR.strings.label_default)
    val title = stringResource(MR.strings.daihon_crop_brightness)
    var open by rememberSaveable(manga?.id) { mutableStateOf(false) }

    CheckboxItem(label = stringResource(MR.strings.pref_crop_borders), pref = preference)
    TextPreferenceWidget(
        title = title,
        subtitle = threshold?.toString() ?: defaultLabel,
        enabled = enabled && manga != null,
        onPreferenceClick = { open = true },
    )
    if (open) {
        val values = remember(defaultLabel) { listOf(defaultLabel) + (0..255).map(Int::toString) }
        var selected by remember { mutableIntStateOf((threshold ?: -1) + 1) }
        var valid by remember { mutableStateOf(true) }
        PaperAlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(MR.strings.daihon_crop_brightness_help))
                    TextValuePicker(
                        items = values,
                        startIndex = selected,
                        onSelectionChanged = { selected = it },
                        onValidityChanged = { valid = it },
                    )
                }
            },
            confirmButton = {
                PaperButton(enabled = valid, onClick = {
                    viewModel.setCropBrightnessThreshold((selected - 1).takeIf { it >= 0 })
                    open = false
                }) { Text(stringResource(MR.strings.action_ok)) }
            },
        )
    }
}
