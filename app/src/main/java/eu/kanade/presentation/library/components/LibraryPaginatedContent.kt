package eu.kanade.presentation.library.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.ui.library.LibraryNavigation
import eu.kanade.tachiyomi.ui.library.LibraryPageInfo
import kotlinx.coroutines.flow.filterNotNull
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryPageButtons
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LocalPaperNavigationArea
import tachiyomi.presentation.core.components.LocalPaperNavigationSettings
import tachiyomi.presentation.core.components.PaperNavigationArea
import tachiyomi.presentation.core.components.PaperPageControls
import tachiyomi.presentation.core.components.PaperPageNumber
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs
import kotlin.math.ceil
import tachiyomi.presentation.core.components.material.PaperIconButton as IconButton
import tachiyomi.presentation.core.components.material.PaperSurface as Surface

@Composable
internal fun LibraryPaginatedContent(
    items: List<LibraryItem>,
    navigation: LibraryNavigation,
    active: Boolean,
    listState: LazyListState,
    gridState: LazyGridState,
    displayMode: LibraryDisplayMode,
    columns: Int,
    requestedRows: Int,
    onPageRowsChange: (Int) -> Unit,
    stretchPageSpacing: Boolean,
    buttons: LibraryPageButtons,
    showSlider: Boolean,
    contentPadding: PaddingValues,
    searchQuery: String?,
    hasActiveFilters: Boolean,
    onGlobalSearchClicked: () -> Unit,
    content: @Composable (List<LibraryItem>, Dp, Int) -> Unit,
) {
    val isList = displayMode == LibraryDisplayMode.List
    var anchorIndex by rememberSaveable(searchQuery, hasActiveFilters) { mutableIntStateOf(0) }
    val density = LocalDensity.current
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var listLayout by remember(viewport, displayMode, columns, requestedRows, buttons, density) {
        mutableStateOf<LibraryPageLayout?>(null)
    }
    val gridPadding = with(density) { 16.dp.roundToPx() }
    val columnSpacing = with(density) { CommonMangaItemDefaults.GridHorizontalSpacer.roundToPx() }
    val adaptiveColumnWidth = with(density) { 128.dp.roundToPx() }
    val itemPadding = with(density) { (CommonMangaItemDefaults.GridItemPadding * 2).roundToPx() }
    val chromeHeight = with(density) {
        itemPadding + if (displayMode == LibraryDisplayMode.ComfortableGrid) {
            (CommonMangaItemDefaults.ComfortableGridTitlePadding * 2).roundToPx() +
                ceil(CommonMangaItemDefaults.GridTitleLineHeight.toPx() * 2).toInt()
        } else {
            0
        }
    }
    val minimumCoverHeight = with(density) {
        if (displayMode == LibraryDisplayMode.CompactGrid) {
            maxOf(
                48.dp.roundToPx(),
                ceil(CommonMangaItemDefaults.GridTitleLineHeight.toPx() * 2).toInt() +
                    16.dp.roundToPx(),
            )
        } else {
            48.dp.roundToPx()
        }
    }

    val columnCount = if (columns > 0) {
        columns
    } else {
        ((viewport.width - gridPadding + columnSpacing) / (adaptiveColumnWidth + columnSpacing)).coerceAtLeast(1)
    }
    // Grid geometry is known from the viewport. Apply the fitted width and capacity together,
    // before exposing any covers, rather than waiting for an unconstrained grid to draw first.
    val gridLayout = if (!isList && viewport.width > gridPadding && viewport.height > gridPadding) {
        libraryGridPageLayout(
            height = viewport.height - gridPadding,
            itemWidth = libraryGridCellWidth(viewport.width - gridPadding, columnCount, columnSpacing),
            columns = columnCount,
            padding = itemPadding,
            chromeHeight = chromeHeight,
            spacing = with(density) { CommonMangaItemDefaults.GridVerticalSpacer.roundToPx() },
            minimumCoverHeight = minimumCoverHeight,
            requestedRows = requestedRows,
        )
    } else {
        null
    }
    val layout = if (isList) listLayout else gridLayout
    val pageSize = layout?.capacity ?: 0
    val rowsPerPage = layout?.rows ?: 0
    val maximumRows = layout?.maxRows ?: 1
    val canStretchSpacing = layout?.canStretchSpacing == true
    val maxItemWidth = layout?.itemWidth?.let { with(density) { it.toDp() } } ?: Dp.Infinity

    LaunchedEffect(viewport, displayMode, columns, requestedRows, buttons, density) {
        if (!isList) {
            gridState.scrollToItem(0)
            return@LaunchedEffect
        }
        listState.scrollToItem(0)
        snapshotFlow {
            val info = listState.layoutInfo
            val row = info.visibleItemsInfo.firstOrNull { it.contentType == "library_list_item" }
            row?.takeIf { info.viewportSize == viewport }?.let {
                val height = info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding
                val maximum = libraryPageCapacity(height, it.size, 0, 1, 0)
                val rows = if (requestedRows <= 0) maximum else requestedRows.coerceAtMost(maximum)
                LibraryPageLayout(rows, maximum, null, rows > 1 && height - rows * it.size > 1, rows)
            }
        }.filterNotNull().collect { listLayout = it }
    }

    val size = pageSize.coerceAtLeast(1)
    fun currentPageStart(): Int {
        return libraryPageStart(anchorIndex, size, items.size)
    }
    val start = currentPageStart()
    val page = start / size
    val pageCount = (items.size - 1) / size + 1
    val onPageChange: (Int) -> Unit = { direction ->
        val next = libraryPageStart(currentPageStart() + direction * size, size, items.size)
        anchorIndex = next
    }
    val currentOnPageChange by rememberUpdatedState(onPageChange)
    val handler: (Int) -> Unit = remember { { currentOnPageChange(it) } }
    SideEffect {
        if (active && pageSize > 0) {
            navigation.pageInfo = LibraryPageInfo(page, pageCount, maximumRows)
            val compatibleRows = libraryCompatiblePageRows(requestedRows, maximumRows)
            if (compatibleRows != requestedRows) onPageRowsChange(compatibleRows)
        }
    }
    DisposableEffect(navigation, active) {
        if (active) navigation.onPageChange = handler
        onDispose {
            if (navigation.onPageChange === handler) {
                navigation.onPageChange = null
                navigation.pageInfo = null
            }
        }
    }

    val swipeThreshold = with(density) { 48.dp.toPx() }
    Column(Modifier.fillMaxSize().padding(contentPadding).consumeWindowInsets(contentPadding).navigationBarsPadding()) {
        if (!searchQuery.isNullOrEmpty()) {
            GlobalSearchItem(searchQuery = searchQuery, onClick = onGlobalSearchClicked)
        }
        Row(Modifier.weight(1f)) {
            if (buttons == LibraryPageButtons.Left) {
                LibraryPageControls(page, pageCount, true, showSlider, onPageChange)
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (buttons == LibraryPageButtons.Top) {
                    LibraryPageControls(page, pageCount, false, showSlider, onPageChange)
                }
                Box(
                    Modifier.weight(1f)
                        .onSizeChanged { viewport = it }
                        .alpha(if (pageSize == 0) 0f else 1f)
                        .pointerInput(swipeThreshold) {
                            var distance = 0f
                            detectVerticalDragGestures(
                                onDragStart = { distance = 0f },
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    distance += amount
                                },
                                onDragEnd = {
                                    if (abs(distance) >= swipeThreshold) {
                                        currentOnPageChange(if (distance < 0) 1 else -1)
                                    }
                                },
                            )
                        },
                ) {
                    content(
                        items.subList(start, (start + size).coerceAtMost(items.size)),
                        maxItemWidth,
                        if (stretchPageSpacing && canStretchSpacing) rowsPerPage else 0,
                    )
                }
                if (buttons == LibraryPageButtons.Bottom) {
                    LibraryPageControls(page, pageCount, false, showSlider, onPageChange)
                }
            }
            if (buttons == LibraryPageButtons.Right) {
                LibraryPageControls(page, pageCount, true, showSlider, onPageChange)
            }
        }
    }
}

