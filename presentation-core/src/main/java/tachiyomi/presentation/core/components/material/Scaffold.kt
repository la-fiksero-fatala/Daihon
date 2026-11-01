package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.LocalPaperPageIndicator
import tachiyomi.presentation.core.components.PaperPageIndicatorState

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBarScrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
        rememberTopAppBarState(),
    ),
    topBar: @Composable (TopAppBarScrollBehavior) -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    content: @Composable (PaddingValues) -> Unit,
) {
    var fabHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    val indicator = remember { PaperPageIndicatorState() }
    CompositionLocalProvider(LocalPaperPageIndicator provides indicator) {
        androidx.compose.material3.Scaffold(
            modifier = modifier.border(1.dp, Color.Black),
            topBar = {
                Row(Modifier.border(1.dp, Color.Black), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).onSizeChanged { indicator.hasHeader = it.height > 0 }) {
                        topBar(topBarScrollBehavior)
                    }
                    if (indicator.hasHeader) indicator.Indicator(Modifier.statusBarsPadding())
                }
            },
            bottomBar = { Box(Modifier.border(1.dp, Color.Black)) { bottomBar() } },
            snackbarHost = snackbarHost,
            floatingActionButton = {
                Box(Modifier.onSizeChanged { fabHeight = it.height }) {
                    floatingActionButton()
                }
            },
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = containerColor,
            contentColor = contentColor,
            contentWindowInsets = contentWindowInsets,
        ) { contentPadding ->
            val paddingValues = remember(contentPadding) {
                FabAwarePaddingValues(contentPadding) {
                    if (fabHeight == 0) 0.dp else with(density) { fabHeight.toDp() } + FabSpacing
                }
            }
            content(paddingValues)
        }
    }
}

private class FabAwarePaddingValues(
    private val base: PaddingValues,
    private val extraBottom: () -> Dp,
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        base.calculateLeftPadding(layoutDirection)

    override fun calculateTopPadding(): Dp = base.calculateTopPadding()

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        base.calculateRightPadding(layoutDirection)

    override fun calculateBottomPadding(): Dp = base.calculateBottomPadding() + extraBottom()
}
private val FabSpacing = 16.dp
