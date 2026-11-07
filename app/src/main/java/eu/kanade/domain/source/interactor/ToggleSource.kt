package eu.kanade.domain.source.interactor

import dev.zacsweers.metro.Inject
import eu.kanade.domain.source.service.SourcePreferences
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.source.model.Source

@Inject
class ToggleSource(
    private val preferences: SourcePreferences,
) {

    fun await(source: Source, enable: Boolean = !preferences.languageSelection().isEnabled(source.id, source.lang)) {
        await(source.id, enable)
    }

    fun await(sourceId: Long, enable: Boolean) = await(listOf(sourceId), enable)

    /** Null restores the global language defaults for these sources. */
    fun await(sourceIds: List<Long>, enable: Boolean?) {
        val ids = sourceIds.map(Long::toString)
        val explicitEnables = ids.map { "+$it" }
        preferences.disabledSources.getAndSet { overrides ->
            val cleared = overrides - ids - explicitEnables
            when (enable) {
                true -> cleared + explicitEnables
                false -> cleared + ids
                null -> cleared
            }
        }
    }
}
