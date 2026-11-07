package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.screen.browse.ContentLanguageChoices
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal class ContentLanguageStep : OnboardingStep {
    override val isComplete = true

    @Composable
    override fun Content() {
        Column(Modifier.fillMaxSize()) {
            Text(
                stringResource(MR.strings.daihon_content_language),
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            ContentLanguageChoices(Modifier.weight(1f))
        }
    }
}
