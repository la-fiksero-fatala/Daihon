package tachiyomi.presentation.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.zIndex
import tachiyomi.presentation.core.components.material.DialogCloseButton
import tachiyomi.presentation.core.components.material.PaperSurface as Surface

/** A stationary, outlined dialog; no draggable sheet or dimming transition. */
@Composable
fun AdaptiveSheet(
    isTabletUi: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    enableImplicitDismiss: Boolean = true,
    showCloseButton: Boolean = true,
    title: String? = null,
    content: @Composable () -> Unit,
) {
    val indicator = remember { PaperPageIndicatorState(showCloseButton) }
    val configuration = LocalConfiguration.current
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = enableImplicitDismiss,
            dismissOnClickOutside = enableImplicitDismiss,
            usePlatformDefaultWidth = false,
        ),
    ) {
        CompositionLocalProvider(
            LocalPaperNavigationArea provides PaperNavigationArea.Dialogs,
            LocalPaperPageIndicator provides indicator,
        ) {
            val window = (LocalView.current.parent as? DialogWindowProvider)?.window
            SideEffect {
                window?.setWindowAnimations(0)
                window?.setDimAmount(0f)
            }
            Box(Modifier.fillMaxSize()) {
                if (enableImplicitDismiss) {
                    Box(
                        Modifier.matchParentSize().pointerInput(onDismissRequest) {
                            detectTapGestures { onDismissRequest() }
                        },
                    )
                }
                Box(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp), contentAlignment = Alignment.Center) {
                    Surface(
                        modifier = modifier.paperPopupReady(indicator).widthIn(max = 560.dp).heightIn(
                            max = (configuration.screenHeightDp * .9f).dp,
                        ),
                        shape = MaterialTheme.shapes.small,
                        color = Color.White,
                        contentColor = Color.Black,
                        border = BorderStroke(2.dp, Color.Black),
                    ) {
                        if (showCloseButton) {
                            Column {
                                Row(
                                    Modifier.fillMaxWidth().zIndex(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.weight(1f).padding(start = 24.dp)) {
                                        if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
                                    }
                                    indicator.Indicator()
                                    DialogCloseButton(onDismissRequest, Modifier.padding(end = 4.dp))
                                }
                                Box(Modifier.weight(1f, fill = false).clipToBounds()) { content() }
                            }
                        } else {
                            content()
                        }
                    }
                }
            }
        }
    }
}
