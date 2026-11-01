package tachiyomi.presentation.core.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
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
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val owner = remember { Any() }
    var viewport by remember { mutableIntStateOf(1) }
    var showJump by remember { mutableStateOf(false) }
    val sections = remember(state) { PaperSections { state.value } }
    val headings = sections.headings.values.toList()
    val pinned = headings.filter { it.top < state.value }.maxByOrNull { it.top }
    val headerSpace = headings.maxOfOrNull { it.height } ?: 0
    val step = paperPageStep((viewport - headerSpace).coerceAtLeast(1))
    val orphan = headings.firstOrNull {
        it.top > state.value && it.top < state.value + viewport && it.top + it.height >= state.value + viewport
    }
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
        val fill = constraints.minHeight > 0
        Column(Modifier.heightIn(min = with(density) { constraints.minHeight.toDp() }, max = limit).fillMaxWidth()) {
            Box(
                Modifier.weight(1f, fill = fill).clipToBounds().onSizeChanged { viewport = it.height.coerceAtLeast(1) }
                    .onGloballyPositioned {
                        sections.viewport = it
                        sections.headings.values.forEach(sections::position)
                    }
                    .then(paperInput(owner, enabled && !showJump, navigate, atTop = page == 0)),
                propagateMinConstraints = fill,
            ) {
                CompositionLocalProvider(LocalPaperSections provides sections) {
                    Column(
                        Modifier.fillMaxWidth().verticalScroll(
                            state,
                            enabled = false,
                            reverseScrolling = reverseScrolling,
                        ),
                        verticalArrangement = verticalArrangement,
                        horizontalAlignment = horizontalAlignment,
                        content = content,
                    )
                }
                pinned?.let { heading ->
                    Box(
                        Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Color.White)
                            .pointerInput(Unit) { detectTapGestures {} },
                    ) {
                        heading.content()
                    }
                }
                orphan?.let { heading ->
                    Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .height(with(density) { (state.value + viewport - heading.top).toDp() })
                            .background(Color.White).pointerInput(Unit) { detectTapGestures {} },
                    )
                }
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
