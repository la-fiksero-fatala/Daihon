package eu.kanade.tachiyomi.ui.browse.source

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.browse.components.ExtensionIcon
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.more.settings.screen.browse.ContentLanguageScreen
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsManagerScreen
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsViewModel
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsScreen
import eu.kanade.tachiyomi.ui.browse.migration.manga.MigrateMangaScreen
import eu.kanade.tachiyomi.ui.browse.migration.sources.MigrationSourcesScreen
import eu.kanade.tachiyomi.ui.browse.source.SourcesDashboardViewModel.Tile
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceViewModel.Listing
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Download
import mihon.icons.materialsymbols.rounded.Explore
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.rounded.Sync
import mihon.icons.materialsymbols.rounded.TravelExplore
import mihon.icons.materialsymbols.roundedfilled.Extension
import mihon.icons.materialsymbols.roundedfilled.Favorite
import mihon.icons.materialsymbols.roundedfilled.Keep
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LocalPaperNavigationArea
import tachiyomi.presentation.core.components.LocalPaperNavigationSettings
import tachiyomi.presentation.core.components.PaperLazyColumn
import tachiyomi.presentation.core.components.PaperLazyVerticalGrid
import tachiyomi.presentation.core.components.material.PaperAlertDialog
import tachiyomi.presentation.core.components.material.PaperButton
import tachiyomi.presentation.core.components.material.PaperIconButton
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun SourcesDashboard() {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val viewModel = metroViewModel<SourcesDashboardViewModel>()
    val tiles by viewModel.tiles.collectAsStateWithLifecycle()
    val extensions = metroViewModel<ExtensionsViewModel>()
    val updates by extensions.updatesCount.collectAsStateWithLifecycle()
    val downloadSteps by extensions.downloadSteps.collectAsStateWithLifecycle()
    var confirmUpdateAll by rememberSaveable { mutableStateOf(false) }
    val columns by viewModel.preferences.sourceColumns.collectAsState()
    val preferLatest by viewModel.preferences.exploreLatest.collectAsState()
    var exploreKey by rememberSaveable { mutableStateOf<String?>(null) }
    var infoKey by rememberSaveable { mutableStateOf<String?>(null) }

    fun explore(source: Source) {
        exploreKey = null
        val listing = if (preferLatest && source.supportsLatest) Listing.Latest else Listing.Popular
        navigator.push(BrowseSourceScreen(source.id, listing.query))
    }

    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = stringResource(MR.strings.label_sources),
                actions = {
                    AppBarActions(
                        buildList {
                            if (updates > 0) {
                                add(
                                    AppBar.Action(
                                        title = stringResource(MR.strings.daihon_update_all_extensions),
                                        icon = MaterialSymbols.Rounded.Sync,
                                        onClick = { confirmUpdateAll = true },
                                    ),
                                )
                            }
                            addAll(
                                listOf(
                                    AppBar.Action(
                                        title = stringResource(MR.strings.action_global_search),
                                        icon = MaterialSymbols.Rounded.TravelExplore,
                                        onClick = { navigator.push(GlobalSearchScreen()) },
                                    ),
                                    AppBar.Action(
                                        title = stringResource(MR.strings.label_extensions),
                                        icon = MaterialSymbols.RoundedFilled.Extension,
                                        onClick = { navigator.push(ExtensionsManagerScreen()) },
                                    ),
                                    AppBar.OverflowAction(
                                        title = stringResource(MR.strings.daihon_content_language),
                                        onClick = { navigator.push(ContentLanguageScreen()) },
                                    ),
                                    AppBar.OverflowAction(
                                        title = stringResource(MR.strings.label_migration),
                                        onClick = { navigator.push(MigrationSourcesScreen()) },
                                    ),
                                ),
                            )
                        },
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val items = tiles
        when {
            items == null -> LoadingScreen(Modifier.padding(padding))
            items.isEmpty() -> EmptyScreen(MR.strings.source_empty_screen, modifier = Modifier.padding(padding))
            else -> BoxWithConstraints(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                val gap = 8.dp
                val minTileWidth = 152.dp
                val maxTileWidth = (maxHeight - gap * 2).coerceIn(1.dp, 256.dp)
                fun columnCount(width: Dp) =
                    ((width - gap) / (minTileWidth + gap)).toInt().coerceIn(1, columns.coerceIn(1, 5))

                fun tileWidth(width: Dp, columns: Int) =
                    ((width - gap * (columns + 1)) / columns).coerceIn(1.dp, maxTileWidth)

                val fullColumns = columnCount(maxWidth)
                val rows = (items.size - 1) / fullColumns + 1
                val needsPaging = (tileWidth(maxWidth, fullColumns) + gap + 1.dp) * rows + gap > maxHeight
                val navigation = LocalPaperNavigationSettings.current
                val railWidth = if (needsPaging && navigation.visible(LocalPaperNavigationArea.current)) {
                    navigation.height
                } else {
                    0.dp
                }
                val gridWidth = maxWidth - railWidth
                val fittedColumns = columnCount(gridWidth)
                // Keep row height stable if the smaller tiles fit and the rail disappears.
                val tileSide = tileWidth(gridWidth, fittedColumns)

                key(fittedColumns, tileSide, maxHeight) {
                    PaperLazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Fixed(fittedColumns),
                        contentPadding = PaddingValues(gap),
                        verticalArrangement = Arrangement.spacedBy(gap),
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        items(items, key = Tile::key) { tile ->
                            val update = tile.extension?.takeIf { it.hasUpdate }
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                SourceTile(
                                    tile = tile,
                                    modifier = Modifier.widthIn(max = tileSide),
                                    onExplore = {
                                        val source = tile.enabledSources.singleOrNull()
                                        if (source != null) explore(source) else exploreKey = tile.key
                                    },
                                    onInfo = {
                                        if (tile.extension != null) {
                                            navigator.push(ExtensionDetailsScreen(tile.extension.pkgName))
                                        } else {
                                            infoKey = tile.key
                                        }
                                    },
                                    onLibrary = { navigator.push(MigrateMangaScreen(tile.sourceIds, tile.name)) },
                                    onUpdate = update?.let { { extensions.updateExtension(it) } },
                                    updating = update?.let {
                                        downloadSteps[it.pkgName]?.isCompleted() == false
                                    } == true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmUpdateAll) {
        PaperAlertDialog(
            title = { Text(stringResource(MR.strings.daihon_confirm_update_all_extensions)) },
            confirmButton = {
                PaperButton(onClick = {
                    confirmUpdateAll = false
                    extensions.updateAllExtensions()
                }) {
                    Text(stringResource(MR.strings.ext_update_all))
                }
            },
            onDismissRequest = { confirmUpdateAll = false },
        )
    }

    tiles?.firstOrNull { it.key == exploreKey }?.let { tile ->
        PaperAlertDialog(
            title = { Text(tile.name) },
            text = {
                if (tile.enabledSources.isEmpty()) {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.daihon_no_enabled_languages),
                        onPreferenceClick = {
                            exploreKey = null
                            tile.extension?.let { navigator.push(ExtensionDetailsScreen(it.pkgName)) }
                        },
                    )
                } else {
                    val showNames = tile.enabledSources.map(Source::name).distinct().size > 1
                    PaperLazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(tile.enabledSources, key = Source::id) { source ->
                            val language = LocaleHelper.getSourceDisplayName(source.lang, context)
                            TextPreferenceWidget(
                                title = if (showNames) source.name else language,
                                subtitle = language.takeIf { showNames },
                                onPreferenceClick = { explore(source) },
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            onDismissRequest = { exploreKey = null },
        )
    }

    tiles?.firstOrNull { it.key == infoKey }?.let { tile ->
        PaperAlertDialog(
            title = { Text(tile.name) },
            text = {
                TextPreferenceWidget(
                    title = stringResource(if (tile.pinned) MR.strings.action_unpin else MR.strings.action_pin),
                    onPreferenceClick = {
                        tile.standalone?.let(viewModel::togglePin)
                        infoKey = null
                    },
                )
            },
            confirmButton = {},
            onDismissRequest = { infoKey = null },
        )
    }
}

@Composable
private fun SourceTile(
    tile: Tile,
    modifier: Modifier,
    onExplore: () -> Unit,
    onInfo: () -> Unit,
    onLibrary: () -> Unit,
    onUpdate: (() -> Unit)?,
    updating: Boolean,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 8.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tile.extension != null) {
                        ExtensionIcon(tile.extension, Modifier.size(32.dp))
                    } else {
                        tile.standalone?.let { SourceIcon(it, Modifier.size(32.dp)) }
                    }
                    if (tile.pinned) {
                        Icon(
                            MaterialSymbols.RoundedFilled.Keep,
                            stringResource(MR.strings.pinned_sources),
                            Modifier.padding(start = 4.dp).size(16.dp),
                        )
                    }
                }
                Text(
                    text = tile.name,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TileAction(MaterialSymbols.Rounded.Explore, stringResource(MR.strings.daihon_source_explore), onExplore)
                TileAction(MaterialSymbols.Rounded.Settings, stringResource(MR.strings.label_extension_info), onInfo)
                TileAction(
                    MaterialSymbols.RoundedFilled.Favorite,
                    stringResource(MR.strings.daihon_source_in_library),
                    onLibrary,
                )
            }
        }
        if (onUpdate != null) {
            TileAction(
                MaterialSymbols.Rounded.Download,
                stringResource(MR.strings.ext_update),
                onUpdate,
                modifier = Modifier.align(Alignment.TopEnd),
                enabled = !updating,
            )
        }
    }
}

@Composable
private fun TileAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PaperIconButton(onClick = onClick, modifier = modifier.size(48.dp), enabled = enabled) {
        Icon(icon, label, Modifier.size(22.dp))
    }
}
