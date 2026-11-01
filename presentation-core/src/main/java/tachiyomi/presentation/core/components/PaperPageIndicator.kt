package tachiyomi.presentation.core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.PaperSurface as Surface

@Stable
class PaperPageIndicatorState(header: Boolean = false) {
    var hasHeader by mutableStateOf(header)
    private val entries = mutableStateMapOf<Any, Entry>()
    private var order = 0L
    private class Entry(label: String, val click: () -> Unit, val order: Long) {
        var label by mutableStateOf(label)
    }
    fun register(owner: Any, label: String, click: () -> Unit) {
        entries[owner] = Entry(label, click, ++order)
    }
    fun update(owner: Any, label: String) {
        entries[owner]?.label = label
    }
    fun remove(owner: Any) {
        entries.remove(owner)
    }

    @Composable
    fun Indicator(modifier: Modifier = Modifier) {
        val entry = entries[PaperNavigation.activeOwner] ?: entries.values.maxByOrNull { it.order } ?: return
        PaperPageNumber(entry.label, entry.click, modifier)
    }
}

val LocalPaperPageIndicator = staticCompositionLocalOf<PaperPageIndicatorState?> { null }

@Composable
fun PaperPageNumber(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier.widthIn(max = 160.dp).clickable(role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Text(
            label,
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
