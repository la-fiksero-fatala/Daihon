package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.ui.reader.setting.PageRefresh
import mihon.app.di.appGraph
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PaperAlertDialog
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun PageRefreshSetting() {
    val context = LocalContext.current
    val preference = remember { context.appGraph.readerPreferences.pageRefresh }
    val selected by preference.collectAsState()
    var open by remember { mutableStateOf(false) }
    val title = stringResource(MR.strings.page_refresh)
    TextPreferenceWidget(title = title, subtitle = stringResource(selected.title), onPreferenceClick = { open = true })
    if (open) {
        PaperAlertDialog(
            onDismissRequest = { open = false },
            title = { Text(title) },
            text = {
                androidx.compose.foundation.layout.Column {
                    PageRefresh.entries.forEach { mode ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .selectable(selected == mode, role = Role.RadioButton) {
                                    preference.set(mode)
                                    open = false
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected == mode, onClick = null)
                            Text(stringResource(mode.title), Modifier.padding(start = 12.dp))
                        }
                    }
                    Text(stringResource(MR.strings.page_refresh_system_help))
                }
            },
            confirmButton = {},
        )
    }
}
