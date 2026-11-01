package eu.kanade.presentation.manga

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.manga.components.ChapterHeader
import eu.kanade.presentation.manga.components.ChapterNavigationButtons
import eu.kanade.presentation.manga.components.ChapterPageJumpDialog
import eu.kanade.presentation.manga.components.ChapterPageNumbers
import eu.kanade.presentation.manga.components.ExpandableMangaDescription
import eu.kanade.presentation.manga.components.MangaActionRow
import eu.kanade.presentation.manga.components.MangaBottomActionMenu
import eu.kanade.presentation.manga.components.MangaChapterListItem
import eu.kanade.presentation.manga.components.MangaInfoBox
import eu.kanade.presentation.manga.components.MangaToolbar
import eu.kanade.presentation.manga.components.MissingChapterCountListItem
import eu.kanade.presentation.manga.components.rememberChapterScrollNavigation
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.getNameForMangaInfo
import eu.kanade.tachiyomi.ui.manga.ChapterList
import eu.kanade.tachiyomi.ui.manga.MangaViewModel
import eu.kanade.tachiyomi.util.chapter.getNextUnread
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.FormatListNumbered
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.StubSource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LocalPaperNavigationArea
import tachiyomi.presentation.core.components.PaperNavigationArea
import tachiyomi.presentation.core.components.PaperScrollColumn
import tachiyomi.presentation.core.components.TwoPanelBox
import tachiyomi.presentation.core.components.VerticalFastScroller
import tachiyomi.presentation.core.components.material.PaperButton
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.shouldExpandFAB
import tachiyomi.source.local.isLocal
import kotlin.time.Instant
import tachiyomi.presentation.core.components.PaperLazyColumn as LazyColumn
import tachiyomi.presentation.core.components.material.PaperExtendedActionButton as SmallExtendedFloatingActionButton
import tachiyomi.presentation.core.components.material.PaperSnackbarHost as SnackbarHost

