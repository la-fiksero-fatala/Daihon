package tachiyomi.presentation.core.components.material
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check

@Composable
fun PaperFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) = androidx.compose.material3.FilterChip(
    selected = selected, onClick = onClick, label = label, modifier = modifier, enabled = enabled,
    leadingIcon = if (selected) {
        { Icon(MaterialSymbols.Rounded.Check, null) }
    } else {
        leadingIcon
    },
    trailingIcon = trailingIcon, interactionSource = interactionSource,
    colors = FilterChipDefaults.filterChipColors(
        containerColor = Color.White,
        selectedContainerColor = Color.White,
        labelColor = Color.Black,
        selectedLabelColor = Color.Black,
        iconColor = Color.Black,
        selectedLeadingIconColor = Color.Black,
    ),
    border = BorderStroke(if (selected) 2.dp else 1.dp, Color.Black),
)
