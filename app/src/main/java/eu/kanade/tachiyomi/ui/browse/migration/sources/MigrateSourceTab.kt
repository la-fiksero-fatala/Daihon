package eu.kanade.tachiyomi.ui.browse.migration.sources

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.domain.source.interactor.SetMigrateSorting
import eu.kanade.presentation.browse.MigrateSourceScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.ui.browse.migration.manga.MigrateMangaScreen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Help
import mihon.icons.materialsymbols.rounded.ArrowDownward
import mihon.icons.materialsymbols.rounded.ArrowUpward
import mihon.icons.materialsymbols.rounded.Numbers
import mihon.icons.materialsymbols.rounded.SortByAlpha
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun Screen.migrateSourceTab(): TabContent {
    val uriHandler = LocalUriHandler.current
    val navigator = LocalNavigator.currentOrThrow
    val viewModel = metroViewModel<MigrateSourceViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    return TabContent(
        titleRes = MR.strings.label_migration,
        actions = listOf(
            AppBar.Action(
                title = stringResource(
                    if (state.sortingMode == SetMigrateSorting.Mode.ALPHABETICAL) {
                        MR.strings.action_sort_alpha
                    } else {
                        MR.strings.action_sort_count
                    },
                ),
                icon = if (state.sortingMode == SetMigrateSorting.Mode.ALPHABETICAL) {
                    MaterialSymbols.Rounded.SortByAlpha
                } else {
                    MaterialSymbols.Rounded.Numbers
                },
                onClick = viewModel::toggleSortingMode,
            ),
            AppBar.Action(
                title = stringResource(
                    if (state.sortingDirection == SetMigrateSorting.Direction.ASCENDING) {
                        MR.strings.action_asc
                    } else {
                        MR.strings.action_desc
                    },
                ),
                icon = if (state.sortingDirection == SetMigrateSorting.Direction.ASCENDING) {
                    MaterialSymbols.Rounded.ArrowUpward
                } else {
                    MaterialSymbols.Rounded.ArrowDownward
                },
                onClick = viewModel::toggleSortingDirection,
            ),
            AppBar.Action(
                title = stringResource(MR.strings.migration_help_guide),
                icon = MaterialSymbols.AutoMirroredRounded.Help,
                onClick = {
                    uriHandler.openUri("https://mihon.app/docs/guides/source-migration")
                },
            ),
        ),
        content = { contentPadding, _ ->
            MigrateSourceScreen(
                state = state,
                contentPadding = contentPadding,
                onClickItem = { source ->
                    navigator.push(MigrateMangaScreen(source.id))
                },
            )
        },
    )
}
