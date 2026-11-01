package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PaperAlertDialog
import tachiyomi.presentation.core.components.material.PaperButton
import tachiyomi.presentation.core.components.material.PaperIconButton
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun PaperPageControls(
    label: String,
    canPrevious: Boolean,
    canNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJump: () -> Unit,
    modifier: Modifier = Modifier,
    canJump: Boolean = true,
) {
    Row(
        modifier.fillMaxWidth().drawBehind {
            val stroke = 1.dp.toPx()
            drawLine(Color.Black, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), strokeWidth = stroke)
        }.padding(horizontal = 32.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PaperIconButton(onClick = onPrevious, enabled = canPrevious, modifier = Modifier.size(56.dp)) {
            Icon(MaterialSymbols.Rounded.KeyboardArrowUp, stringResource(MR.strings.spen_previous_page))
        }
        PaperButton(
            onClick = onJump,
            enabled = canJump,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        PaperIconButton(onClick = onNext, enabled = canNext, modifier = Modifier.size(56.dp)) {
            Icon(MaterialSymbols.Rounded.KeyboardArrowDown, stringResource(MR.strings.spen_next_page))
        }
    }
}

@Composable
fun PaperJumpDialog(
    index: Int,
    count: Int,
    onSelect: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    itemNumber: Boolean = false,
) {
    var input by remember {
        mutableStateOf(TextFieldValue((index + 1).toString(), TextRange(0, (index + 1).toString().length)))
    }
    val selected = paperPageIndex(input.text, count)
    val confirm = {
        selected?.let {
            onSelect(it)
            onDismissRequest()
        }
    }
    PaperAlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(stringResource(if (itemNumber) MR.strings.daihon_item_number else MR.strings.library_jump_to_page))
        },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        stringResource(
                            if (itemNumber) MR.strings.daihon_entry_input else MR.strings.library_page_input,
                            count,
                        ),
                    )
                },
                singleLine = true,
                isError = input.text.isNotEmpty() && selected == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
            )
        },
        confirmButton = {
            PaperButton(onClick = {
                confirm()
            }, enabled = selected != null) { Text(stringResource(MR.strings.action_ok)) }
        },
        dismissButton = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaperButton(onClick = {
                    onSelect(0)
                    onDismissRequest()
                }) {
                    Text(stringResource(MR.strings.library_jump_to_top))
                }
                PaperButton(onClick = onDismissRequest) { Text(stringResource(MR.strings.action_cancel)) }
            }
        },
    )
}
