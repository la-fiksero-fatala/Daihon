package eu.kanade.presentation.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.library.components.CommonMangaItemDefaults
import eu.kanade.presentation.library.components.MangaComfortableGridItem
import eu.kanade.presentation.library.components.libraryPageStart
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowLeft
import mihon.icons.materialsymbols.rounded.KeyboardArrowRight
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.manga.model.asMangaCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.components.material.PaperIconButton as IconButton

@Composable
fun GlobalSearchCardRow(
    titles: List<Manga>,
    getManga: @Composable (Manga) -> State<Manga>,
    onClick: (Manga) -> Unit,
    onLongClick: (Manga) -> Unit,
) {
    var anchor by rememberSaveable { mutableIntStateOf(0) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val arrowWidth = 48.dp.coerceAtMost(maxWidth / 3)
        val available = (maxWidth - arrowWidth * 2).coerceAtLeast(1.dp)
        val cardWidth = 88.dp.coerceAtMost(available)
        val gap = MaterialTheme.padding.extraSmall
        val density = LocalDensity.current
        val capacity = with(density) {
            searchResultRowCapacity(available.roundToPx(), cardWidth.roundToPx(), gap.roundToPx())
        }
        val start = libraryPageStart(anchor, capacity, titles.size)
        val end = (start + capacity).coerceAtMost(titles.size)
        Row(
            Modifier.fillMaxWidth().padding(vertical = MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { anchor = (start - capacity).coerceAtLeast(0) },
                enabled = start > 0,
                modifier = Modifier.width(arrowWidth),
            ) {
                Icon(MaterialSymbols.Rounded.KeyboardArrowLeft, stringResource(MR.strings.spen_previous_page))
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                if (titles.isEmpty()) {
                    EmptyResultItem()
                } else {
                    titles.subList(start, end).forEach { item ->
                        key(item.id) {
                            val title by getManga(item)
                            MangaItem(
                                title = title.title,
                                cover = title.asMangaCover(),
                                isFavorite = title.favorite,
                                width = cardWidth,
                                onClick = { onClick(title) },
                                onLongClick = { onLongClick(title) },
                            )
                        }
                    }
                }
            }
            IconButton(
                onClick = { anchor = end },
                enabled = end < titles.size,
                modifier = Modifier.width(arrowWidth),
            ) {
                Icon(MaterialSymbols.Rounded.KeyboardArrowRight, stringResource(MR.strings.spen_next_page))
            }
        }
    }
}

@Composable
private fun MangaItem(
    title: String,
    cover: MangaCover,
    isFavorite: Boolean,
    width: Dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(modifier = Modifier.width(width)) {
        MangaComfortableGridItem(
            title = title,
            titleMaxLines = 2,
            coverData = cover,
            coverBadgeStart = {
                InLibraryBadge(enabled = isFavorite)
            },
            coverAlpha = if (isFavorite) CommonMangaItemDefaults.BrowseFavoriteCoverAlpha else 1f,
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

@Composable
private fun EmptyResultItem() {
    Text(
        text = stringResource(MR.strings.no_results_found),
        modifier = Modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
    )
}

/** Complete cards only; both arrow slots are excluded before computing this width. */
internal fun searchResultRowCapacity(width: Int, cardWidth: Int, gap: Int): Int =
    ((width + gap) / (cardWidth + gap).coerceAtLeast(1)).coerceAtLeast(1)
