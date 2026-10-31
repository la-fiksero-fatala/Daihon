package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryDetailMode
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.PaperLazyColumn
import tachiyomi.presentation.core.components.PaperScrollColumn
import tachiyomi.presentation.core.components.material.PaperAlertDialog
import tachiyomi.presentation.core.components.material.PaperButton
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

class OpenSourceLicensesScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.licenses),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            val libraries by produceLibraries(R.raw.aboutlibraries)
            var selected by remember { mutableStateOf<Library?>(null) }
            PaperLazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
                items(libraries?.libraries.orEmpty(), key = { it.uniqueId }) { library ->
                    PaperButton(onClick = { selected = library }, modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text("${library.name}\n${library.artifactVersion.orEmpty()}")
                    }
                }
            }
            selected?.let { library ->
                val uriHandler = LocalUriHandler.current
                PaperAlertDialog(
                    onDismissRequest = { selected = null },
                    title = { Text(library.name) },
                    text = {
                        PaperScrollColumn(Modifier.heightIn(max = 400.dp)) {
                            Text(library.artifactId)
                            Text(library.developers.joinToString { it.name.orEmpty() })
                            library.description?.let { Text(it) }
                            library.website?.let { url ->
                                PaperButton(onClick = { uriHandler.openUri(url) }) { Text(url) }
                            }
                            library.licenses.forEach { license ->
                                HorizontalDivider()
                                Text(license.name)
                                license.licenseContent?.let {
                                    Text(HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_COMPACT).toString())
                                }
                                license.url?.let { url ->
                                    PaperButton(onClick = { uriHandler.openUri(url) }) { Text(url) }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        PaperButton(onClick = { selected = null }) { Text(stringResource(MR.strings.action_close)) }
                    },
                )
            }
        }
    }
}
