package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PaperButton
import tachiyomi.presentation.core.components.material.PaperIconButton
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun NumberValuePicker(
    items: List<Number>,
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    onSelectionChanged: (Int) -> Unit = {},
    onValidityChanged: (Boolean) -> Unit = {},
) = TextValuePicker(items.map { it.toString() }, modifier, startIndex, onSelectionChanged, onValidityChanged)

/** Direct entry and one-step controls; choices never open another paginated list. */
@Composable
fun TextValuePicker(
    items: List<String>,
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    onSelectionChanged: (Int) -> Unit = {},
    onValidityChanged: (Boolean) -> Unit = {},
) {
    var index by rememberSaveable(items) { mutableIntStateOf(startIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))) }
    var input by rememberSaveable(items, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(items.getOrNull(index).orEmpty()))
    }
    val entered = valuePickerInputIndex(items, input.text)
    LaunchedEffect(entered, items) {
        onValidityChanged(entered != null)
        if (entered != null) {
            index = entered
            onSelectionChanged(entered)
        }
    }
    val numeric = items.filter { it.toBigDecimalOrNull() != null }
    val named = items.indices.filter { items[it].toBigDecimalOrNull() == null }
    val numericInput = numeric.isNotEmpty() && named.size <= 1
    val keyboard = when {
        !numericInput -> KeyboardType.Text
        numeric.any { '.' in it } -> KeyboardType.Decimal
        else -> KeyboardType.Number
    }
    val choose: (Int) -> Unit = {
        index = it
        input = TextFieldValue(items[it], TextRange(0, items[it].length))
    }
    val decrease = stringResource(MR.strings.daihon_decrease_value)
    val increase = stringResource(MR.strings.daihon_increase_value)
    Column(modifier.widthIn(min = 180.dp, max = 320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PaperIconButton(
                onClick = { choose(index - 1) },
                enabled = index > 0,
                modifier = Modifier.semantics { contentDescription = decrease },
            ) { Text("−", style = MaterialTheme.typography.titleLarge) }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).onFocusChanged {
                    if (it.isFocused) input = input.copy(selection = TextRange(0, input.text.length))
                },
                enabled = items.isNotEmpty(),
                singleLine = true,
                isError = entered == null,
                label = { Text(stringResource(MR.strings.daihon_select_value)) },
                supportingText = {
                    Text(
                        if (entered == null) {
                            stringResource(MR.strings.daihon_valid_value)
                        } else if (numericInput) {
                            "${numeric.first()} – ${numeric.last()}"
                        } else {
                            stringResource(MR.strings.daihon_filter_value)
                        },
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            )
            PaperIconButton(
                onClick = { choose(index + 1) },
                enabled = index < items.lastIndex,
                modifier = Modifier.semantics { contentDescription = increase },
            ) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
        if (numericInput && named.size == 1) {
            val default = named.single()
            PaperButton(onClick = { choose(default) }, enabled = entered != default) { Text(items[default]) }
        } else if (!numericInput && entered == null) {
            items.indices.filter {
                items[it].contains(input.text.trim(), ignoreCase = true)
            }.take(5).forEach { choice ->
                PaperButton(onClick = { choose(choice) }, modifier = Modifier.fillMaxWidth()) { Text(items[choice]) }
            }
        }
    }
}

internal fun valuePickerInputIndex(items: List<String>, input: String): Int? {
    val text = input.trim()
    items.indexOfFirst { it.equals(text, ignoreCase = true) }.takeIf { it >= 0 }?.let { return it }
    val number = text.replace(',', '.').toBigDecimalOrNull() ?: return null
    return items.indexOfFirst { it.toBigDecimalOrNull()?.compareTo(number) == 0 }.takeIf { it >= 0 }
}
