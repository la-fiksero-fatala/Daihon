package tachiyomi.presentation.core.components

import android.content.Context
import android.view.KeyEvent
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import tachiyomi.presentation.core.components.material.LocalPaperRefresh
import kotlin.math.abs

/** The most recently used visible pane owns page keys; background windows cannot consume them. */
object PaperNavigation {
    private data class Handler(val enabled: () -> Boolean, val navigate: (Int) -> Unit)
    private val handlers = linkedMapOf<Any, Handler>()
    private var active: Any? = null
    private data class WindowBinding(val original: Window.Callback, val callback: Window.Callback, var users: Int)
    private val windows = mutableMapOf<Window, WindowBinding>()

    internal fun bindWindow(window: Window): () -> Unit {
        val existing = windows[window]
        val binding = if (existing != null) {
            existing.apply { users++ }
        } else {
            val original = window.callback
            val callback = object : Window.Callback by original {
                override fun dispatchKeyEvent(event: KeyEvent): Boolean =
                    PaperNavigation.dispatchKeyEvent(event) || original.dispatchKeyEvent(event)
            }
            WindowBinding(original, callback, 1).also {
                windows[window] = it
                window.callback = callback
            }
        }
        return {
            binding.users--
            if (binding.users == 0) {
                if (window.callback === binding.callback) window.callback = binding.original
                windows.remove(window)
            }
        }
    }

    fun activate(owner: Any) {
        active = owner
    }

    internal fun register(owner: Any, enabled: () -> Boolean, navigate: (Int) -> Unit) {
        handlers[owner] = Handler(enabled, navigate)
    }

    internal fun unregister(owner: Any) {
        handlers.remove(owner)
        if (active === owner) active = null
    }

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) return false
        val direction = when (event.keyCode) {
            KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_VOLUME_UP -> -1
            KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN -> 1
            else -> return false
        }
        val handler = handlers[active]?.takeIf { it.enabled() }
            ?: handlers.values.lastOrNull { it.enabled() }
            ?: return false
        if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) handler.navigate(direction)
        return true
    }
}

@Composable
internal fun paperInput(
    owner: Any,
    enabled: Boolean = true,
    onNavigate: (Int) -> Unit,
    atTop: Boolean = false,
): Modifier {
    val view = LocalView.current
    val window = LocalWindowInfo.current
    val dialogWindow = (view.parent as? DialogWindowProvider)?.window
    val inputMethod = remember(view) {
        view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }
    val refresh by rememberUpdatedState(LocalPaperRefresh.current)
    val firstPage by rememberUpdatedState(atTop)
    val navigate by rememberUpdatedState(onNavigate)
    val available by rememberUpdatedState(enabled)
    var visible by remember { mutableStateOf(false) }
    val threshold = with(LocalDensity.current) { 48.dp.toPx() }
    DisposableEffect(owner, view, dialogWindow) {
        val releaseWindow = dialogWindow?.let(PaperNavigation::bindWindow)
        PaperNavigation.register(
            owner,
            enabled = { available && visible && window.isWindowFocused && !inputMethod.isAcceptingText },
            navigate = { navigate(it) },
        )
        onDispose {
            PaperNavigation.unregister(owner)
            releaseWindow?.invoke()
        }
    }
    return Modifier
        .onGloballyPositioned { visible = !it.boundsInWindow().isEmpty }
        .onPreviewKeyEvent { PaperNavigation.dispatchKeyEvent(it.nativeKeyEvent) }
        .pointerInput(owner, threshold) {
            var distance = 0f
            detectVerticalDragGestures(
                onDragStart = {
                    distance = 0f
                    PaperNavigation.activate(owner)
                },
                onVerticalDrag = { change, amount ->
                    change.consume()
                    distance += amount
                },
                onDragCancel = { distance = 0f },
                onDragEnd = {
                    if (available && abs(distance) >= threshold) {
                        if (distance > 0 && firstPage && refresh != null) {
                            refresh?.invoke()
                        } else {
                            navigate(if (distance < 0) 1 else -1)
                        }
                    }
                },
            )
        }
}
