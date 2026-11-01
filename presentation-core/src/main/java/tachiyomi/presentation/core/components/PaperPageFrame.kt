package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned

/** Arrow rails take width; position counters always occupy the existing heading. */
@Composable
internal fun PaperPageFrame(
    owner: Any,
    label: String,
    paged: Boolean,
    canPrevious: Boolean,
    canNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onJump: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val settings = LocalPaperNavigationSettings.current
    val area = LocalPaperNavigationArea.current
    val visibleBar = settings.visible(area)
    val host = LocalPaperPageIndicator.current
    val currentJump by rememberUpdatedState(onJump)
    var visible by remember { mutableStateOf(false) }
    val publish = visible && area != PaperNavigationArea.Library
    DisposableEffect(owner, host, publish) {
        if (publish) host?.register(owner, label) { currentJump() }
        onDispose { host?.remove(owner) }
    }
    SideEffect { if (publish) host?.update(owner, label) }
    Column(modifier.onGloballyPositioned { visible = !it.boundsInWindow().isEmpty }) {
        if (host?.hasHeader != true) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                PaperPageNumber(label, onJump)
            }
        }
        Row(Modifier.weight(1f, fill = fill).fillMaxWidth()) {
            Column(Modifier.weight(1f), content = content)
            if (paged && visibleBar) {
                PaperPageControls(
                    canPrevious,
                    canNext,
                    onPrevious,
                    onNext,
                    vertical = true,
                )
            }
        }
    }
}
