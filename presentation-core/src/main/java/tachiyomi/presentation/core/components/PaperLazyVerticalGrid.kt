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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    alwaysShowNavigation: Boolean = false,
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
    var viewport by remember { mutableIntStateOf(0) }
    var showJump by remember { mutableStateOf(false) }
    val info = state.layoutInfo
    val total = (info.totalItemsCount - 1).coerceAtLeast(0)
    val visible = info.visibleItemsInfo.filter { it.index < total }
    fun position() = PaperAnchor(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
    suspend fun snap(anchor: PaperAnchor) = state.scrollToItem(anchor.index, anchor.offset)
    val first = position()
    val end = info.viewportEndOffset - info.afterContentPadding
    fun liveNext(): PaperAnchor? {
        val layout = state.layoutInfo
        val count = (layout.totalItemsCount - 1).coerceAtLeast(0)
        return nextPaperAnchor(
            layout.visibleItemsInfo.filter {
                it.index < count
            }.map { PaperItem(it.index, it.offset.y, it.size.height) },
            PaperAnchor(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset),
            layout.viewportEndOffset - layout.afterContentPadding,
            count,
        )
    }
    val next = liveNext()
    val hasPrevious = first.index > 0 || first.offset > 0
    val paged = total > 0 && (hasPrevious || next != null)
    val partial = visible.firstOrNull { it.offset.y > 0 && it.offset.y + it.size.height > end }
    val mask = partial?.let { (viewport - info.beforeContentPadding - it.offset.y).coerceAtLeast(0) } ?: 0
    LaunchedEffect(first, end) {
        val rowHeight = visible.filter { it.offset.y == visible.minOfOrNull { item -> item.offset.y } }
            .maxOfOrNull { it.size.height } ?: 0
        if (first.offset > 0 && rowHeight <= end) state.scrollToItem(first.index)
    }
    fun liveProbeKey(): PaperPageProbeKey {
        val layout = state.layoutInfo
        return PaperPageProbeKey((layout.totalItemsCount - 1).coerceAtLeast(0), layout.viewportSize)
    }
    val probeKey = liveProbeKey()
    val pages = remember(state, probeKey.viewport, if (alwaysShowNavigation) 0 else total) { PaperLazyPages() }
    var probeResult by remember(state) { mutableStateOf<PaperPageProbeResult?>(null) }
    val probeMutex = remember(state) { Mutex() }
    val layoutReady = alwaysShowNavigation || total == 0 || probeResult?.key == probeKey
    LaunchedEffect(probeKey, alwaysShowNavigation) {
        if (alwaysShowNavigation || total == 0 || probeKey.viewport.width <= 0 || probeKey.viewport.height <= 0) {
            return@LaunchedEffect
        }
        probeMutex.withLock {
            // Let the rail's initial width settle before inspecting either page.
            withFrameNanos {}
            if (liveProbeKey() != probeKey) return@withLock
            val original = PaperAnchor(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
            val third = probePaperThirdPage(
                original,
                top = { state.scrollToItem(0) },
                next = ::liveNext,
                move = { state.scrollToItem(it.index, it.offset) },
                restore = {
                    val last = (state.layoutInfo.totalItemsCount - 2).coerceAtLeast(0)
                    state.scrollToItem(it.index.coerceIn(0, last), it.offset)
                },
                settle = { withFrameNanos {} },
            )
            withFrameNanos {}
            if (liveProbeKey() == probeKey) probeResult = PaperPageProbeResult(probeKey, third)
        }
    }
    LaunchedEffect(pages, layoutReady) {
        if (layoutReady) {
            snapshotFlow { position() }.collect {
                probeMutex.withLock { pages.locate(::position, ::liveNext, ::snap, ::snap) }
            }
        }
    }
    val page = pages.indexOf(first)
    fun goToPage(index: Int) {
        scope.launch {
            probeMutex.withLock { pages.goTo(index, ::position, ::liveNext, ::snap, ::snap) }
        }
    }
    val navigate: (Int) -> Unit = { step ->
        PaperNavigation.activate(owner)
        scope.launch {
            probeMutex.withLock {
                pages.navigate(step, ::position, ::liveNext, ::snap, ::snap)
            }
        }
    }
    LaunchedEffect(total, viewport) {
        if (total > 0 && state.firstVisibleItemIndex >= total) state.scrollToItem(total - 1)
    }
    val fallback = (LocalConfiguration.current.screenHeightDp * .75f).dp
    BoxWithConstraints(modifier) {
        val limit = if (constraints.hasBoundedHeight) maxHeight else fallback
        PaperPageFrame(
            owner, (page + 1).toString(),
            paged, hasPrevious, next != null, { navigate(-1) }, { navigate(1) }, { showJump = true },
            Modifier.drawWithContent {
                val current = liveProbeKey()
                if (!pages.seeking && (alwaysShowNavigation || current.count == 0 || probeResult?.key == current)) {
                    drawContent()
                }
            }.heightIn(max = limit).fillMaxWidth().padding(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            ).consumeWindowInsets(contentPadding).navigationBarsPadding(),
            fill = constraints.minHeight > 0, ready = info.totalItemsCount > 0 && layoutReady,
            showJumpToTop = alwaysShowNavigation || probeResult?.hasThirdPage == true,
            alwaysShowNavigation = alwaysShowNavigation,
            onJumpToTop = {
                PaperNavigation.activate(owner)
                goToPage(0)
            },
        ) {
            Box(
                Modifier.weight(1f, fill = false).clipToBounds().onSizeChanged { viewport = it.height }
                    .then(
                        paperInput(owner, !showJump && layoutReady && !pages.seeking, navigate, atTop = !hasPrevious),
                    ),
            ) {
                LazyVerticalGrid(
                    columns = columns,
                    state = state,
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(direction),
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
                if (mask > 0) {
                    Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .height(with(density) { mask.toDp() }).background(Color.White)
                            .pointerInput(Unit) { detectTapGestures(onTap = {}) },
                    )
                }
            }
        }
    }
    if (showJump) {
        PaperJumpDialog(page, null, ::goToPage, onDismissRequest = { showJump = false })
    }
}
