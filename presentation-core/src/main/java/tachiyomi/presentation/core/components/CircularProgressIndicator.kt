package tachiyomi.presentation.core.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CombinedCircularProgressIndicator(progress: () -> Float, modifier: Modifier = Modifier) {
    StaticCircularProgressIndicator(
        progress = { progress().takeIf { it > 0f } ?: Float.NaN },
        modifier = modifier,
    )
}
