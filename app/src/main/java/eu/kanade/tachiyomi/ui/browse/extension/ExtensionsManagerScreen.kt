package eu.kanade.tachiyomi.ui.browse.extension

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.PaperNavigationArea
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

class ExtensionsManagerScreen : Screen() {
    override val pageNavigationArea get() = PaperNavigationArea.Browse

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<ExtensionsViewModel>()
        val query by viewModel.searchQuery.collectAsStateWithLifecycle()
        val content = extensionsTab(viewModel)
        val snackbar = remember { SnackbarHostState() }
        Scaffold(
            topBar = { scrollBehavior ->
                SearchToolbar(
                    searchQuery = query,
                    onChangeSearchQuery = viewModel::search,
                    titleContent = { AppBarTitle(stringResource(MR.strings.label_extensions)) },
                    navigateUp = navigator::pop,
                    actions = { AppBarActions(content.actions) },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding -> content.content(padding, snackbar) }
    }
}
