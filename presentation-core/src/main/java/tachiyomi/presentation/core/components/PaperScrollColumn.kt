package tachiyomi.presentation.core.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
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

/** Complete control/text-line pages; no overlap, scrolling animation, or final-page backfill. */
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
    var viewport by remember { mutableIntStateOf(0) }
    var blocks by remember { mutableStateOf(emptyList<PaperSpan>()) }
    var measured by remember { mutableStateOf(false) }
    var showJump by remember { mutableStateOf(false) }
    val sections = remember(state) { PaperSections { state.value } }
    val breaks = remember(state) { PaperContentBreaks { state.value } }
    val headings = sections.headings.values.filter { it.top != Int.MAX_VALUE }
    val pages = paperFormPages(
        blocks,
        breaks.pieces.values.flatten(),
        headings.map { PaperSpan(it.top, it.top + it.height) },
        viewport,
    )
    val page = pages.indexOfLast { it.start <= state.value }.coerceAtLeast(0)
    val slice = pages[page]
    val pinned = if (slice.headerHeight > 0) headings.filter { it.top <= slice.start }.maxByOrNull { it.top } else null
    val contentEnd = blocks.maxOfOrNull { it.bottom } ?: 0
    val navigate: (Int) -> Unit = { direction ->
        PaperNavigation.activate(owner)
        scope.launch { state.scrollTo(pages[(page + direction).coerceIn(pages.indices)].start) }
    }
    LaunchedEffect(pages, state.value) {
        if (state.value != slice.start) state.scrollTo(slice.start)
    }
    val fallback = (LocalConfiguration.current.screenHeightDp * .75f).dp
    BoxWithConstraints(modifier) {
        val limit = if (constraints.hasBoundedHeight) maxHeight else fallback
        val fill = constraints.minHeight > 0
        PaperPageFrame(
            owner, "${page + 1} / ${pages.size}", pages.size > 1,
            page > 0, page < pages.lastIndex, { navigate(-1) }, { navigate(1) }, { showJump = true },
            Modifier.heightIn(min = with(density) { constraints.minHeight.toDp() }, max = limit)
                .fillMaxWidth().navigationBarsPadding(),
            fill = fill, ready = measured && viewport > 0,
            showJumpToTop = pages.size > 2,
            onJumpToTop = {
                PaperNavigation.activate(owner)
                scope.launch { state.scrollTo(0) }
            },
        ) {
            Box(
                Modifier.weight(1f, fill = fill).clipToBounds().onSizeChanged { viewport = it.height.coerceAtLeast(1) }
                    .then(paperInput(owner, enabled && !showJump, navigate, atTop = page == 0)),
                propagateMinConstraints = fill,
            ) {
                Column {
                    pinned?.let { heading ->
                        Box(Modifier.fillMaxWidth().background(Color.White)) { heading.content() }
                    }
                    Box(
                        Modifier.weight(1f, fill = fill).clipToBounds().onGloballyPositioned {
                            sections.viewport = it
                            sections.headings.values.forEach(sections::position)
                            breaks.viewport = it
                            breaks.refresh()
                        },
                    ) {
                        CompositionLocalProvider(
                            LocalPaperSections provides sections,
                            LocalPaperContentBreaks provides breaks,
                        ) {
                            PaperFormColumn(
                                Modifier.fillMaxWidth().verticalScroll(
                                    state,
                                    enabled = false,
                                    reverseScrolling = reverseScrolling,
                                ),
                                tail = if (pages.size > 1 || contentEnd > viewport) viewport else 0,
                                arrangement = verticalArrangement,
                                alignment = horizontalAlignment,
                                onMeasured = {
                                    measured = true
                                    if (blocks != it) blocks = it
                                },
                                content = content,
                            )
                        }
                        val used = (slice.end - slice.start).coerceAtLeast(0)
                        val blank = (viewport - slice.headerHeight - used).coerceAtLeast(0)
                        if (blank > 0 && pages.size > 1) {
                            Box(
                                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                    .height(with(density) { blank.toDp() }).background(Color.White)
                                    .pointerInput(Unit) { detectTapGestures {} },
                            )
                        }
                    }
                }
            }
        }
    }
    if (showJump) {
        PaperJumpDialog(
            page,
            pages.size,
            { index -> scope.launch { state.scrollTo(pages[index].start) } },
            onDismissRequest = { showJump = false },
        )
    }
}
