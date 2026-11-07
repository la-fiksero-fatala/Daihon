package eu.kanade.presentation.browse

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.extension.interactor.ExtensionSourceItem
import eu.kanade.presentation.browse.components.ExtensionIcon
import eu.kanade.presentation.browse.components.ExtensionPill
import eu.kanade.presentation.browse.components.label
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TrailingWidgetBuffer
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsViewModel
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.copyToClipboard
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Eyeglasses2
import mihon.icons.materialsymbols.rounded.Info
import mihon.icons.materialsymbols.rounded.Keep
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.roundedfilled.Keep
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.components.material.PaperAlertDialog as AlertDialog
import tachiyomi.presentation.core.components.material.PaperButton as Button
import tachiyomi.presentation.core.components.material.PaperButton as TextButton
import tachiyomi.presentation.core.components.material.PaperIconButton as IconButton

@Composable
fun ExtensionDetailsScreen(
    navigateUp: () -> Unit,
    state: ExtensionDetailsViewModel.State.Success,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickEnableAll: () -> Unit,
    onClickDisableAll: () -> Unit,
    onClickGlobalLanguages: () -> Unit,
    onClickPin: () -> Unit,
    onClickClearCookies: () -> Unit,
    onClickUninstall: () -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    onClickIncognito: (Boolean) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = state.extension.name,
                navigateUp = navigateUp,
                actions = {
                    AppBarActions(
                        actions = buildList {
                            if (state.sources.isNotEmpty()) {
                                add(
                                    AppBar.Action(
                                        title = stringResource(
                                            if (state.pinned) {
                                                MR.strings.daihon_unpin_extension
                                            } else {
                                                MR.strings.daihon_pin_extension
                                            },
                                        ),
                                        icon = if (state.pinned) {
                                            MaterialSymbols.RoundedFilled.Keep
                                        } else {
                                            MaterialSymbols.Rounded.Keep
                                        },
                                        onClick = onClickPin,
                                    ),
                                )
                            }
                            add(
                                AppBar.OverflowAction(
                                    title = stringResource(MR.strings.pref_clear_cookies),
                                    onClick = onClickClearCookies,
                                ),
                            )
                        },
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        ExtensionDetails(
            contentPadding = paddingValues,
            extension = state.extension,
            onClickStore = state.extension.store
                ?.contact
                ?.website
                ?.takeIf { it.isNotBlank() }
                ?.let { website -> { uriHandler.openUri(website) } },
            sources = state.sources,
            onClickToggleAllLanguages = if (state.sources.all { it.enabled }) onClickDisableAll else onClickEnableAll,
            onClickGlobalLanguages = onClickGlobalLanguages,
            incognitoMode = state.isIncognito,
            onClickSourcePreferences = onClickSourcePreferences,
            onClickUninstall = onClickUninstall,
            onClickSource = onClickSource,
            onClickIncognito = onClickIncognito,
        )
    }
}

@Composable
private fun ExtensionDetails(
    contentPadding: PaddingValues,
    extension: Extension.Loaded,
    onClickStore: (() -> Unit)?,
    sources: List<ExtensionSourceItem>,
    onClickToggleAllLanguages: () -> Unit,
    onClickGlobalLanguages: () -> Unit,
    incognitoMode: Boolean,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickUninstall: () -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    onClickIncognito: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var showContentWarning by remember { mutableStateOf(false) }
    val contentWarning = extension.contentWarning.label

    ScrollbarLazyColumn(
        contentPadding = contentPadding,
    ) {
        if (extension.isObsolete) {
            item {
                WarningBanner(MR.strings.obsolete_extension_message)
            }
        }

        item {
            DetailsHeader(
                extension = extension,
                extIncognitoMode = incognitoMode,
                onClickUninstall = onClickUninstall,
                onClickAppInfo = {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", extension.pkgName, null)
                        context.startActivity(this)
                    }
                    Unit
                }.takeIf { extension.isShared },
                onClickStore = onClickStore,
                onClickContentWarning = {
                    showContentWarning = true
                },
                onExtIncognitoChange = onClickIncognito,
            )
        }

        if (sources.isNotEmpty()) {
            item {
                FlowRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Button(onClick = onClickToggleAllLanguages) {
                        Text(
                            stringResource(
                                if (sources.all { it.enabled }) {
                                    MR.strings.daihon_disable_all_languages
                                } else {
                                    MR.strings.daihon_enable_all_languages
                                },
                            ),
                        )
                    }
                    if (sources.any { it.enabled != it.enabledByDefault }) {
                        Button(onClick = onClickGlobalLanguages) {
                            Text(stringResource(MR.strings.daihon_use_global_languages))
                        }
                    }
                }
            }
        }

        items(
            items = sources,
            key = { it.source.id },
        ) { source ->
            SourceSwitchPreference(
                modifier = Modifier.animateItem(),
                source = source,
                onClickSourcePreferences = onClickSourcePreferences,
                onClickSource = onClickSource,
            )
        }
    }
    if (showContentWarning && contentWarning != null) {
        ContentWarningDialog(
            label = contentWarning.title,
            description = contentWarning.description,
            onClickConfirm = {
                showContentWarning = false
            },
        )
    }
}

