package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun PaperSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    androidx.compose.material3.SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        PaperSurface(Modifier.padding(12.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(data.visuals.message)
                FlowRow {
                    data.visuals.actionLabel?.let { label ->
                        PaperButton(onClick = data::performAction) { Text(label) }
                    }
                    if (data.visuals.withDismissAction) {
                        PaperButton(onClick = data::dismiss) { Text(stringResource(MR.strings.action_close)) }
                    }
                }
            }
        }
    }
}
