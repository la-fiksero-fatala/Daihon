package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Check

@Composable
fun IconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    imageVector: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
) {
    FilledIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = Color.White,
            contentColor = Color.Black,
            checkedContainerColor = Color.White,
            checkedContentColor = Color.Black,
        ),
        modifier = modifier
            .height(48.dp).border(if (checked) 2.dp else 1.dp, Color.Black),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.small),
        ) {
            if (checked) Icon(MaterialSymbols.Rounded.Check, null)
            Icon(
                imageVector = imageVector,
                contentDescription = null,
            )

            Text(
                text = title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
