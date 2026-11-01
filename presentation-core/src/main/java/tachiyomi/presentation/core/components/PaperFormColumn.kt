package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth

/** Unbounded form column, exposing root-control bounds for complete-item page breaks. */
@Composable
internal fun PaperFormColumn(
    modifier: Modifier,
    tail: Int,
    arrangement: Arrangement.Vertical,
    alignment: Alignment.Horizontal,
    onMeasured: (List<PaperSpan>) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column {
        val scope = this
        Layout(content = { scope.content() }, modifier = modifier) { measurables, constraints ->
            val children = measurables.map {
                it.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
            }
            val spacing = arrangement.spacing.roundToPx()
            val naturalHeight = children.sumOf { it.height } + spacing * (children.size - 1).coerceAtLeast(0)
            val contentHeight = constraints.constrainHeight(naturalHeight)
            val positions = IntArray(children.size)
            with(arrangement) { arrange(contentHeight, children.map { it.height }.toIntArray(), positions) }
            onMeasured(
                children.mapIndexed { index, child ->
                    PaperSpan(positions[index], positions[index] + child.height)
                },
            )
            val width = if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                constraints.constrainWidth(children.maxOfOrNull { it.width } ?: 0)
            }
            layout(width, contentHeight + tail) {
                children.forEachIndexed { index, child ->
                    child.place(alignment.align(child.width, width, layoutDirection), positions[index])
                }
            }
        }
    }
}
