package tachiyomi.presentation.core.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun StaticCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    strokeWidth: Dp = 3.dp,
    trackColor: Color = Color.White,
    strokeCap: StrokeCap = StrokeCap.Butt,
) = StaticCircularProgressIndicator({ Float.NaN }, modifier, color, strokeWidth, trackColor, strokeCap)

@Composable
fun StaticCircularProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    strokeWidth: Dp = 3.dp,
    trackColor: Color = Color.White,
    strokeCap: StrokeCap = StrokeCap.Butt,
) {
    val value = progress()
    Canvas(
        modifier.size(40.dp).semantics {
            progressBarRangeInfo = if (value.isNaN()) {
                ProgressBarRangeInfo.Indeterminate
            } else {
                ProgressBarRangeInfo(value.coerceIn(0f, 1f), 0f..1f)
            }
        },
    ) {
        val stroke = strokeWidth.toPx()
        drawCircle(color, radius = ((size.minDimension - stroke) / 2f).coerceAtLeast(0f), style = Stroke(stroke))
        if (value.isNaN()) {
            for (position in listOf(.35f, .5f, .65f)) {
                drawCircle(color, radius = stroke / 2f, center = Offset(size.width * position, size.height / 2f))
            }
        } else {
            drawArc(color, -90f, 360f * value.coerceIn(0f, 1f), true)
        }
    }
}

@Composable
fun StaticLinearProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    trackColor: Color = Color.White,
    strokeCap: StrokeCap = StrokeCap.Butt,
) = StaticLinearProgressIndicator({ Float.NaN }, modifier, color, trackColor, strokeCap)

@Composable
fun StaticLinearProgressIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
    trackColor: Color = Color.White,
    strokeCap: StrokeCap = StrokeCap.Butt,
) {
    val value = progress()
    Canvas(
        modifier.fillMaxWidth().height(4.dp).semantics {
            progressBarRangeInfo = if (value.isNaN()) {
                ProgressBarRangeInfo.Indeterminate
            } else {
                ProgressBarRangeInfo(value.coerceIn(0f, 1f), 0f..1f)
            }
        },
    ) {
        drawRect(trackColor)
        drawRect(color, style = Stroke(1.dp.toPx()))
        drawRect(color, size = Size(size.width * if (value.isNaN()) .25f else value.coerceIn(0f, 1f), size.height))
    }
}
