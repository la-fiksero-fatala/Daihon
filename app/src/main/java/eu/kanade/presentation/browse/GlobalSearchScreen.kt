package eu.kanade.presentation.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.presentation.browse.components.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.presentation.browse.components.GlobalSearchToolbar
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchViewModel
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SourceFilter
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PaperAlertDialog
import tachiyomi.presentation.core.components.material.PaperButton
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.components.PaperLazyColumn as LazyColumn

@Composable
fun GlobalSearchScreen(
    state: SearchViewModel.State,
    navigateUp: () -> Unit,
    onChangeSearchQuery: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onChangeSearchFilter: (SourceFilter) -> Unit,
    onToggleResults: () -> Unit,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            GlobalSearchToolbar(
                searchQuery = state.searchQuery,
                progress = state.progress,
                total = state.total,
                navigateUp = navigateUp,
                onChangeSearchQuery = onChangeSearchQuery,
                onSearch = onSearch,
                hideSourceFilter = false,
                sourceFilter = state.sourceFilter,
                onChangeSearchFilter = onChangeSearchFilter,
                onlyShowHasResults = state.onlyShowHasResults,
                onToggleResults = onToggleResults,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        GlobalSearchContent(
            items = state.filteredItems,
            contentPadding = paddingValues,
            getManga = getManga,
            onClickSource = onClickSource,
            onClickItem = onClickItem,
            onLongClickItem = onLongClickItem,
        )
    }
}

@Composable
internal fun GlobalSearchContent(
    items: Map<Source, SearchItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (Source) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
    fromSourceId: Long? = null,
) {
    var selectedId by rememberSaveable { mutableStateOf(fromSourceId) }
    var chooseSource by remember { mutableStateOf(false) }
    val source = items.keys.find { it.id == selectedId } ?: items.keys.firstOrNull()
    SideEffect { if (source != null && selectedId != source.id) selectedId = source.id }
    Column(Modifier.fillMaxSize().padding(contentPadding).consumeWindowInsets(contentPadding)) {
        if (source != null) {
            PaperButton(onClick = { chooseSource = true }, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text("${source.name} · ${LocaleHelper.getLocalizedDisplayName(source.lang)} ▾")
            }
            PaperButton(onClick = {
                onClickSource(source)
            }, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Text(stringResource(MR.strings.daihon_browse_source))
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                key(source.id) {
                    when (val result = items[source]) {
                        SearchItemResult.Loading -> GlobalSearchLoadingResultItem()
                        is SearchItemResult.Success -> GlobalSearchCardRow(
                            result.result,
                            getManga,
                            onClickItem,
                            onLongClickItem,
                        )
                        is SearchItemResult.Error -> GlobalSearchErrorResultItem(message = result.throwable.message)
                        null -> Unit
                    }
                }
            }
        }
    }
    if (chooseSource) {
        PaperAlertDialog(
            onDismissRequest = { chooseSource = false },
            title = { Text(stringResource(MR.strings.daihon_choose_source)) },
            text = {
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items.forEach { (entry, result) ->
                        item(key = entry.id) {
                            PaperButton(onClick = {
                                selectedId = entry.id
                                chooseSource = false
                            }, modifier = Modifier.fillMaxWidth()) {
                                val count = (result as? SearchItemResult.Success)?.result?.size?.toString() ?: "…"
                                Text((if (entry == source) "✓ " else "") + "${entry.name} ($count)")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                PaperButton(onClick = { chooseSource = false }) { Text(stringResource(MR.strings.action_close)) }
            },
        )
    }
}