@Composable
internal fun LibraryPageControls(
    page: Int,
    pageCount: Int,
    vertical: Boolean,
    showSlider: Boolean,
    onPageChange: (Int) -> Unit,
) {
    val height = LocalPaperNavigationSettings.current.height
    if (pageCount <= 1) {
        // Keep fitting geometry stable when hiding a single-page rail.
        Spacer(if (vertical) Modifier.width(height).fillMaxHeight() else Modifier.fillMaxWidth().height(height))
        return
    }
    if (showSlider) {
        var target by remember(page, pageCount) { mutableIntStateOf(page) }
        val size = if (vertical) {
            Modifier.width(
                height,
            ).fillMaxHeight()
        } else {
            Modifier.fillMaxWidth().heightIn(min = height)
        }
        LibraryPageSlider(
            page = target,
            pageCount = pageCount,
            onValueChange = { target = it },
            onValueChangeFinished = { onPageChange(target - page) },
            modifier = size.border(1.dp, Color.Black).padding(
                horizontal = if (vertical) 8.dp else 32.dp,
                vertical = if (vertical) 32.dp else ((height.value - 48f) / 2f).coerceIn(0f, 8f).dp,
            ),
            vertical = vertical,
            showNumbers = false,
        )
        return
    }
    CompositionLocalProvider(LocalPaperNavigationArea provides PaperNavigationArea.Library) {
        PaperPageControls(
            canPrevious = page > 0,
            canNext = page + 1 < pageCount,
            onPrevious = { onPageChange(-1) },
            onNext = { onPageChange(1) },
            vertical = vertical,
            showJumpToTop = pageCount > 2,
            onJumpToTop = { onPageChange(-page) },
        )
    }
}

