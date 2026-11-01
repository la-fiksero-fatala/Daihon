package eu.kanade.presentation.more.onboarding

import androidx.compose.runtime.Composable
import eu.kanade.presentation.more.settings.PreferenceScreen
import eu.kanade.presentation.more.settings.screen.deviceDisplayGroup
import eu.kanade.presentation.more.settings.screen.displayProfileGroup

internal class DeviceDisplayStep : OnboardingStep {
    override val isComplete = true

    @Composable
    override fun Content() {
        PreferenceScreen(items = listOf(deviceDisplayGroup()))
    }
}

internal class DisplayProfileStep(private val reader: Boolean) : OnboardingStep {
    override val isComplete = true

    @Composable
    override fun Content() {
        PreferenceScreen(items = listOf(displayProfileGroup(reader)))
    }
}
