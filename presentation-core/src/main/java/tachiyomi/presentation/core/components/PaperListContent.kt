package tachiyomi.presentation.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Preserve callers' keys and item scopes while making section headers sticky in paged lists. */
internal class PaperListContent : LazyListScope {
    private val emitters = mutableListOf<LazyListScope.() -> Unit>()
    val headers = mutableListOf<Int>()
    private var count = 0

    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
        if (contentType == "header") {
            stickyHeader(key, contentType) { content() }
        } else {
            emitters.add { item(key, contentType, content) }
            count++
        }
    }

    override fun items(
        count: Int,
        key: ((Int) -> Any)?,
        contentType: (Int) -> Any?,
        itemContent: @Composable LazyItemScope.(Int) -> Unit,
    ) {
        // Most callers already supply grouped intervals; keep their lazy ranges intact.
        var start = 0
        fun emitRange(end: Int) {
            val from = start
            if (from >= end) return
            emitters.add {
                items(end - from, key?.let { original -> { original(it + from) } }, { contentType(it + from) }) {
                    itemContent(it + from)
                }
            }
            this.count += end - from
        }
        for (index in 0 until count) {
            if (contentType(index) == "header") {
                emitRange(index)
                stickyHeader(key?.invoke(index), "header") { itemContent(index) }
                start = index + 1
            }
        }
        emitRange(count)
    }

    override fun stickyHeader(key: Any?, contentType: Any?, content: @Composable LazyItemScope.(Int) -> Unit) {
        val index = count++
        headers.add(index)
        emitters.add {
            stickyHeader(key, "header") {
                val itemScope = this
                Column(Modifier.fillMaxWidth().background(Color.White)) { content(itemScope, index) }
            }
        }
    }

    fun emit(scope: LazyListScope) {
        emitters.forEach { it(scope) }
    }
}
