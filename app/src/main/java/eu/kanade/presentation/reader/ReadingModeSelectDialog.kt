package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.manga.model.readingMode
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.reader.components.ModeSelectionDialog
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsViewModel
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.PaperLazyColumn
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.components.material.PaperSurface as Surface

private val ReadingModesWithoutDefault = ReadingMode.entries - ReadingMode.DEFAULT

@Composable
fun ReadingModeSelectDialog(
    onDismissRequest: () -> Unit,
    viewModel: ReaderSettingsViewModel,
    onChange: (StringResource) -> Unit,
) {
    val manga by viewModel.mangaFlow.collectAsState()
    val readingMode = remember(manga) { ReadingMode.fromPreference(manga?.readingMode?.toInt()) }

    AdaptiveSheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.widthIn(max = 360.dp),
        title = stringResource(MR.strings.pref_category_reading_mode),
    ) {
        DialogContent(
            readingMode = readingMode,
            onChangeReadingMode = {
                viewModel.setReadingMode(it)
                onChange(it.stringRes)
                onDismissRequest()
            },
        )
    }
}

@Composable
private fun DialogContent(
    readingMode: ReadingMode,
    onChangeReadingMode: (ReadingMode) -> Unit,
) {
    var selected by remember { mutableStateOf(readingMode) }

    ModeSelectionDialog(
        onUseDefault = { onChangeReadingMode(ReadingMode.DEFAULT) }.takeIf { readingMode != ReadingMode.DEFAULT },
        onApply = { onChangeReadingMode(selected) },
    ) {
        PaperLazyColumn(
            modifier = Modifier.weight(1f, fill = false).selectableGroup(),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(ReadingModesWithoutDefault) { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                        .selectable(selected = mode == selected, role = Role.RadioButton, onClick = { selected = mode })
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = mode == selected, onClick = null)
                    Icon(mode.icon, null, modifier = Modifier.padding(start = 8.dp, end = 16.dp))
                    Text(stringResource(mode.stringRes), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun DialogContentPreview() {
    TachiyomiPreviewTheme {
        Surface {
            Column {
                DialogContent(
                    readingMode = ReadingMode.DEFAULT,
                    onChangeReadingMode = {},
                )

                DialogContent(
                    readingMode = ReadingMode.LEFT_TO_RIGHT,
                    onChangeReadingMode = {},
                )
            }
        }
    }
}
