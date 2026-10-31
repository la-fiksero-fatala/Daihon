package eu.kanade.presentation.download

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.download.DownloadQueueSection
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.DragHandle
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.icons.materialsymbols.rounded.MoreVert
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import sh.calvin.reorderable.rememberScroller
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.PaperAnchor
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.components.PaperLazyColumn as LazyColumn
import tachiyomi.presentation.core.components.StaticLinearProgressIndicator as LinearProgressIndicator
import tachiyomi.presentation.core.components.material.PaperIconButton as IconButton
import tachiyomi.presentation.core.components.material.PaperSurface as Surface

@Composable
fun DownloadQueueList(
    sections: List<DownloadQueueSection>,
    lazyListState: LazyListState,
    contentPadding: PaddingValues,
    onReorder: (List<Download>) -> Unit,
    onMove: (Download, toTop: Boolean) -> Unit,
    onMoveSeries: (Download, toTop: Boolean) -> Unit,
    onCancel: (Download) -> Unit,
    onCancelSeries: (Download) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingAnchor by remember { mutableStateOf<PaperAnchor?>(null) }
    LaunchedEffect(sections) {
        pendingAnchor?.let { anchor ->
            lazyListState.scrollToItem(anchor.index, anchor.offset)
            pendingAnchor = null
        }
    }
    fun keepPage() {
        pendingAnchor = PaperAnchor(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset)
    }
    fun moveSection(index: Int, step: Int) {
        val updated = sections.toMutableList()
        updated.add(index + step, updated.removeAt(index))
        keepPage()
        onReorder(updated.flatMap { it.downloads })
    }
    fun moveDownload(sectionIndex: Int, index: Int, step: Int) {
        val updated = sections[sectionIndex].downloads.toMutableList()
        updated.add(index + step, updated.removeAt(index))
        keepPage()
        onReorder(sections.flatMapIndexed { i, section -> if (i == sectionIndex) updated else section.downloads })
    }
    val all = sections.flatMap { it.downloads }
    LazyColumn(modifier = modifier.fillMaxSize(), state = lazyListState, contentPadding = contentPadding) {
        sections.forEachIndexed { sectionIndex, section ->
            item(key = "section-${section.id}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ListGroupHeader("${section.name} (${section.downloads.size})", Modifier.weight(1f))
                    IconButton(onClick = {
                        moveSection(sectionIndex, -1)
                    }, enabled = sectionIndex > 0) {
                        Icon(MaterialSymbols.Rounded.KeyboardArrowUp, stringResource(MR.strings.daihon_move_up))
                    }
                    IconButton(onClick = {
                        moveSection(sectionIndex, 1)
                    }, enabled = sectionIndex < sections.lastIndex) {
                        Icon(MaterialSymbols.Rounded.KeyboardArrowDown, stringResource(MR.strings.daihon_move_down))
                    }
                }
            }
            itemsIndexed(section.downloads, key = { _, download ->
                "download-${download.chapter.id}"
            }) { index, download ->
                DownloadQueueItem(
                    download = download,
                    canMoveToTop = download != all.firstOrNull(), canMoveToBottom = download != all.lastOrNull(),
                    onStep = { moveDownload(sectionIndex, index, it) }, canStepUp = index > 0,
                    canStepDown =
                    index < section.downloads.lastIndex,
                    onMove = { onMove(download, it) }, onMoveSeries = { onMoveSeries(download, it) },
                    onCancel = { onCancel(download) }, onCancelSeries = { onCancelSeries(download) },
                )
            }
        }
    }
}

@Composable
private fun DownloadQueueItem(
    download: Download,
    canMoveToTop: Boolean,
    canMoveToBottom: Boolean,
    onStep: (Int) -> Unit,
    canStepUp: Boolean,
    canStepDown: Boolean,
    onMove: (toTop: Boolean) -> Unit,
    onMoveSeries: (toTop: Boolean) -> Unit,
    onCancel: () -> Unit,
    onCancelSeries: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by remember(download) { download.pagesProgressFlow() }.collectAsState(null)

    Surface(
        modifier = modifier,

    ) {
        Row(
            modifier = Modifier.padding(
                start = MaterialTheme.padding.small,
                end = MaterialTheme.padding.extraSmall,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                IconButton(onClick = {
                    onStep(-1)
                }, enabled = canStepUp) {
                    Icon(MaterialSymbols.Rounded.KeyboardArrowUp, stringResource(MR.strings.daihon_move_up))
                }
                IconButton(onClick = {
                    onStep(1)
                }, enabled = canStepDown) {
                    Icon(MaterialSymbols.Rounded.KeyboardArrowDown, stringResource(MR.strings.daihon_move_down))
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = MaterialTheme.padding.small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                    Text(
                        text = download.manga.title,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    progress?.let {
                        Text(
                            text = "${it.downloadedPages}/${it.pageCount}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(
                    text = download.chapter.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
                LinearProgressIndicator(
                    progress = { progress?.fraction ?: 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.padding.extraSmall),
                )
            }
            DownloadQueueItemMenu(
                canMoveToTop = canMoveToTop,
                canMoveToBottom = canMoveToBottom,
                onMove = onMove,
                onMoveSeries = onMoveSeries,
                onCancel = onCancel,
                onCancelSeries = onCancelSeries,
            )
        }
    }
}

@Composable
private fun DownloadQueueItemMenu(
    canMoveToTop: Boolean,
    canMoveToBottom: Boolean,
    onMove: (toTop: Boolean) -> Unit,
    onMoveSeries: (toTop: Boolean) -> Unit,
    onCancel: () -> Unit,
    onCancelSeries: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val closeMenu = { expanded = false }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = MaterialSymbols.Rounded.MoreVert,
                contentDescription = stringResource(MR.strings.action_menu),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = closeMenu) {
            if (canMoveToTop) {
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.action_move_to_top)) },
                    onClick = {
                        closeMenu()
                        onMove(true)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_move_to_top_all_for_series)) },
                onClick = {
                    closeMenu()
                    onMoveSeries(true)
                },
            )
            if (canMoveToBottom) {
                DropdownMenuItem(
                    text = { Text(stringResource(MR.strings.action_move_to_bottom)) },
                    onClick = {
                        closeMenu()
                        onMove(false)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_move_to_bottom_all_for_series)) },
                onClick = {
                    closeMenu()
                    onMoveSeries(false)
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_cancel)) },
                onClick = {
                    closeMenu()
                    onCancel()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.cancel_all_for_series)) },
                onClick = {
                    closeMenu()
                    onCancelSeries()
                },
            )
        }
    }
}

private class PagesProgress(
    val pageCount: Int,
    val downloadedPages: Int,
    totalProgress: Int,
) {
    val fraction = if (pageCount == 0) 0f else totalProgress / (pageCount * 100f)
}

/**
 * Progress of the pages of this download, or null until its pages are known.
 */
private fun Download.pagesProgressFlow() = pagesFlow.flatMapLatest { pages ->
    when {
        pages == null -> flowOf(null)
        pages.isEmpty() -> flowOf(PagesProgress(pageCount = 0, downloadedPages = 0, totalProgress = 0))
        else ->
            combine(pages.map { page -> combine(page.statusFlow, page.progressFlow, ::Pair) }) { states ->
                PagesProgress(
                    pageCount = pages.size,
                    downloadedPages = states.count { (status, _) -> status == Page.State.Ready },
                    totalProgress = states.sumOf { (_, progress) -> progress },
                )
            }
    }
}
