package eu.kanade.tachiyomi.ui.browse.migration.manga

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.components.TabbedDialog
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.presentation.library.components.LibraryMangaPage
import eu.kanade.presentation.library.components.LibraryPageJumpDialog
import eu.kanade.presentation.library.components.LibraryPageNumber
import eu.kanade.presentation.manga.components.BaseMangaListItem
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.library.LibraryNavigation
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mihon.feature.migration.config.MigrationConfigScreen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowForward
import mihon.icons.materialsymbols.rounded.Deselect
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.icons.materialsymbols.rounded.SelectAll
import mihon.icons.materialsymbols.rounded.SwapCalls
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.model.LibraryNavigationMode
import tachiyomi.domain.library.model.LibraryPageButtons
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.PaperNavigationArea
import tachiyomi.presentation.core.components.PaperScrollColumn
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.components.material.PaperExtendedActionButton
import tachiyomi.presentation.core.components.material.PaperIconButton
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.selectedBackground
import tachiyomi.presentation.core.util.shouldExpandFAB

data class MigrateMangaScreen(
    private val sourceIds: List<Long>,
    private val extensionTitle: String? = null,
) : Screen() {

    constructor(sourceId: Long) : this(listOf(sourceId))

    override val pageNavigationArea get() = PaperNavigationArea.Browse

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val inLibrary = extensionTitle != null
        val viewModel =
            assistedMetroViewModel<MigrateMangaViewModel, MigrateMangaViewModel.Factory> {
                create(sourceIds = sourceIds, inLibrary = inLibrary)
            }
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(viewModel) {
            viewModel.events.collectLatest { context.toast(MR.strings.internal_error) }
        }

        if (state.isLoading) {
            LoadingScreen()
            return
        }

        fun startMigration() {
            if (state.selection.isEmpty()) return
            val selection = state.selection
            viewModel.clearSelection()
            navigator.push(MigrationConfigScreen(selection))
        }

        BackHandler(enabled = state.selectionMode || state.search != null) {
            if (state.selectionMode) viewModel.clearSelection() else viewModel.setSearch(null)
        }

        val navigation = remember { LibraryNavigation() }
        var showOptions by rememberSaveable { mutableStateOf(false) }
        val lazyListState = rememberLazyListState()
        val pageInfo = navigation.displayedPageInfo?.takeIf { !state.isEmpty && it.count > 1 }
        val pageBadge: @Composable () -> Unit = {
            pageInfo?.let {
                LibraryPageNumber(
                    page = it.index,
                    pageCount = it.count,
                    compact = true,
                    onClick = { navigation.showPageDialog = true },
                )
            }
        }
        val selectionButton: @Composable () -> Unit = {
            PaperIconButton(onClick = viewModel::selectAll) {
                Icon(
                    if (state.allSelected) MaterialSymbols.Rounded.Deselect else MaterialSymbols.Rounded.SelectAll,
                    stringResource(
                        if (state.allSelected) {
                            MR.strings.migrationConfigScreen_selectNoneLabel
                        } else {
                            MR.strings.action_select_all
                        },
                    ),
                )
            }
        }

        Scaffold(
            topBar = { scrollBehavior ->
                if (inLibrary && !state.selectionMode) {
                    SearchToolbar(
                        searchQuery = state.search,
                        onChangeSearchQuery = viewModel::setSearch,
                        titleContent = { AppBarTitle(extensionTitle) },
                        navigateUp = { navigator.pop() },
                        beforeSearchActions = { pageBadge() },
                        actions = {
                            AppBarActions(
                                buildList {
                                    add(
                                        AppBar.Action(
                                            title = stringResource(MR.strings.action_sort),
                                            icon = MaterialSymbols.Rounded.FilterList,
                                            onClick = { showOptions = true },
                                        ),
                                    )
                                    if (!state.isEmpty) {
                                        add(
                                            AppBar.OverflowAction(
                                                title = stringResource(MR.strings.action_select_all),
                                                onClick = viewModel::selectAll,
                                            ),
                                        )
                                    }
                                },
                            )
                        },
                        scrollBehavior = scrollBehavior,
                    )
                } else {
                    AppBar(
                        titleContent = {
                            AppBarTitle(
                                if (state.selectionMode) {
                                    stringResource(MR.strings.daihon_source_selected, state.selection.size)
                                } else {
                                    state.source?.name
                                },
                            )
                        },
                        navigateUp = { navigator.pop() },
                        isActionMode = state.selectionMode,
                        onCancelActionMode = viewModel::clearSelection,
                        actions = {
                            pageBadge()
                            if (!state.isEmpty) selectionButton()
                            if (inLibrary && state.selectionMode) {
                                PaperIconButton(onClick = ::startMigration) {
                                    Icon(MaterialSymbols.Rounded.SwapCalls, stringResource(MR.strings.action_migrate))
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior,
                    )
                }
            },
            floatingActionButton = {
                if (!inLibrary) {
                    PaperExtendedActionButton(
                        text = { Text(text = stringResource(MR.strings.migrationConfigScreen_continueButtonText)) },
                        icon = { Icon(MaterialSymbols.AutoMirroredRounded.ArrowForward, contentDescription = null) },
                        onClick = ::startMigration,
                        expanded = lazyListState.shouldExpandFAB(),
                        modifier = Modifier.animateFloatingActionButton(
                            visible = state.selectionMode,
                            alignment = Alignment.BottomEnd,
                        ),
                    )
                }
            },
        ) { contentPadding ->
            when {
                state.isEmpty -> EmptyScreen(
                    stringRes = when {
                        !state.search.isNullOrBlank() -> MR.strings.no_results_found
                        state.filters.isNotEmpty() -> MR.strings.error_no_match
                        else -> MR.strings.empty_screen
                    },
                    modifier = Modifier.padding(contentPadding),
                )
                inLibrary -> SourceLibraryContent(
                    viewModel = viewModel,
                    state = state,
                    navigation = navigation,
                    contentPadding = contentPadding,
                    onClickManga = { manga ->
                        if (state.selectionMode) {
                            viewModel.toggleSelection(manga)
                        } else {
                            navigator.push(MangaScreen(manga.id))
                        }
                    },
                )
                else -> MigrateMangaContent(
                    lazyListState = lazyListState,
                    contentPadding = contentPadding,
                    state = state,
                    onClickItem = viewModel::toggleSelection,
                    onClickCover = { navigator.push(MangaScreen(it.id)) },
                )
            }
        }

        if (showOptions) {
            SourceLibraryOptions(state, viewModel, onDismiss = { showOptions = false })
        }
        val page = navigation.pageInfo
        if (navigation.showPageDialog && page != null) {
            LibraryPageJumpDialog(
                page = page.index,
                pageCount = page.count,
                onSelectPage = navigation::goToPage,
                onDismissRequest = { navigation.showPageDialog = false },
            )
        }
    }

    @Composable
    private fun SourceLibraryContent(
        viewModel: MigrateMangaViewModel,
        state: MigrateMangaViewModel.State,
        navigation: LibraryNavigation,
        contentPadding: PaddingValues,
        onClickManga: (Manga) -> Unit,
    ) {
        val preferences = viewModel.libraryPreferences
        val displayMode by preferences.displayMode.collectAsState()
        val columnPreference = if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            preferences.landscapeColumns
        } else {
            preferences.portraitColumns
        }
        val columns by columnPreference.collectAsState()
        val rows by preferences.pageRows.collectAsState()
        val stretch by preferences.stretchPageSpacing.collectAsState()
        val navigationMode by preferences.pageNavigation.collectAsState()
        val buttonPosition by preferences.pageButtons.collectAsState()
        val showContinue by preferences.showContinueReadingButton.collectAsState()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        LibraryMangaPage(
            items = state.libraryItems,
            navigation = navigation,
            paginated = true,
            active = true,
            displayMode = displayMode,
            columns = columns,
            pageRows = rows,
            stretchPageSpacing = stretch,
            // A smaller source viewport must not rewrite the Library preference.
            onPageRowsChange = {},
            pageButtons = if (navigationMode ==
                LibraryNavigationMode.Hidden
            ) {
                LibraryPageButtons.Hidden
            } else {
                buttonPosition
            },
            pageSlider = navigationMode == LibraryNavigationMode.Bar,
            contentPadding = contentPadding,
            hasActiveFilters = state.filters.isNotEmpty(),
            selection = state.selection,
            searchQuery = state.search,
            onClickManga = { onClickManga(it.manga) },
            onLongClickManga = { viewModel.toggleSelection(it.manga) },
            onClickContinueReading = if (showContinue && !state.selectionMode) {
                { item: LibraryManga ->
                    scope.launch {
                        val chapter = viewModel.getNextUnreadChapter(item.manga)
                        if (chapter == null) {
                            context.toast(MR.strings.no_next_chapter)
                        } else {
                            context.startActivity(ReaderActivity.newIntent(context, chapter.mangaId, chapter.id))
                        }
                    }
                }
            } else {
                null
            },
            usePaperInput = true,
        )
    }

    @Composable
    private fun SourceLibraryOptions(
        state: MigrateMangaViewModel.State,
        viewModel: MigrateMangaViewModel,
        onDismiss: () -> Unit,
    ) {
        TabbedDialog(
            onDismissRequest = onDismiss,
            tabTitles = listOf(stringResource(MR.strings.action_sort), stringResource(MR.strings.action_filter)),
        ) { page ->
            PaperScrollColumn(
                modifier = Modifier.padding(vertical = TabbedDialogPaddings.Vertical),
                state = rememberScrollState(),
            ) {
                if (page == 0) {
                    for ((label, type) in sourceSortOptions) {
                        SortItem(
                            label = stringResource(label),
                            sortDescending = if (state.sort.type == type) !state.sort.isAscending else null,
                            onClick = { viewModel.setSort(type) },
                        )
                    }
                } else {
                    for ((label, filter) in sourceFilterOptions) {
                        TriStateItem(
                            label = stringResource(label),
                            state = state.filters[filter] ?: TriState.DISABLED,
                            onClick = { viewModel.setFilter(filter, it) },
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MigrateMangaContent(
        lazyListState: LazyListState,
        contentPadding: PaddingValues,
        state: MigrateMangaViewModel.State,
        onClickItem: (Manga) -> Unit,
        onClickCover: (Manga) -> Unit,
    ) {
        FastScrollLazyColumn(
            state = lazyListState,
            contentPadding = contentPadding,
        ) {
            items(state.titles, key = Manga::id) { manga ->
                BaseMangaListItem(
                    modifier = Modifier.selectedBackground(manga.id in state.selection),
                    manga = manga,
                    onClickItem = { onClickItem(manga) },
                    onClickCover = { onClickCover(manga) },
                )
            }
        }
    }
}

private val sourceSortOptions = listOf(
    MR.strings.action_sort_alpha to LibrarySort.Type.Alphabetical,
    MR.strings.action_sort_total to LibrarySort.Type.TotalChapters,
    MR.strings.action_sort_last_read to LibrarySort.Type.LastRead,
    MR.strings.action_sort_last_manga_update to LibrarySort.Type.LastUpdate,
    MR.strings.action_sort_unread_count to LibrarySort.Type.UnreadCount,
    MR.strings.action_sort_latest_chapter to LibrarySort.Type.LatestChapter,
    MR.strings.action_sort_chapter_fetch_date to LibrarySort.Type.ChapterFetchDate,
    MR.strings.action_sort_date_added to LibrarySort.Type.DateAdded,
)

private val sourceFilterOptions = listOf(
    MR.strings.action_filter_unread to MigrateMangaViewModel.Filter.Unread,
    MR.strings.label_started to MigrateMangaViewModel.Filter.Started,
    MR.strings.action_filter_bookmarked to MigrateMangaViewModel.Filter.Bookmarked,
    MR.strings.completed to MigrateMangaViewModel.Filter.Completed,
)
