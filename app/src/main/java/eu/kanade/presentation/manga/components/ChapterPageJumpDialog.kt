package eu.kanade.presentation.manga.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import eu.kanade.presentation.util.formatChapterNumber
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.PaperJumpActions
import tachiyomi.presentation.core.components.TextValuePicker
import tachiyomi.presentation.core.components.material.PaperAlertDialog
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ChapterPageJumpDialog(
    numbers: ChapterPageNumbers,
    currentRow: Int,
    onSelectRow: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val labels = remember(numbers) { numbers.choices.map(::formatChapterNumber) }
    var selected by remember(numbers) { mutableIntStateOf(numbers.choices.indexOf(numbers.numberAtRow(currentRow))) }
    var valid by remember { mutableStateOf(true) }
    PaperAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.show_chapter_number)) },
        text = {
            TextValuePicker(
                items = labels,
                startIndex = selected,
                onSelectionChanged = { selected = it },
                onValidityChanged = { valid = it },
            )
        },
        confirmButton = {
            PaperJumpActions(
                onTop = {
                    onSelectRow(0)
                    onDismissRequest()
                },
                onCancel = onDismissRequest,
                onConfirm = {
                    onSelectRow(numbers.rowForNumber(numbers.choices[selected]))
                    onDismissRequest()
                },
                confirmEnabled = valid,
            )
        },
    )
}
