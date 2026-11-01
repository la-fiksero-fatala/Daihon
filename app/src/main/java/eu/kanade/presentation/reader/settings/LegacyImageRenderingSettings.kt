package eu.kanade.presentation.reader.settings

import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.TabbedDialogPaddings
import eu.kanade.presentation.more.settings.widget.PrefsHorizontalPadding
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.ui.reader.setting.LegacyImageFilter
import eu.kanade.tachiyomi.ui.reader.setting.LegacyUpscaler
import mihon.app.di.appGraph
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.RadioItem
import tachiyomi.presentation.core.components.material.PaperAlertDialog
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

@Composable
internal fun ColumnScope.LegacyImageRenderingSettings() {
    val context = LocalContext.current
    val preferences = remember { context.appGraph.readerPreferences }
    val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    RenderingChoice(
        preferences.legacyUpscaler,
        MR.strings.reader_upscaling_algorithm,
        LegacyUpscaler.entries.associateWith { it.title },
        supported,
        LegacyUpscaler.System,
    )
    RenderingChoice(
        preferences.legacyImageFilter,
        MR.strings.reader_image_filter,
        LegacyImageFilter.entries.associateWith { it.title },
        supported,
        LegacyImageFilter.None,
    )
    Text(
        stringResource(
            if (supported) MR.strings.reader_legacy_sampling_help else MR.strings.reader_legacy_requires_android_13,
        ),
        modifier = Modifier.padding(
            horizontal = PrefsHorizontalPadding,
            vertical = TabbedDialogPaddings.Vertical,
        ),
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
private fun <T> RenderingChoice(
    preference: Preference<T>,
    title: StringResource,
    entries: Map<T, StringResource>,
    enabled: Boolean,
    fallback: T,
) {
    val selected by preference.collectAsState()
    var open by remember { mutableStateOf(false) }
    TextPreferenceWidget(
        title = stringResource(title),
        subtitle = entries[if (enabled) selected else fallback]?.let { stringResource(it) },
        enabled = enabled,
        onPreferenceClick = { open = true },
    )
    if (open && enabled) {
        PaperAlertDialog(
            onDismissRequest = { open = false },
            title = { Text(stringResource(title)) },
            text = {
                Column {
                    entries.forEach { (value, label) ->
                        RadioItem(selected = selected == value, onClick = {
                            preference.set(value)
                            open = false
                        }, label = stringResource(label))
                    }
                }
            },
            confirmButton = {},
        )
    }
}