@Composable
private fun DetailsHeader(
    extension: Extension.Loaded,
    extIncognitoMode: Boolean,
    onClickStore: (() -> Unit)?,
    onClickContentWarning: () -> Unit,
    onClickUninstall: () -> Unit,
    onClickAppInfo: (() -> Unit)?,
    onExtIncognitoChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val contentWarning = extension.contentWarning.label
    val incognitoSummary = stringResource(MR.strings.pref_incognito_mode_extension_summary)

    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        val sideBySide = maxWidth >= 420.dp * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val actionsWidth = if (sideBySide) (maxWidth * 0.5f).coerceAtMost(256.dp) else maxWidth
        val identityWidth = if (sideBySide) maxWidth - actionsWidth - 12.dp else maxWidth
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.width(identityWidth).clip(MaterialTheme.shapes.small).clickable {
                    val debugInfo = """
                        Extension name: ${extension.name} (lang: ${extension.lang}; package: ${extension.pkgName})
                        Extension version: ${extension.versionName} (lib: ${extension.libVersion}; version code: ${extension.versionCode})
                        Content warning: ${extension.contentWarning}

                        Update available: ${extension.hasUpdate}
                        Orphaned: ${extension.isObsolete}
                        Shared: ${extension.isShared}
                    """.trimIndent()
                    val repository = extension.store?.let { "\nRepository: ${it.indexUrl}" }.orEmpty()
                    context.copyToClipboard("Extension Debug information", debugInfo + repository)
                }.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExtensionIcon(extension, Modifier.size(56.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(extension.versionName, style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = extension.pkgName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    extension.store?.let { store ->
                        Text(
                            text = store.name,
                            modifier = Modifier.clickable(enabled = onClickStore != null) { onClickStore?.invoke() },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (!extension.isShared) {
                        ExtensionPill(text = stringResource(MR.strings.ext_installer_private))
                    }
                    if (contentWarning != null) {
                        Row(
                            modifier = Modifier.clickable(onClick = onClickContentWarning),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(contentWarning.title),
                                style = MaterialTheme.typography.labelMedium,
                                color = contentWarning.color,
                            )
                            Icon(
                                MaterialSymbols.Rounded.Info,
                                stringResource(MR.strings.ext_info_warning),
                                Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.width(actionsWidth),
                horizontalAlignment = if (sideBySide) Alignment.End else Alignment.Start,
            ) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onClickUninstall, contentPadding = PaddingValues(horizontal = 12.dp)) {
                        Text(stringResource(MR.strings.ext_uninstall))
                    }
                    if (onClickAppInfo != null) {
                        Button(onClick = onClickAppInfo, contentPadding = PaddingValues(horizontal = 12.dp)) {
                            Text(stringResource(MR.strings.ext_app_info))
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .toggleable(extIncognitoMode, role = Role.Switch, onValueChange = onExtIncognitoChange)
                        .semantics { contentDescription = incognitoSummary },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(MaterialSymbols.Rounded.Eyeglasses2, null, Modifier.size(20.dp))
                    Text(
                        stringResource(MR.strings.pref_incognito_mode),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(checked = extIncognitoMode, onCheckedChange = null)
                }
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun SourceSwitchPreference(
    source: ExtensionSourceItem,
    onClickSourcePreferences: (sourceId: Long) -> Unit,
    onClickSource: (sourceId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    TextPreferenceWidget(
        modifier = modifier,
        title = if (source.labelAsName) {
            source.source.toString()
        } else {
            LocaleHelper.getSourceDisplayName(source.source.lang, context)
        },
        widget = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (source.source is ConfigurableSource) {
                    IconButton(onClick = { onClickSourcePreferences(source.source.id) }) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Settings,
                            contentDescription = stringResource(MR.strings.label_settings),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Switch(
                    checked = source.enabled,
                    onCheckedChange = null,
                    modifier = Modifier.padding(start = TrailingWidgetBuffer),
                )
            }
        },
        onPreferenceClick = { onClickSource(source.source.id) },
    )
}

@Composable
private fun ContentWarningDialog(
    label: StringResource,
    description: StringResource,
    onClickConfirm: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(text = stringResource(description))
        },
        confirmButton = {
            TextButton(onClick = onClickConfirm) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        onDismissRequest = onClickConfirm,
    )
}
