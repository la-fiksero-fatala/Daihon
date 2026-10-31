package tachiyomi.presentation.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PaperAlertDialog
import tachiyomi.presentation.core.components.material.PaperButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clearFocusOnSoftKeyboardHide
import tachiyomi.presentation.core.util.clickableNoIndication
import tachiyomi.presentation.core.util.showSoftKeyboard
import kotlin.math.absoluteValue

@Composable
fun WheelNumberPicker(
    items: List<Number>,
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    size: DpSize = DpSize(128.dp, 128.dp),
    onSelectionChanged: (index: Int) -> Unit = {},
    backgroundContent: (@Composable (size: DpSize) -> Unit)? = {
        WheelPickerDefaults.Background(size = it)
    },
) {
    WheelPicker(
        modifier = modifier,
        startIndex = startIndex,
        items = items,
        size = size,
        onSelectionChanged = onSelectionChanged,
        manualInputType = KeyboardType.Number,
        backgroundContent = backgroundContent,
    ) {
        WheelPickerDefaults.Item(text = "$it")
    }
}

@Composable
fun WheelTextPicker(
    items: List<String>,
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    size: DpSize = DpSize(128.dp, 128.dp),
    onSelectionChanged: (index: Int) -> Unit = {},
    backgroundContent: (@Composable (size: DpSize) -> Unit)? = {
        WheelPickerDefaults.Background(size = it)
    },
) {
    WheelPicker(
        modifier = modifier,
        startIndex = startIndex,
        items = items,
        size = size,
        onSelectionChanged = onSelectionChanged,
        backgroundContent = backgroundContent,
    ) {
        WheelPickerDefaults.Item(text = it)
    }
}

@Composable
private fun <T> WheelPicker(
    items: List<T>,
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    size: DpSize = DpSize(128.dp, 128.dp),
    onSelectionChanged: (index: Int) -> Unit = {},
    manualInputType: KeyboardType? = null,
    backgroundContent: (@Composable (size: DpSize) -> Unit)? = null,
    itemContent: @Composable LazyItemScope.(item: T) -> Unit,
) {
    var index by rememberSaveable(items) {
        mutableIntStateOf(startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)))
    }
    LaunchedEffect(Unit) { if (items.isNotEmpty()) onSelectionChanged(index) }
    var open by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    PaperButton(onClick = {
        input = items.getOrNull(index)?.toString().orEmpty()
        open = true
    }, modifier = modifier.width(size.width), enabled = items.isNotEmpty()) {
        Text(items.getOrNull(index)?.toString().orEmpty(), style = MaterialTheme.typography.titleMedium)
    }
    if (open) {
        val select: (Int) -> Unit = {
            index = it
            onSelectionChanged(it)
            open = false
        }
        val entered = items.indexOfFirst { it.toString() == input }
        PaperAlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(MR.strings.daihon_select_value)) },
            text = {
                Column {
                    if (manualInputType != null) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = manualInputType),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    PaperLazyColumn(Modifier.heightIn(max = 280.dp), state = rememberLazyListState(index)) {
                        itemsIndexed(items) { i, item ->
                            PaperButton(onClick = { select(i) }, modifier = Modifier.fillMaxWidth()) {
                                Text((if (i == index) "✓ " else "") + item.toString())
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (manualInputType !=
                    null
                ) {
                    PaperButton(onClick = {
                        select(entered)
                    }, enabled = entered >= 0) { Text(stringResource(MR.strings.action_ok)) }
                }
            },
            dismissButton = {
                PaperButton(onClick = { open = false }) { Text(stringResource(MR.strings.action_cancel)) }
            },
        )
    }
}

object WheelPickerDefaults {
    @Composable fun Background(size: DpSize) = Unit

    @Composable fun Item(text: String) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
