package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.ui.reader.setting.UpscalingAlgorithm
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.RadioItem
import tachiyomi.presentation.core.components.material.PaperAlertDialog
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun UpscalingAlgorithmSetting() {
    val context = LocalContext.current
    val preference = remember { context.appGraph.readerPreferences.upscalingAlgorithm }
    val selected by preference.collectAsState()
    var open by remember { mutableStateOf(false) }
    val title = stringResource(MR.strings.reader_upscaling_algorithm)
    TextPreferenceWidget(title = title, subtitle = stringResource(selected.title), onPreferenceClick = { open = true })
    if (open) {
        PaperAlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                Column {
                    UpscalingAlgorithm.entries.forEach { mode ->
                        RadioItem(selected = selected == mode, onClick = {
                            preference.set(mode)
                            open = false
                        }, label = stringResource(mode.title))
                    }
                    Text(stringResource(MR.strings.reader_upscaling_help))
                }
            },
            confirmButton = {},
        )
    }
}
