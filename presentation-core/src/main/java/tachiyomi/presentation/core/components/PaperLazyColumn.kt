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
import androidx.compose.runtime.rememberUpdatedState
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
    fillRowGaps: Boolean = false,
    hasMoreItems: Boolean = false,
    alwaysShowNavigation: Boolean = false,
    pageLabel: ((first: Int, last: Int) -> String?)? = null,
    onJump: ((first: Int) -> Unit)? = null,
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
    val chapterNumbers = LocalPaperNavigationArea.current == PaperNavigationArea.Chapters || pageLabel != null
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
    val itemTotal = sections.itemCount
    val visible = info.visibleItemsInfo.filter { it.index < total }.sortedBy { it.index }
    val headerSizes = remember(sections) { mutableMapOf<Int, Int>() }
    visible.filter { it.index in sections.headers }.forEach { headerSizes[it.index] = it.size }
    val pinnedHeight = visible.lastOrNull { it.index in sections.headers && it.offset <= 0 }?.size ?: 0
    val leading = visible.firstOrNull { it.index !in sections.headers && it.offset + it.size > pinnedHeight }
    val first = leading?.let { PaperAnchor(it.index, (pinnedHeight - it.offset).coerceAtLeast(0)) }
        ?: PaperAnchor(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
    fun position() = PaperAnchor(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
    suspend fun snap(anchor: PaperAnchor) = state.scrollToItem(anchor.index, anchor.offset)
    val firstItem = sections.itemIndex(first.index)
    val end = info.viewportEndOffset - info.afterContentPadding
    val pageItems = visible.map { PaperItem(it.index, it.offset, it.size) }
    val orphan = paperOrphanHeader(pageItems, sections.headers, first, end)
    fun liveNext(): PaperAnchor? {
        val layout = state.layoutInfo
        val count = (layout.totalItemsCount - 1).coerceAtLeast(0)
        return nextPaperListAnchor(
            layout.visibleItemsInfo.filter { it.index < count }.map { PaperItem(it.index, it.offset, it.size) },
            sections.headers,
            PaperAnchor(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset),
            layout.viewportEndOffset - layout.afterContentPadding,
            count,
        )
    }
    val next = liveNext()
    val rowHeight = visible.maxOfOrNull { it.size } ?: 0
    val spacing = paperRowSpacing((end - info.beforeContentPadding).coerceAtLeast(0), rowHeight, itemTotal)
    val arrangement = if (fillRowGaps) Arrangement.spacedBy(with(density) { spacing.toDp() }) else verticalArrangement
    val hasPrevious = state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0
    val paged = itemTotal > 0 && (hasPrevious || next != null)
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
        val reserve = if (target.index in sections.headers) 0 else height
        state.scrollToItem(target.index, target.offset - reserve)
    }
    // External item jumps and viewport changes must not leave a normal row under its sticky header.
    LaunchedEffect(first, pinnedHeight, end) {
        if (first.offset > 0 && leading != null && leading.size <= end - pinnedHeight) {
            jumpTo(PaperAnchor(first.index))
        }
    }
    fun liveProbeKey(): PaperPageProbeKey {
        val layout = state.layoutInfo
        return PaperPageProbeKey((layout.totalItemsCount - 1).coerceAtLeast(0), layout.viewportSize)
    }
    val probeKey = liveProbeKey()
    val pages = remember(state, probeKey.viewport, if (hasMoreItems) 0 else total) { PaperLazyPages() }
    var probeResult by remember(state) { mutableStateOf<PaperPageProbeResult?>(null) }
    val probeMutex = remember(state) { Mutex() }
    val skipProbe = alwaysShowNavigation && hasMoreItems
    val layoutReady = skipProbe || total == 0 || probeResult?.key == probeKey
    LaunchedEffect(probeKey, pages, skipProbe) {
        if (skipProbe || total == 0 || probeKey.viewport.width <= 0 || probeKey.viewport.height <= 0) {
            return@LaunchedEffect
        }
        probeMutex.withLock {
            // Let the rail's initial width settle before inspecting either page.
            withFrameNanos {}
            if (liveProbeKey() != probeKey) return@withLock
            val third = if (chapterNumbers) {
                val original = PaperAnchor(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
                probePaperThirdPage(
                    original,
                    top = { state.scrollToItem(0) },
                    next = ::liveNext,
                    move = { jumpTo(it) },
                    restore = {
                        val last = (state.layoutInfo.totalItemsCount - 2).coerceAtLeast(0)
                        state.scrollToItem(it.index.coerceIn(0, last), it.offset)
                    },
                    settle = { withFrameNanos {} },
                )
            } else {
                pages.measure(::position, ::liveNext, ::jumpTo, ::snap)
                pages.hasThirdPage
            }
            withFrameNanos {}
            if (liveProbeKey() == probeKey) probeResult = PaperPageProbeResult(probeKey, third)
        }
    }
    val locatePage by rememberUpdatedState<suspend () -> Unit> {
        pages.locate(::position, ::liveNext, ::jumpTo, ::snap)
    }
    LaunchedEffect(pages, layoutReady, chapterNumbers) {
        if (layoutReady && !chapterNumbers) {
            snapshotFlow { position() }.collect {
                probeMutex.withLock { locatePage() }
            }
        }
    }
    val page = pages.indexOf(position())
    val pageCount = pages.count.takeUnless { hasMoreItems }
    fun goToPage(index: Int) {
        scope.launch {
            probeMutex.withLock { pages.goTo(index, ::position, ::liveNext, ::jumpTo, ::snap) }
        }
    }
    val navigate: (Int) -> Unit = { step ->
        PaperNavigation.activate(owner)
        scope.launch {
            if (!chapterNumbers) {
                probeMutex.withLock {
                    pages.navigate(step, ::position, ::liveNext, ::jumpTo, ::snap)
                }
            } else if (step > 0 && next != null) {
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
        val shownLast = visible.lastOrNull { it.index !in sections.headers && it.offset + it.size <= end }?.index
            ?: first.index
        val lastItem = sections.itemIndex(shownLast)
        val label = if (chapterNumbers) {
            pageLabel?.invoke(first.index, shownLast) ?: "${firstItem + 1}–${lastItem + 1} / $itemTotal"
        } else {
            "${page + 1}/${pageCount ?: ".."}"
        }
        PaperPageFrame(
            owner, label, paged, hasPrevious, next != null,
            { navigate(-1) }, { navigate(1) },
            {
                if (onJump != null) {
                    history.clear()
                    onJump(first.index)
                } else {
                    showJump = true
                }
            },
            Modifier.drawWithContent {
                val current = liveProbeKey()
                if (!pages.seeking && (skipProbe || current.count == 0 || probeResult?.key == current)) {
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
                if (chapterNumbers) {
                    history.clear()
                    scope.launch { state.scrollToItem(0) }
                } else {
                    goToPage(0)
                }
            },
        ) {
            Box(
                Modifier.weight(1f, fill = false).clipToBounds().onSizeChanged { viewport = it.height }
                    .then(
                        paperInput(owner, !showJump && layoutReady && !pages.seeking, navigate, atTop = !hasPrevious),
                    ),
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
                                if (paged) with(density) { viewport.toDp() } else 0.dp,
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
        }
    }
    if (showJump) {
        if (chapterNumbers) {
            PaperJumpDialog(firstItem, itemTotal, { index ->
                history.clear()
                scope.launch { jumpTo(PaperAnchor(sections.listIndex(index))) }
            }, onDismissRequest = { showJump = false }, itemNumber = true)
        } else {
            PaperJumpDialog(page, pageCount, ::goToPage, onDismissRequest = { showJump = false })
        }
    }
}