@Composable
fun MangaScreen(
    state: MangaViewModel.State.Success,
    snackbarHostState: SnackbarHostState,
    nextUpdate: Instant?,
    isTabletUi: Boolean,

    navigateUp: () -> Unit,
    onChapterClicked: (Chapter) -> Unit,
    onDownloadChapter: ((List<ChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    downloadProgressProvider: (chapterId: Long) -> Int,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,

    // For tags menu
    onTagSearch: (String) -> Unit,

    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,

    // For cover dialog
    onCoverClicked: () -> Unit,

    // For top action menu
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onEditFetchIntervalClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    onEditNotesClicked: () -> Unit,

    // For bottom action menu
    onMultiBookmarkClicked: (List<Chapter>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<Chapter>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsReadClicked: (Chapter) -> Unit,
    onMultiDeleteClicked: (List<Chapter>) -> Unit,

    // For chapter swipe
    onToggleChapterRead: (ChapterList.Item) -> Unit,
    onToggleChapterBookmark: (ChapterList.Item) -> Unit,

    // Chapter selection
    onChapterSelected: (ChapterList.Item, Boolean, Boolean) -> Unit,
    onAllChapterSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
) {
    val context = LocalContext.current
    val chapters = state.chapterListItems
    val selected = state.isAnySelected
    var showDetails by rememberSaveable(state.manga.id) { mutableStateOf(true) }
    val nextChapter = remember(state.chapters, state.manga) { state.chapters.getNextUnread(state.manga) }
    val chapterState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val chapterNumbers = remember(chapters, state.chapters) {
        ChapterPageNumbers.from(
            chapters,
            state.chapters.maxOfOrNull { item ->
                item.chapter.chapterNumber.takeIf { it.isFinite() && it >= 0 } ?: 0.0
            },
        )
    }
    var jumpFromRow by remember { mutableStateOf<Int?>(null) }
    BackHandler(enabled = selected) { onAllChapterSelected(false) }

    val details: @Composable () -> Unit = {
        PaperScrollColumn(Modifier.fillMaxSize()) {
            MangaInfoBox(
                isTabletUi = isTabletUi,
                appBarPadding = 0.dp,
                manga = state.manga,
                source = state.source,
                isStubSource = state.source is StubSource,
                onCoverClick = onCoverClicked,
                doSearch = onSearch,
            )
            MangaActionRow(
                favorite = state.manga.favorite,
                trackingCount = state.trackingCount,
                nextUpdate = nextUpdate,
                isUserIntervalMode = state.manga.fetchInterval < 0,
                onAddToLibraryClicked = onAddToLibraryClicked,
                onWebViewClicked = onWebViewClicked,
                onWebViewLongClicked = onWebViewLongClicked,
                onTrackingClicked = onTrackingClicked,
                onEditIntervalClicked = onEditFetchIntervalClicked,
                onEditCategory = onEditCategoryClicked,
            )
            MangaReadingActions(
                onContinueReading = onContinueReading,
                canContinue = nextChapter != null && !selected,
                showingDetails = true,
                onSwitchView = if (isTabletUi) {
                    null
                } else {
                    { showDetails = false }
                },
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
            ) {
                Icon(MaterialSymbols.RoundedFilled.PlayArrow, null)
                val chapterName = nextChapter?.let {
                    if (it.isRecognizedNumber) {
                        stringResource(MR.strings.display_mode_chapter, formatChapterNumber(it.chapterNumber))
                    } else {
                        it.name
                    }
                }
                Text(
                    text = if (chapterName.isNullOrBlank()) {
                        stringResource(if (state.hasReadChapters) MR.strings.action_resume else MR.strings.action_start)
                    } else {
                        stringResource(
                            if (state.hasReadChapters) MR.strings.action_resume_from else MR.strings.action_start_from,
                            chapterName,
                        )
                    },
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ExpandableMangaDescription(
                defaultExpandState = true,
                description = state.manga.description,
                tagsProvider = { state.manga.genre },
                notes = state.manga.notes,
                onTagSearch = onTagSearch,
                onCopyTagToClipboard = { if (it.isNotEmpty()) context.copyToClipboard(it, it) },
                onEditNotes = onEditNotesClicked,
            )
        }
    }
    val chapterPane: @Composable () -> Unit = {
        CompositionLocalProvider(LocalPaperNavigationArea provides PaperNavigationArea.Chapters) {
            Column(Modifier.fillMaxSize()) {
                ChapterHeader(
                    enabled = !selected,
                    chapterCount = state.processedChapters.size,
                    missingChapterCount = state.missingChapterCount,
                    onClick = onFilterButtonClicked,
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = chapterState,
                    alwaysShowControls = true,
                    fillRowGaps = true,
                    pageLabel = chapterNumbers?.let { numbers -> { first, last -> numbers.label(first, last) } },
                    onJump = chapterNumbers?.let { { row -> jumpFromRow = row } },
                    beforeControls = {
                        if (!selected) {
                            MangaReadingActions(
                                onContinueReading = onContinueReading,
                                canContinue = state.hasUnreadChapters,
                                showingDetails = false,
                                onSwitchView = if (isTabletUi) {
                                    null
                                } else {
                                    { showDetails = true }
                                },
                                modifier = Modifier.padding(horizontal = 32.dp),
                            ) {
                                Icon(MaterialSymbols.RoundedFilled.PlayArrow, null)
                                Text(
                                    stringResource(
                                        if (state.hasReadChapters) {
                                            MR.strings.action_resume
                                        } else {
                                            MR.strings.action_start
                                        },
                                    ),
                                )
                            }
                        }
                    },
                ) {
                    sharedChapterItems(
                        manga = state.manga,
                        chapters = chapters,
                        isAnyChapterSelected = selected,

                        onChapterClicked = onChapterClicked,
                        onDownloadChapter = onDownloadChapter,
                        downloadProgressProvider = downloadProgressProvider,
                        onChapterSelected = onChapterSelected,
                        onToggleChapterRead = onToggleChapterRead,
                        onToggleChapterBookmark = onToggleChapterBookmark,
                    )
                }
            }
        }
    }
    Scaffold(
        topBar = {
            MangaToolbar(
                title = state.manga.title,
                hasFilters = state.filterActive,
                navigateUp = navigateUp,
                onClickFilter = onFilterButtonClicked,
                onClickShare = onShareClicked,
                onClickDownload = onDownloadActionClicked,
                onClickEditCategory = onEditCategoryClicked,
                onClickRefresh = onRefresh,
                onClickMigrate = onMigrateClicked,
                onClickEditNotes = onEditNotesClicked,
                actionModeCounter = state.selectedChapters.size,
                onCancelActionMode = { onAllChapterSelected(false) },
                onSelectAll = { onAllChapterSelected(true) },
                onInvertSelection = onInvertSelection,
                titleAlphaProvider = { 1f },
                backgroundAlphaProvider = { 1f },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            SharedMangaBottomActionMenu(
                selected = state.selectedChapters,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
                onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked,
                onDownloadChapter = onDownloadChapter,
                onMultiDeleteClicked = onMultiDeleteClicked,
                fillFraction = 1f,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            if (isTabletUi) {
                TwoPanelBox(
                    startContent = { details() },
                    endContent = { chapterPane() },
                )
            } else {
                if (showDetails) details() else chapterPane()
            }
        }
    }
    val jumpRow = jumpFromRow
    if (jumpRow != null && chapterNumbers != null) {
        ChapterPageJumpDialog(
            numbers = chapterNumbers,
            currentRow = jumpRow,
            onSelectRow = { row -> scope.launch { chapterState.scrollToItem(row) } },
            onDismissRequest = { jumpFromRow = null },
        )
    }
}

/** One outline and one divider keep the two actions from drawing doubled adjoining borders. */
@Composable
private fun MangaReadingActions(
    onContinueReading: () -> Unit,
    canContinue: Boolean,
    showingDetails: Boolean,
    onSwitchView: (() -> Unit)?,
    modifier: Modifier = Modifier,
    readingLabel: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).height(IntrinsicSize.Min)
            .clip(shape).border(1.dp, Color.Black, shape),
    ) {
        PaperButton(
            onClick = onContinueReading,
            enabled = canContinue,
            modifier = Modifier.weight(if (onSwitchView == null) 1f else 2f).fillMaxHeight(),
            shape = RectangleShape,
            border = null,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            content = readingLabel,
        )
        if (onSwitchView != null) {
            Box(Modifier.width(1.dp).fillMaxHeight().background(Color.Black))
            PaperButton(
                onClick = onSwitchView,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RectangleShape,
                border = null,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Icon(
                    if (showingDetails) MaterialSymbols.Rounded.FormatListNumbered else MaterialSymbols.Rounded.Info,
                    null,
                )
                Text(
                    stringResource(if (showingDetails) MR.strings.chapters else MR.strings.daihon_details),
                    Modifier.padding(start = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SharedMangaBottomActionMenu(
    selected: List<ChapterList.Item>,
    onMultiBookmarkClicked: (List<Chapter>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<Chapter>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsReadClicked: (Chapter) -> Unit,
    onDownloadChapter: ((List<ChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    onMultiDeleteClicked: (List<Chapter>) -> Unit,
    fillFraction: Float,
    modifier: Modifier = Modifier,
) {
    MangaBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = modifier.fillMaxWidth(fillFraction),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected.fastMap { it.chapter }, true)
        }.takeIf { selected.fastAny { !it.chapter.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected.fastMap { it.chapter }, false)
        }.takeIf { selected.fastAll { it.chapter.bookmark } },
        onMarkAsReadClicked = {
            onMultiMarkAsReadClicked(selected.fastMap { it.chapter }, true)
        }.takeIf { selected.fastAny { !it.chapter.read } },
        onMarkAsUnreadClicked = {
            onMultiMarkAsReadClicked(selected.fastMap { it.chapter }, false)
        }.takeIf { selected.fastAny { it.chapter.read || it.chapter.lastPageRead > 0 } },
        onMarkPreviousAsReadClicked = {
            onMarkPreviousAsReadClicked(selected[0].chapter)
        }.takeIf { selected.size == 1 },
        onDownloadClicked = {
            onDownloadChapter!!(selected.toList(), ChapterDownloadAction.START)
        }.takeIf {
            onDownloadChapter != null && selected.fastAny { it.downloadState != Download.State.DOWNLOADED }
        },
        onDeleteClicked = {
            onMultiDeleteClicked(selected.fastMap { it.chapter })
        }.takeIf {
            selected.fastAny { it.downloadState == Download.State.DOWNLOADED }
        },
    )
}

private fun LazyListScope.sharedChapterItems(
    manga: Manga,
    chapters: List<ChapterList>,
    isAnyChapterSelected: Boolean,

    onChapterClicked: (Chapter) -> Unit,
    onDownloadChapter: ((List<ChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    downloadProgressProvider: (chapterId: Long) -> Int,
    onChapterSelected: (ChapterList.Item, Boolean, Boolean) -> Unit,
    onToggleChapterRead: (ChapterList.Item) -> Unit,
    onToggleChapterBookmark: (ChapterList.Item) -> Unit,
) {
    items(
        items = chapters,
        key = { item ->
            when (item) {
                is ChapterList.MissingCount -> "missing-count-${item.id}"
                is ChapterList.Item -> "chapter-${item.id}"
            }
        },
        contentType = { MangaScreenItem.CHAPTER },
    ) { item ->
        val haptic = LocalHapticFeedback.current

        when (item) {
            is ChapterList.MissingCount -> {
                MissingChapterCountListItem(count = item.count)
            }
            is ChapterList.Item -> {
                MangaChapterListItem(
                    title = if (manga.displayMode == Manga.CHAPTER_DISPLAY_NUMBER) {
                        stringResource(
                            MR.strings.display_mode_chapter,
                            formatChapterNumber(item.chapter.chapterNumber),
                        )
                    } else {
                        item.chapter.name
                    },
                    date = relativeDateText(item.chapter.dateUpload),
                    readProgress = item.chapter.lastPageRead
                        .takeIf { !item.chapter.read && it > 0L }
                        ?.let {
                            stringResource(
                                MR.strings.chapter_progress,
                                it + 1,
                            )
                        },
                    scanlator = item.chapter.scanlator.takeIf { !it.isNullOrBlank() },
                    read = item.chapter.read,
                    bookmark = item.chapter.bookmark,
                    selected = item.selected,
                    actionsEnabled = !isAnyChapterSelected,
                    downloadIndicatorEnabled = !isAnyChapterSelected && !manga.isLocal(),
                    downloadStateProvider = { item.downloadState },
                    downloadProgressProvider = { downloadProgressProvider(item.id) },

                    onLongClick = {
                        onChapterSelected(item, !item.selected, true)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onClick = {
                        onChapterItemClick(
                            chapterItem = item,
                            isAnyChapterSelected = isAnyChapterSelected,
                            onToggleSelection = { onChapterSelected(item, !item.selected, false) },
                            onChapterClicked = onChapterClicked,
                        )
                    },
                    onDownloadClick = if (onDownloadChapter != null) {
                        { onDownloadChapter(listOf(item), it) }
                    } else {
                        null
                    },
                    onToggleRead = { onToggleChapterRead(item) },
                    onToggleBookmark = { onToggleChapterBookmark(item) },
                )
            }
        }
    }
}

private fun onChapterItemClick(
    chapterItem: ChapterList.Item,
    isAnyChapterSelected: Boolean,
    onToggleSelection: (Boolean) -> Unit,
    onChapterClicked: (Chapter) -> Unit,
) {
    when {
        chapterItem.selected -> onToggleSelection(false)
        isAnyChapterSelected -> onToggleSelection(true)
        else -> onChapterClicked(chapterItem.chapter)
    }
}
