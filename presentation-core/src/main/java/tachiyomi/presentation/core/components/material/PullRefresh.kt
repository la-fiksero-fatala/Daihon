package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal val LocalPaperRefresh = staticCompositionLocalOf<(() -> Unit)?> { null }

/** A downward swipe at the first page requests refresh only after release. */
@Composable
fun PullRefresh(
    refreshing: Boolean,
    enabled: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    indicatorPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPaperRefresh provides onRefresh.takeIf { enabled && !refreshing }) {
        Box(modifier) {
            content()
            if (refreshing) {
                PaperSurface(Modifier.align(Alignment.TopCenter).padding(indicatorPadding)) {
                    Text(stringResource(MR.strings.loading), Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                }
            }
        }
    }
}
