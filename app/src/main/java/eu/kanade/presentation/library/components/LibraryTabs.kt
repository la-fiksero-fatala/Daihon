package eu.kanade.presentation.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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
    currentIndex: Int,
    getItemCountForCategory: (Category) -> Int?,
    onTabItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    iconOnly: Boolean = false,
) {
    if (categories.isEmpty()) return
    val current = currentIndex.coerceIn(categories.indices)
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.then(if (iconOnly) Modifier.width(48.dp) else Modifier.fillMaxWidth()).heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = { expanded = true })
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!iconOnly) {
            Text(categories[current].visualName, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            getItemCountForCategory(categories[current])?.let {
                Text(it.toString(), Modifier.padding(horizontal = 8.dp))
            }
        }
        Icon(MaterialSymbols.Rounded.KeyboardArrowDown, stringResource(MR.strings.daihon_categories))
    }
    if (expanded) {
        PaperAlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text(stringResource(MR.strings.daihon_categories)) },
            text = {
                PaperLazyColumn(modifier = Modifier.heightIn(max = 360.dp).selectableGroup()) {
                    itemsIndexed(categories, key = { _, category -> category.id }) { index, category ->
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                .selectable(
                                    selected = index == current,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onTabItemClick(index)
                                        expanded = false
                                    },
                                ).padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(category.visualName, Modifier.weight(1f))
                            getItemCountForCategory(category)?.let { Text(it.toString()) }
                            if (index == current) {
                                Icon(MaterialSymbols.Rounded.Check, stringResource(MR.strings.selected))
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
