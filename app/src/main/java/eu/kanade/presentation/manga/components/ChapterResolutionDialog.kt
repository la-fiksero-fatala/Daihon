package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.manga.ChapterResolutionProgress
import eu.kanade.tachiyomi.ui.manga.MangaViewModel
import kotlinx.coroutines.CancellationException
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.StaticLinearProgressIndicator
import tachiyomi.presentation.core.components.material.PaperAlertDialog
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ChapterResolutionDialog(chapter: Chapter, viewModel: MangaViewModel, onDismissRequest: () -> Unit) {
    var progress by remember(chapter.id) { mutableStateOf(ChapterResolutionProgress()) }
    var failed by remember(chapter.id) { mutableStateOf(false) }
    LaunchedEffect(chapter.id) {
        try {
            viewModel.checkChapterResolution(chapter) { progress = it }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            failed = true
        }
    }
    val finished = progress.complete || failed
    PaperAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(MR.strings.daihon_check_resolution)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(chapter.name)
                if (!finished) {
                    if (progress.total == 0) {
                        StaticLinearProgressIndicator(Modifier.fillMaxWidth())
                    } else {
                        StaticLinearProgressIndicator(
                            progress = { progress.checked.toFloat() / progress.total },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                if (progress.total > 0) {
                    Text(stringResource(MR.strings.daihon_resolution_progress, progress.checked, progress.total))
                }
                if (progress.median != null) {
                    Text(stringResource(MR.strings.daihon_median_resolution, progress.median.toString()))
                }
                if (failed || (progress.complete && progress.median == null)) {
                    Text(stringResource(MR.strings.daihon_resolution_failed))
                } else if (progress.failed > 0) {
                    Text(stringResource(MR.strings.daihon_resolution_partial, progress.failed))
                }
            }
        },
        confirmButton = {},
    )
}
