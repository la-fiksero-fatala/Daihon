package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.components.material.PaperButton as TextButton

internal data class LibrarySearchActions(val allCategories: (() -> Unit)? = null, val searchingAll: Boolean = false)
internal val LocalLibrarySearchActions = staticCompositionLocalOf { LibrarySearchActions() }

@Composable
internal fun GlobalSearchItem(
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = LocalLibrarySearchActions.current
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onClick) {
            Text(stringResource(MR.strings.action_global_search_query, searchQuery), Modifier.zIndex(99f))
        }
        actions.allCategories?.let { onAllCategories ->
            TextButton(onClick = onAllCategories, enabled = !actions.searchingAll) {
                Text(stringResource(MR.strings.daihon_search_all_categories))
            }
        }
    }
}
