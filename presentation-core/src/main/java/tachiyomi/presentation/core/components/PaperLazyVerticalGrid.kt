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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

@Composable
fun PaperLazyVerticalGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (reverseLayout) Arrangement.Bottom else Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    hasMoreItems: Boolean = false,
    content: LazyGridScope.() -> Unit,
) {
    if (!userScrollEnabled) {
        LazyVerticalGrid(
            columns, modifier, state, contentPadding, reverseLayout, verticalArrangement,
            horizontalArrangement, flingBehavior, false, content = content,
        )
        return
    }
    val owner = remember { Any() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val history = remember { mutableStateListOf<PaperAnchor>() }
    var viewport by remember { mutableIntStateOf(0) }
    var showJump by remember { mutableStateOf(false) }
    val info = state.layoutInfo
    val total = (info.totalItemsCount - 1).coerceAtLeast(0)
    val visible = info.visibleItemsInfo.filter { it.index < total }
    val first = PaperAnchor(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
    val end = info.viewportEndOffset - info.afterContentPadding
    val next = nextPaperAnchor(visible.map { PaperItem(it.index, it.offset.y, it.size.height) }, first, end, total)
    val hasPrevious = first.index > 0 || first.offset > 0
    val paged = total > 0 && (hasPrevious || next != null)
    val partial = visible.firstOrNull { it.offset.y > 0 && it.offset.y + it.size.height > end }
    val mask = partial?.let { (viewport - info.beforeContentPadding - it.offset.y).coerceAtLeast(0) } ?: 0
    val navigate: (Int) -> Unit = { step ->
        PaperNavigation.activate(owner)
        scope.launch {
            if (step > 0 && next != null) {
                history.add(first)
                state.scrollToItem(next.index, next.offset)
            } else if (step < 0 && hasPrevious) {
                val target = history.removeLastOrNull()
                if (target != null && target.index < total) {
                    state.scrollToItem(target.index, target.offset)
                } else {
                    val old = state.firstVisibleItemIndex
                    state.scrollBy(-end.coerceAtLeast(1).toFloat())
                    if (state.firstVisibleItemIndex < old) state.scrollToItem(state.firstVisibleItemIndex)
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
        Column(Modifier.heightIn(max = limit).fillMaxWidth()) {
            Box(
                Modifier.weight(1f, fill = false).clipToBounds().onSizeChanged { viewport = it.height }
                    .then(paperInput(owner, !showJump, navigate, atTop = !hasPrevious)),
            ) {
                LazyVerticalGrid(
                    columns = columns,
                    state = state,
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(direction),
                        top = contentPadding.calculateTopPadding(),
                        end = contentPadding.calculateEndPadding(direction),
                    ),
                    reverseLayout = reverseLayout,
                    verticalArrangement = verticalArrangement,
                    horizontalArrangement = horizontalArrangement,
                    userScrollEnabled = false,
                ) {
                    content()
                    item(key = "__daihon_grid_end", span = { GridItemSpan(maxLineSpan) }) {
                        Spacer(Modifier.height(if (paged) with(density) { viewport.toDp() } else 0.dp))
                    }
                }
                // Native lazy layouts draw cached preceding rows into their leading padding.
                // Keep that margin blank when a new page starts.
                if (hasPrevious && contentPadding.calculateTopPadding() > 0.dp) {
                    Box(
                        Modifier.align(Alignment.TopCenter).fillMaxWidth()
                            .height(contentPadding.calculateTopPadding()).background(Color.White),
                    )
                }
                if (mask > 0) {
                    Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .height(with(density) { mask.toDp() }).background(Color.White)
                            .pointerInput(Unit) { detectTapGestures(onTap = {}) },
                    )
                }
            }
            if (paged) {
                val shownLast = visible.lastOrNull { it.offset.y + it.size.height <= end }?.index ?: first.index
                PaperPageControls(
                    "${first.index + 1}–${shownLast + 1} / $total${if (hasMoreItems) "+" else ""}",
                    hasPrevious,
                    next != null,
                    { navigate(-1) },
                    { navigate(1) },
                    { showJump = true },
                    Modifier.padding(bottom = contentPadding.calculateBottomPadding())
                        .consumeWindowInsets(contentPadding).navigationBarsPadding(),
                )
            }
        }
    }
    if (showJump) {
        PaperJumpDialog(first.index, total, { index ->
            history.clear()
            scope.launch { state.scrollToItem(index) }
        }, onDismissRequest = { showJump = false }, itemNumber = true)
    }
}
