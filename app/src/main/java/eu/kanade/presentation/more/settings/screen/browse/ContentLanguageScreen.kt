package eu.kanade.presentation.more.settings.screen.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.source.service.SourceLanguageSelection
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.app.di.appGraph
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.components.PaperLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import java.util.Locale

class ContentLanguageScreen : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.daihon_content_language),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { padding -> ContentLanguageChoices(Modifier.padding(padding).consumeWindowInsets(padding)) }
    }
}

@Composable
internal fun ContentLanguageChoices(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val graph = remember(context) { context.appGraph }
    val preference = graph.sourcePreferences.enabledLanguages
    val selected by preference.collectAsState()
    val installed by graph.extensionManager.loadedExtensionsFlow.collectAsStateWithLifecycle(emptyList())
    val available by graph.extensionManager.availableExtensionsFlow.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    val appLanguage = LocaleHelper.getDefaultEnabledLanguages().first()
    val initialChoices = remember(preference) { preference.get() }
    val languages = remember(appLanguage, initialChoices, installed, available) {
        (
            Locale.getISOLanguages().toSet() + initialChoices + appLanguage +
                installed.flatMap { extension -> extension.sources.map { it.lang } } +
                available.flatMap { extension -> extension.sources.map { it.lang } }
            )
            .filterNot { SourceLanguageSelection.isUnclassified(it) }
            .sortedWith(compareBy<String> { it != appLanguage }.then(LocaleHelper.comparator))
    }

    Column(modifier) {
        Text(
            stringResource(MR.strings.daihon_content_language_help),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            label = { Text(stringResource(MR.strings.action_search)) },
            singleLine = true,
        )
        PaperLazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            items(
                languages.filter {
                    it.contains(query.trim(), ignoreCase = true) ||
                        LocaleHelper.getLocalizedDisplayName(it).contains(query.trim(), ignoreCase = true) ||
                        LocaleHelper.getDisplayName(it).contains(query.trim(), ignoreCase = true)
                },
                key = { it },
            ) { language ->
                LabeledCheckbox(
                    label = LocaleHelper.getLocalizedDisplayName(language),
                    checked = language in selected,
                    onCheckedChange = { enabled ->
                        preference.getAndSet { if (enabled) it + language else it - language }
                    },
                )
            }
        }
    }
}
