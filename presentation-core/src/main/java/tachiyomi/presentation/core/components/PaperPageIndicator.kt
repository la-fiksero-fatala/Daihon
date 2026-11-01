package tachiyomi.presentation.core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
    Box(modifier.widthIn(max = 112.dp).clickable(role = Role.Button, onClick = onClick).padding(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
