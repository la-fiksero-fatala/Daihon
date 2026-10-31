package tachiyomi.presentation.core.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Static form/text pages with overlap; the finger never moves the content. */
@Composable
fun PaperScrollColumn(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    reverseScrolling: Boolean = false,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val owner = remember { Any() }
    var viewport by remember { mutableIntStateOf(1) }
    var showJump by remember { mutableStateOf(false) }
    val step = paperPageStep(viewport)
    val maximum = state.maxValue.takeUnless { it == Int.MAX_VALUE } ?: 0
    val count = paperPageCount(maximum, step)
    val page = ((state.value + step - 1) / step).coerceIn(0, count - 1)
    val navigate: (Int) -> Unit = { direction ->
        PaperNavigation.activate(owner)
        scope.launch { state.scrollTo(((page + direction).coerceIn(0, count - 1) * step).coerceAtMost(state.maxValue)) }
    }
    val fallback = (LocalConfiguration.current.screenHeightDp * .75f).dp
    BoxWithConstraints(modifier) {
        val limit = if (constraints.hasBoundedHeight) maxHeight else fallback
        Column(Modifier.heightIn(max = limit).fillMaxWidth()) {
            Box(
                Modifier.weight(1f, fill = false).onSizeChanged { viewport = it.height.coerceAtLeast(1) }
                    .then(paperInput(owner, enabled && !showJump, navigate, atTop = page == 0)),
            ) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(state, enabled = false, reverseScrolling = reverseScrolling),
                    verticalArrangement = verticalArrangement,
                    horizontalAlignment = horizontalAlignment,
                    content = content,
                )
            }
            if (count > 1) {
                PaperPageControls(
                    "${page + 1} / $count",
                    page > 0,
                    page + 1 < count,
                    { navigate(-1) },
                    { navigate(1) },
                    { showJump = true },
                    Modifier.navigationBarsPadding(),
                )
            }
        }
    }
    if (showJump) {
        PaperJumpDialog(
            page,
            count,
            { index -> scope.launch { state.scrollTo(index * step) } },
            onDismissRequest = { showJump = false },
        )
    }
}
