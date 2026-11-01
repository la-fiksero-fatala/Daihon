package tachiyomi.presentation.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Complete rows per step, preserving empty space on an incomplete final page. */
@Composable
fun PaperLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (reverseLayout) Arrangement.Bottom else Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    alwaysShowControls: Boolean = false,
    showControlsDivider: Boolean = true,
    fillRowGaps: Boolean = false,
    preserveLastPageSpace: Boolean = true,
    beforeControls: @Composable (() -> Unit)? = null,
    hasMoreItems: Boolean = false,
    content: LazyListScope.() -> Unit,
) {
    // The library already supplies a fixed subset and its own page controls.
    if (!userScrollEnabled) {
        LazyColumn(
            modifier, state, contentPadding, reverseLayout, verticalArrangement, horizontalAlignment,
            flingBehavior, false, content = content,
        )
        return
    }
    val sections by remember(content) { derivedStateOf { PaperListContent().apply(content) } }
    val owner = remember { Any() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val history = remember { mutableStateListOf<PaperAnchor>() }
    var viewport by remember { mutableIntStateOf(0) }
    var showJump by remember { mutableStateOf(false) }
    val info = state.layoutInfo
    val total = (info.totalItemsCount - 1).coerceAtLeast(0)
    val visible = info.visibleItemsInfo.filter { it.index < total }.sortedBy { it.index }
    val headerSizes = remember(sections) { mutableMapOf<Int, Int>() }
    visible.filter { it.index in sections.headers }.forEach { headerSizes[it.index] = it.size }
    val first = PaperAnchor(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
    val end = info.viewportEndOffset - info.afterContentPadding
    val pageItems = visible.map { PaperItem(it.index, it.offset, it.size) }
    val orphan = paperOrphanHeader(pageItems, sections.headers, first, end)
    val next = orphan?.let { PaperAnchor(it.index) } ?: nextPaperAnchor(
        pageItems.filter { it.index >= first.index },
        first,
        end,
        total,
    )
    val rowHeight = visible.maxOfOrNull { it.size } ?: 0
    val spacing = paperRowSpacing((end - info.beforeContentPadding).coerceAtLeast(0), rowHeight, total)
    val arrangement = if (fillRowGaps) Arrangement.spacedBy(with(density) { spacing.toDp() }) else verticalArrangement
    val hasPrevious = first.index > 0 || first.offset > 0
    val paged = total > 0 && (hasPrevious || next != null)
    val partial = visible.firstOrNull { it.index > first.index && it.offset + it.size > end }
    val mask =
        (orphan ?: partial?.let { PaperItem(it.index, it.offset, it.size) })?.let {
            (
                viewport -
                    info.beforeContentPadding -
                    it.offset
                ).coerceAtLeast(0)
        }
            ?: 0
    suspend fun jumpTo(target: PaperAnchor) {
        val header = sections.headers.lastOrNull { it < target.index }
        var height = header?.let { headerSizes[it] } ?: 0
        if (header != null && height == 0 && target.index !in sections.headers) {
            state.scrollToItem(header)
            height = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == header }?.size ?: 0
        }
        val reserve = if (target.index in sections.headers || target.offset > 0) 0 else height
        state.scrollToItem(target.index, target.offset - reserve)
    }
    val navigate: (Int) -> Unit = { step ->
        PaperNavigation.activate(owner)
        scope.launch {
            if (step > 0 && next != null) {
                history.add(first)
                jumpTo(next)
            } else if (step < 0 && hasPrevious) {
                val target = history.removeLastOrNull()
                if (target != null && target.index < total) {
                    jumpTo(target)
                } else {
                    val old = state.firstVisibleItemIndex
                    state.scrollBy(-end.coerceAtLeast(1).toFloat())
                    if (state.firstVisibleItemIndex < old) jumpTo(PaperAnchor(state.firstVisibleItemIndex))
                }
            }
        }
    }
    LaunchedEffect(total, viewport) {
        history.clear()
        if (total > 0 && state.firstVisibleItemIndex >= total) state.scrollToItem(total - 1)
    }
    val fallback = (LocalConfiguration.current.screenHeightDp * .75f).dp
    BoxWithConstraints(modifier) {
        val limit = if (constraints.hasBoundedHeight) maxHeight else fallback
        Column(Modifier.heightIn(max = limit).fillMaxWidth().padding(top = contentPadding.calculateTopPadding())) {
            Box(
                Modifier.weight(1f, fill = false).clipToBounds().onSizeChanged { viewport = it.height }
                    .then(paperInput(owner, !showJump, navigate, atTop = !hasPrevious)),
            ) {
                LazyColumn(
                    state = state,
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(direction),
                        end = contentPadding.calculateEndPadding(direction),
                    ),
                    reverseLayout = reverseLayout,
                    verticalArrangement = arrangement,
                    horizontalAlignment = horizontalAlignment,
                    userScrollEnabled = false,
                ) {
                    sections.emit(this)
                    item(key = "__daihon_page_end", contentType = "__daihon_page_end") {
                        Spacer(
                            Modifier.height(
                                if (paged &&
                                    preserveLastPageSpace
                                ) {
                                    with(density) { viewport.toDp() }
                                } else {
                                    0.dp
                                },
                            ),
                        )
                    }
                }
                if (mask > 0) {
                    Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .height(with(density) { mask.toDp() }).background(Color.White)
                            .pointerInput(Unit) { detectTapGestures(onTap = {}) },
                    )
                }
            }
            beforeControls?.invoke()
            if (paged || alwaysShowControls) {
                val shownLast = visible.lastOrNull { it.offset + it.size <= end }?.index ?: first.index
                PaperPageControls(
                    if (total == 0) {
                        "0 / 0"
                    } else {
                        "${first.index + 1}–${shownLast + 1} / $total${if (hasMoreItems) "+" else ""}"
                    },
                    hasPrevious,
                    next != null,
                    { navigate(-1) },
                    { navigate(1) },
                    { showJump = true },
                    Modifier.padding(bottom = contentPadding.calculateBottomPadding())
                        .consumeWindowInsets(contentPadding).navigationBarsPadding(),
                    canJump = total > 0,
                    showDivider = showControlsDivider,
                )
            }
        }
    }
    if (showJump) {
        PaperJumpDialog(first.index, total, { index ->
            history.clear()
            scope.launch { jumpTo(PaperAnchor(index)) }
        }, onDismissRequest = { showJump = false }, itemNumber = true)
    }
}
