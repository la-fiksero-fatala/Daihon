package eu.kanade.presentation.manga.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.data.download.model.Download
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.RemoveDone
import mihon.icons.materialsymbols.roundedfilled.Bookmark
import mihon.icons.materialsymbols.roundedfilled.Circle
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground
import mihon.icons.materialsymbols.rounded.Bookmark as BookmarkOutline
import tachiyomi.presentation.core.components.material.PaperIconButton as IconButton

@Composable
fun MangaChapterListItem(
    title: String,
    date: String?,
    readProgress: String?,
    scanlator: String?,
    read: Boolean,
    bookmark: Boolean,
    selected: Boolean,
    actionsEnabled: Boolean,
    downloadIndicatorEnabled: Boolean,
    downloadStateProvider: () -> Download.State,
    downloadProgressProvider: () -> Int,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onDownloadClick: ((ChapterDownloadAction) -> Unit)?,
    onToggleRead: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .selectedBackground(selected)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!read) {
                    Icon(
                        imageVector = MaterialSymbols.RoundedFilled.Circle,
                        contentDescription = stringResource(MR.strings.unread),
                        modifier = Modifier
                            .height(8.dp)
                            .padding(end = 4.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,

                    color = LocalContentColor.current,
                )
            }

            Row {
                val subtitleStyle = MaterialTheme.typography.bodySmall
                    .merge(
                        color = LocalContentColor.current
                            .copy(alpha = 1f),
                    )
                ProvideTextStyle(value = subtitleStyle) {
                    if (date != null) {
                        Text(
                            text = date,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (readProgress != null || scanlator != null) DotSeparatorText()
                    }
                    if (readProgress != null) {
                        Text(
                            text = readProgress,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = LocalContentColor.current,
                        )
                        if (scanlator != null) DotSeparatorText()
                    }
                    if (scanlator != null) {
                        Text(
                            text = scanlator,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        IconButton(onClick = onToggleBookmark, enabled = actionsEnabled) {
            Icon(
                imageVector = if (bookmark) {
                    MaterialSymbols.RoundedFilled.Bookmark
                } else {
                    MaterialSymbols.Rounded.BookmarkOutline
                },
                contentDescription = stringResource(
                    if (bookmark) MR.strings.action_remove_bookmark else MR.strings.action_bookmark,
                ),
            )
        }
        IconButton(onClick = onToggleRead, enabled = actionsEnabled) {
            Icon(
                imageVector = if (read) MaterialSymbols.Rounded.RemoveDone else MaterialSymbols.Rounded.Check,
                contentDescription = stringResource(
                    if (read) MR.strings.action_mark_as_unread else MR.strings.action_mark_as_read,
                ),
            )
        }
        ChapterDownloadIndicator(
            enabled = downloadIndicatorEnabled,
            modifier = Modifier.padding(start = 4.dp),
            downloadStateProvider = downloadStateProvider,
            downloadProgressProvider = downloadProgressProvider,
            onClick = { onDownloadClick?.invoke(it) },
        )
    }
}
