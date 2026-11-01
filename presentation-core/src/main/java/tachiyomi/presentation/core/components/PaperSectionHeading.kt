package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.roundToInt

internal val LocalPaperSections = staticCompositionLocalOf<PaperSections?> { null }

internal class PaperHeading(val content: @Composable () -> Unit) {
    var top by mutableIntStateOf(Int.MAX_VALUE)
    var height by mutableIntStateOf(0)
    var coordinates: LayoutCoordinates? = null
}

internal class PaperSections(val scrollOffset: () -> Int) {
    val headings = mutableStateMapOf<Any, PaperHeading>()
    var viewport: LayoutCoordinates? = null

    fun position(heading: PaperHeading) {
        val root = viewport?.takeIf { it.isAttached } ?: return
        val child = heading.coordinates?.takeIf { it.isAttached } ?: return
        heading.top = root.localPositionOf(child, Offset.Zero).y.roundToInt() + scrollOffset()
        heading.height = child.size.height
    }
}

/** The same heading is reused above a text/form page after its original has passed the top. */
@Composable
fun PaperSectionHeading(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val sections = LocalPaperSections.current
    if (sections == null) {
        Box(modifier) { content() }
        return
    }
    val currentContent by rememberUpdatedState(content)
    val heading = remember { PaperHeading { currentContent() } }
    DisposableEffect(sections, heading) {
        sections.headings[heading] = heading
        onDispose { sections.headings.remove(heading) }
    }
    Box(
        modifier.onGloballyPositioned {
            heading.coordinates = it
            sections.position(heading)
        },
    ) { content() }
}
