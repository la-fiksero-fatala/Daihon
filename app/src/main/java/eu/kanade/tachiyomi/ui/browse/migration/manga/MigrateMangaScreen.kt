package eu.kanade.tachiyomi.ui.browse.migration.manga

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.library.components.MangaComfortableGridItem
import eu.kanade.presentation.manga.components.BaseMangaListItem
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import mihon.feature.migration.config.MigrationConfigScreen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowForward
import mihon.icons.materialsymbols.rounded.SelectAll
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.PaperLazyVerticalGrid
import tachiyomi.presentation.core.components.PaperNavigationArea
import tachiyomi.presentation.core.components.material.PaperButton
import tachiyomi.presentation.core.components.material.PaperIconButton
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.selectedBackground
import tachiyomi.presentation.core.util.shouldExpandFAB
import tachiyomi.presentation.core.components.material.PaperExtendedActionButton as SmallExtendedFloatingActionButton

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
        val viewModel =
            assistedMetroViewModel<MigrateMangaViewModel, MigrateMangaViewModel.Factory> {
                create(sourceIds = sourceIds)
            }

        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state.isLoading) {
            LoadingScreen()
            return
        }

        var selecting by rememberSaveable { mutableStateOf(false) }
        val inLibrary = extensionTitle != null
        val selectionMode = selecting || state.selectionMode

        fun clearSelection() {
            selecting = false
            viewModel.clearSelection()
        }

        fun startMigration() {
            val selection = state.selection
            if (selection.isEmpty()) {
                selecting = true
                return
            }
            clearSelection()
            navigator.push(MigrationConfigScreen(selection))
        }

        BackHandler(enabled = selectionMode, onBack = ::clearSelection)

        val lazyListState = rememberLazyListState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = when {
                        selectionMode -> stringResource(MR.strings.daihon_source_selected, state.selection.size)
                        extensionTitle != null -> extensionTitle
                        else -> state.source!!.name
                    },
                    navigateUp = { if (selectionMode) clearSelection() else navigator.pop() },
                    actions = {
                        if (!state.isEmpty && (!inLibrary || selectionMode)) {
                            PaperIconButton(onClick = viewModel::selectAll) {
                                Icon(MaterialSymbols.Rounded.SelectAll, stringResource(MR.strings.action_select_all))
                            }
                        }
                        if (inLibrary && !state.isEmpty) {
                            PaperButton(
                                onClick = ::startMigration,
                                enabled = !selectionMode || state.selection.isNotEmpty(),
                            ) { Text(stringResource(MR.strings.action_migrate)) }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                if (!inLibrary) {
                    SmallExtendedFloatingActionButton(
                        text = { Text(text = stringResource(MR.strings.migrationConfigScreen_continueButtonText)) },
                        icon = {
                            Icon(
                                imageVector = MaterialSymbols.AutoMirroredRounded.ArrowForward,
                                contentDescription = null,
                            )
                        },
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
            if (state.isEmpty) {
                EmptyScreen(
                    stringRes = MR.strings.empty_screen,
                    modifier = Modifier.padding(contentPadding),
                )
                return@Scaffold
            }

            if (inLibrary) {
                PaperLazyVerticalGrid(
                    columns = GridCells.Adaptive(112.dp),
                    contentPadding = contentPadding + PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.titles, key = Manga::id) { manga ->
                        MangaComfortableGridItem(
                            coverData = MangaCover(
                                mangaId = manga.id,
                                sourceId = manga.source,
                                isMangaFavorite = manga.favorite,
                                url = manga.thumbnailUrl,
                                lastModified = manga.coverLastModified,
                            ),
                            title = manga.title,
                            isSelected = manga.id in state.selection,
                            onClick = {
                                if (selectionMode) {
                                    viewModel.toggleSelection(manga)
                                } else {
                                    navigator.push(MangaScreen(manga.id))
                                }
                            },
                            onLongClick = { viewModel.toggleSelection(manga) },
                        )
                    }
                }
            } else {
                MigrateMangaContent(
                    lazyListState = lazyListState,
                    contentPadding = contentPadding,
                    state = state,
                    onClickItem = viewModel::toggleSelection,
                    onClickCover = { navigator.push(MangaScreen(it.id)) },
                )
            }
        }

        LaunchedEffect(Unit) {
            viewModel.events.collectLatest { event ->
                when (event) {
                    MigrationMangaEvent.FailedFetchingFavorites -> {
                        context.toast(MR.strings.internal_error)
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
            items(state.titles) { manga ->
                MigrateMangaItem(
                    manga = manga,
                    isSelected = manga.id in state.selection,
                    onClickItem = onClickItem,
                    onClickCover = onClickCover,
                )
            }
        }
    }

    @Composable
    private fun MigrateMangaItem(
        manga: Manga,
        isSelected: Boolean,
        onClickItem: (Manga) -> Unit,
        onClickCover: (Manga) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        BaseMangaListItem(
            modifier = modifier.selectedBackground(isSelected),
            manga = manga,
            onClickItem = { onClickItem(manga) },
            onClickCover = { onClickCover(manga) },
        )
    }
}
