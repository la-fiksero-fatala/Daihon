package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun DialogCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    PaperIconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(MaterialSymbols.Rounded.Close, stringResource(MR.strings.action_close))
    }
}