@Composable
internal fun LibraryPageNumber(
    page: Int,
    pageCount: Int,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val description = stringResource(MR.strings.library_page_number, page + 1, pageCount)
    val numberModifier = if (onClick != null) modifier.clickable(role = Role.Button, onClick = onClick) else modifier
    if (compact) {
        PaperPageNumber(
            "${page + 1} / $pageCount",
            onClick ?: {},
            modifier.semantics { contentDescription = description },
        )
    } else {
        Text(
            text = "${page + 1} / $pageCount",
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            modifier = numberModifier.semantics { contentDescription = description },
        )
    }
}

internal fun libraryPageCapacity(height: Int, rowHeight: Int, spacing: Int, columns: Int, requestedRows: Int): Int {
    val fittingRows = ((height + spacing) / (rowHeight + spacing).coerceAtLeast(1)).coerceAtLeast(1)
    val rows = if (requestedRows > 0) requestedRows.coerceAtMost(fittingRows) else fittingRows
    return rows * columns.coerceAtLeast(1)
}

internal fun libraryPageStart(anchorIndex: Int, pageSize: Int, itemCount: Int): Int {
    val size = pageSize.coerceAtLeast(1)
    return anchorIndex.coerceIn(0, (itemCount - 1).coerceAtLeast(0)) / size * size
}

// Only shrink oversized grid entries; retain their cover aspect ratio and measured title height.
internal fun libraryPageItemWidth(height: Int, rowHeight: Int, itemWidth: Int, padding: Int): Int {
    val coverHeight = (itemWidth - padding) * 3 / 2
    val titleHeight = rowHeight - coverHeight
    return ((height - titleHeight) * 2 / 3 + padding).coerceIn(padding + 1, itemWidth.coerceAtLeast(padding + 1))
}

internal data class LibraryPageLayout(
    val capacity: Int,
    val maxRows: Int,
    val itemWidth: Int?,
    val canStretchSpacing: Boolean,
    val rows: Int,
)

/** Fixed/adaptive grids give their first cells the remainder pixel. */
internal fun libraryGridCellWidth(available: Int, columns: Int, spacing: Int): Int {
    val count = columns.coerceAtLeast(1)
    return ((available - spacing * (count - 1) + count - 1) / count).coerceAtLeast(1)
}

internal fun libraryGridPageLayout(
    height: Int,
    itemWidth: Int,
    columns: Int,
    padding: Int,
    chromeHeight: Int,
    spacing: Int,
    minimumCoverHeight: Int,
    requestedRows: Int,
): LibraryPageLayout {
    val coverHeight = (itemWidth - padding).coerceAtLeast(1) * 3 / 2
    val naturalHeight = coverHeight + chromeHeight
    val maximum = libraryPageCapacity(height, minOf(coverHeight, minimumCoverHeight) + chromeHeight, spacing, 1, 0)
    val rows = when {
        requestedRows == 0 -> libraryPageCapacity(height, naturalHeight, spacing, 1, 0)
        requestedRows < 0 -> maximum
        else -> requestedRows.coerceAtMost(maximum)
    }
    val rowHeight = ((height - spacing * (rows - 1)) / rows).coerceAtLeast(1)
    val width = if (naturalHeight >= rowHeight) {
        (libraryPageItemWidth(rowHeight, naturalHeight, itemWidth, padding) - 1).coerceAtLeast(padding + 1)
    } else {
        null
    }
    val canStretch = rows > 1 && height - rows * naturalHeight - spacing * (rows - 1) > 1
    return LibraryPageLayout(rows * columns.coerceAtLeast(1), maximum, width, canStretch, rows)
}

internal fun libraryCompatiblePageRows(requestedRows: Int, maximum: Int): Int =
    if (requestedRows < 0) maximum else requestedRows.coerceAtMost(maximum)
