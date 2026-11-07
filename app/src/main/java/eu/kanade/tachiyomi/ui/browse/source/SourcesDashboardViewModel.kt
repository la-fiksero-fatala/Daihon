package eu.kanade.tachiyomi.ui.browse.source

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.domain.source.interactor.ToggleSourcePin
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.repository.SourceRepository
import tachiyomi.source.local.isLocal

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class SourcesDashboardViewModel(
    repository: SourceRepository,
    extensionManager: ExtensionManager,
    val preferences: SourcePreferences,
    private val toggleSourcePin: ToggleSourcePin,
) : ViewModel() {

    val tiles = combine(
        repository.getSources(),
        extensionManager.loadedExtensionsFlow,
        preferences.languageSelectionChanges(),
        preferences.pinnedSources.changes(),
    ) { sources, extensions, languages, pins ->
        val byId = sources.associateBy(Source::id)
        val extensionIds = extensions.flatMap { extension -> extension.sources.map { it.id } }.toSet()
        val installed = extensions.mapNotNull { extension ->
            val ids = extension.sources.map { it.id }.distinct()
            if (ids.isEmpty()) return@mapNotNull null
            val enabled = ids.mapNotNull(byId::get).filter { languages.isEnabled(it.id, it.lang) }
            Tile(extension.pkgName, extension.name, extension, ids, enabled, ids.any { "$it" in pins })
        }
        val standalone = sources.filter { it.id !in extensionIds }.map { source ->
            val enabled = listOf(source).filter { it.isLocal() || languages.isEnabled(it.id, it.lang) }
            Tile("source-${source.id}", source.name, null, listOf(source.id), enabled, "${source.id}" in pins, source)
        }
        (installed + standalone).sortedWith(
            compareByDescending<Tile> { it.extension?.hasUpdate == true }
                .thenByDescending { it.pinned }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun togglePin(source: Source) = toggleSourcePin.await(source)

    data class Tile(
        val key: String,
        val name: String,
        val extension: Extension.Loaded?,
        val sourceIds: List<Long>,
        val enabledSources: List<Source>,
        val pinned: Boolean,
        val standalone: Source? = null,
    )
}
