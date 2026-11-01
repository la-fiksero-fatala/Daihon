package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import tachiyomi.presentation.core.components.LocalPaperNavigationArea
import tachiyomi.presentation.core.components.LocalPaperPageIndicator
import tachiyomi.presentation.core.components.PaperNavigation
import tachiyomi.presentation.core.components.PaperNavigationArea
import tachiyomi.presentation.core.components.PaperPageIndicatorState
import tachiyomi.presentation.core.components.PaperScrollColumn
import tachiyomi.presentation.core.components.paperPopupReady

/** Stationary, paginated menu that stays within the available window. */
@Composable
fun PaperDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    shape: Shape = MaterialTheme.shapes.extraSmall,
    containerColor: Color = Color.White,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = BorderStroke(2.dp, Color.Black),
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val position = remember(offset, density) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset = with(density) {
                val margin = 8.dp.roundToPx()
                IntOffset(
                    (anchorBounds.left + offset.x.roundToPx())
                        .coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)),
                    (anchorBounds.bottom + offset.y.roundToPx())
                        .coerceIn(margin, (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)),
                )
            }
        }
    }
    Popup(position, onDismissRequest, properties) {
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { focus.requestFocus() }
        val indicator = remember { PaperPageIndicatorState(navigationUi = false) }
        CompositionLocalProvider(
            LocalPaperPageIndicator provides indicator,
            LocalPaperNavigationArea provides PaperNavigationArea.Dialogs,
        ) {
            PaperSurface(
                modifier = modifier.paperPopupReady(indicator)
                    .widthIn(min = 112.dp, max = minOf(144.dp, (configuration.screenWidthDp - 24).dp))
                    .heightIn(max = (configuration.screenHeightDp * .7f).dp),
                shape = shape,
                color = Color.White,
                contentColor = Color.Black,
                border = border,
            ) {
                PaperScrollColumn(
                    modifier = Modifier.onPreviewKeyEvent { PaperNavigation.dispatchKeyEvent(it.nativeKeyEvent) }
                        .focusRequester(focus).focusable(),
                    state = scrollState,
                    content = content,
                )
            }
        }
    }
}
