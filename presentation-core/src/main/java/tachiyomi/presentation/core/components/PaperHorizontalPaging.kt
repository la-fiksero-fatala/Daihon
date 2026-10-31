package tachiyomi.presentation.core.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun paperHorizontalPaging(state: PagerState): Modifier {
    val scope = rememberCoroutineScope()
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    return Modifier.pointerInput(state, threshold) {
        var distance = 0f
        detectHorizontalDragGestures(
            onDragStart = { distance = 0f },
            onHorizontalDrag = { change, amount ->
                change.consume()
                distance += amount
            },
            onDragCancel = { distance = 0f },
            onDragEnd = {
                if (abs(distance) >= threshold) {
                    scope.launch {
                        state.scrollToPage(
                            (state.currentPage + if (distance < 0) 1 else -1)
                                .coerceIn(0, (state.pageCount - 1).coerceAtLeast(0)),
                        )
                    }
                }
            },
        )
    }
}
