package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import tachiyomi.core.common.preference.TriState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.CollapsibleBox
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SelectItem
import tachiyomi.presentation.core.components.SortItem
import tachiyomi.presentation.core.components.TextItem
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.components.material.Button
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.components.PaperLazyColumn as LazyColumn
import tachiyomi.presentation.core.components.material.PaperButton as TextButton

@Composable
fun SourceFilterDialog(
    onDismissRequest: () -> Unit,
    filters: FilterList,
    onReset: () -> Unit,
    onFilter: () -> Unit,
    onUpdate: (FilterList) -> Unit,
) {
    val updateFilters = { onUpdate(filters) }
    val expanded = remember(filters) { mutableStateMapOf<Filter<*>, Boolean>() }

    AdaptiveSheet(onDismissRequest = onDismissRequest) {
        Column {
            Column {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .padding(8.dp),
                ) {
                    TextButton(onClick = onReset) {
                        Text(
                            text = stringResource(MR.strings.action_reset),
                            style = LocalTextStyle.current.copy(
                                color = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(onClick = {
                        onFilter()
                        onDismissRequest()
                    }) {
                        Text(stringResource(MR.strings.action_filter))
                    }
                }
                HorizontalDivider()
            }

            LazyColumn(Modifier.weight(1f, fill = false)) {
                filterItems(filters, expanded, { expanded[it] = expanded[it] != true }, updateFilters)
            }
        }
    }
}

private fun LazyListScope.filterItems(
    filters: List<Filter<*>>,
    expanded: Map<Filter<*>, Boolean>,
    onToggle: (Filter<*>) -> Unit,
    onUpdate: () -> Unit,
) {
    filters.forEach { filter ->
        when (filter) {
            is Filter.Group<*>, is Filter.Sort -> {
                val children = (filter as? Filter.Group<*>)?.state?.filterIsInstance<Filter<*>>()
                val open = expanded[filter] == true
                val hasChildren = children?.isNotEmpty() ?: (filter as Filter.Sort).values.isNotEmpty()
                item(contentType = if (open && hasChildren) "header" else "item") {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { onToggle(filter) }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Text(filter.name, Modifier.weight(1f))
                        Icon(
                            if (open) {
                                MaterialSymbols.Rounded.KeyboardArrowUp
                            } else {
                                MaterialSymbols.Rounded.KeyboardArrowDown
                            },
                            null,
                        )
                    }
                }
                if (open && children != null) {
                    filterItems(children, expanded, onToggle, onUpdate)
                } else if (open && filter is Filter.Sort) {
                    items(filter.values.size) { index ->
                        val ascending = filter.state?.ascending?.takeIf { index == filter.state?.index }
                        SortItem(
                            label = filter.values[index],
                            sortDescending = ascending?.not(),
                            onClick = {
                                filter.state = Filter.Sort.Selection(
                                    index,
                                    if (index == filter.state?.index) {
                                        !filter.state!!.ascending
                                    } else {
                                        filter.state?.ascending ?: true
                                    },
                                )
                                onUpdate()
                            },
                        )
                    }
                }
            }
            else -> item(contentType = if (filter is Filter.Header) "header" else "item") {
                FilterItem(filter, onUpdate)
            }
        }
    }
}

@Composable
private fun FilterItem(filter: Filter<*>, onUpdate: () -> Unit) {
    when (filter) {
        // Group and sort containers are emitted as individual lazy rows by filterItems.
        is Filter.Group<*>, is Filter.Sort -> Unit
        is Filter.Header -> {
            HeadingItem(filter.name)
        }
        is Filter.Separator -> {
            HorizontalDivider()
        }
        is Filter.CheckBox -> {
            CheckboxItem(
                label = filter.name,
                checked = filter.state,
            ) {
                filter.state = !filter.state
                onUpdate()
            }
        }
        is Filter.TriState -> {
            TriStateItem(
                label = filter.name,
                state = filter.state.toTriStateFilter(),
            ) {
                filter.state = filter.state.toTriStateFilter().next().toTriStateInt()
                onUpdate()
            }
        }
        is Filter.Text -> {
            TextItem(
                label = filter.name,
                value = filter.state,
            ) {
                filter.state = it
                onUpdate()
            }
        }
        is Filter.Select<*> -> {
            SelectItem(
                label = filter.name,
                options = filter.values,
                selectedIndex = filter.state,
            ) {
                filter.state = it
                onUpdate()
            }
        }
    }
}

private fun Int.toTriStateFilter(): TriState {
    return when (this) {
        Filter.TriState.STATE_IGNORE -> TriState.DISABLED
        Filter.TriState.STATE_INCLUDE -> TriState.ENABLED_IS
        Filter.TriState.STATE_EXCLUDE -> TriState.ENABLED_NOT
        else -> TriState.DISABLED
    }
}

private fun TriState.toTriStateInt(): Int {
    return when (this) {
        TriState.DISABLED -> Filter.TriState.STATE_IGNORE
        TriState.ENABLED_IS -> Filter.TriState.STATE_INCLUDE
        TriState.ENABLED_NOT -> Filter.TriState.STATE_EXCLUDE
    }
}
