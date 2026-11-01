package tachiyomi.presentation.core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.material.PaperSurface as Surface

internal data class PaperIndicatorToken(val owner: Any, val label: String)

@Stable
class PaperPageIndicatorState(header: Boolean = false) {
    var hasHeader by mutableStateOf(header)
    private data class Content(val label: String, val paged: Boolean, val ready: Boolean, val visible: Boolean)
    private class Entry(content: Content, val click: () -> Unit, val order: Long) {
        var content by mutableStateOf(content)
    }
    private val entries = mutableStateMapOf<Any, Entry>()
    private var order = 0L
    private var rendered by mutableStateOf<PaperIndicatorToken?>(null)
    private var revealed = false

    fun register(owner: Any, label: String, paged: Boolean, ready: Boolean, visible: Boolean, click: () -> Unit) {
        entries[owner] = Entry(Content(label, paged, ready, visible), click, ++order)
    }
    fun update(owner: Any, label: String, paged: Boolean, ready: Boolean, visible: Boolean) {
        entries[owner]?.content = Content(label, paged, ready, visible)
    }
    fun remove(owner: Any) {
        entries.remove(owner)
    }

    internal val headerToken: PaperIndicatorToken?
        get() {
            val owner = PaperNavigation.activeOwner?.takeIf { entries[it]?.content?.visible == true }
                ?: entries.filterValues { it.content.visible }.maxByOrNull { it.value.order }?.key
                ?: return null
            val content = entries[owner]?.content ?: return null
            return if (content.ready && content.paged) PaperIndicatorToken(owner, content.label) else null
        }

    internal fun headerComposed(token: PaperIndicatorToken?) {
        rendered = token
    }

    internal val readyToDraw: Boolean
        get() = entries.values.all { it.content.ready } && (!hasHeader || rendered == headerToken)

    internal fun revealIfReady(): Boolean {
        if (!revealed && readyToDraw) revealed = true
        return revealed
    }

    @Composable
    fun Indicator(modifier: Modifier = Modifier) {
        val token = headerToken
        SideEffect { headerComposed(token) }
        if (token != null) entries[token.owner]?.let { PaperPageNumber(token.label, it.click, modifier) }
    }
}

/** Measure popup content and compose its counter before the first visible frame; no timed delay. */
internal fun Modifier.paperPopupReady(indicator: PaperPageIndicatorState): Modifier = drawWithContent {
    if (indicator.revealIfReady()) drawContent()
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
