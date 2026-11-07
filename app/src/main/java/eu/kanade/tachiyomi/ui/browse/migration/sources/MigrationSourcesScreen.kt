package eu.kanade.tachiyomi.ui.browse.migration.sources

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.PaperNavigationArea
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

/** Also lists historical sources whose extension is no longer installed. */
class MigrationSourcesScreen : Screen() {
    override val pageNavigationArea get() = PaperNavigationArea.Browse

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val content = migrateSourceTab()
        val snackbar = remember { SnackbarHostState() }
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.label_migration),
                    navigateUp = navigator::pop,
                    actions = { AppBarActions(content.actions) },
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding -> content.content(padding, snackbar) }
    }
}
