package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.category.visualName
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.PaperLazyColumn
import tachiyomi.presentation.core.components.material.PaperAlertDialog
import tachiyomi.presentation.core.components.material.PaperButton
import tachiyomi.presentation.core.i18n.stringResource

/** A category chooser never requires a sideways scroll, regardless of category count. */
@Composable
internal fun LibraryTabs(
    categories: List<Category>,
    pagerState: PagerState,
    getItemCountForCategory: (Category) -> Int?,
    onTabItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return
    val current = pagerState.currentPage.coerceIn(categories.indices)
    var expanded by remember { mutableStateOf(false) }
    PaperButton(onClick = { expanded = true }, modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Text(categories[current].visualName, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        getItemCountForCategory(categories[current])?.let { Text(it.toString(), Modifier.padding(horizontal = 8.dp)) }
        Icon(MaterialSymbols.Rounded.KeyboardArrowDown, stringResource(MR.strings.daihon_categories))
    }
    if (expanded) {
        PaperAlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text(stringResource(MR.strings.daihon_categories)) },
            text = {
                PaperLazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    itemsIndexed(categories, key = { _, category -> category.id }) { index, category ->
                        PaperButton(
                            onClick = {
                                onTabItemClick(index)
                                expanded = false
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(category.visualName, Modifier.weight(1f))
                                getItemCountForCategory(category)?.let { Text(it.toString()) }
                                if (index ==
                                    current
                                ) {
                                    Icon(MaterialSymbols.Rounded.Check, stringResource(MR.strings.selected))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                PaperButton(onClick = { expanded = false }) { Text(stringResource(MR.strings.action_close)) }
            },
        )
    }
}
