package eu.kanade.tachiyomi.ui.browse.migration.manga

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.library.LibraryItem
import eu.kanade.tachiyomi.util.chapter.getNextUnread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.core.common.utils.mutate
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.applyFilter
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.isLocal
import kotlin.time.Duration.Companion.seconds

@AssistedInject
class MigrateMangaViewModel(
    @Assisted private val sourceIds: List<Long>,
    @Assisted inLibrary: Boolean,
    sourceManager: SourceManager,
    getFavorites: GetFavorites,
    getLibraryManga: GetLibraryManga,
    preferenceStore: PreferenceStore,
    val libraryPreferences: LibraryPreferences,
    private val getChaptersByMangaId: GetChaptersByMangaId,
    private val downloadManager: DownloadManager,
    downloadCache: DownloadCache,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(sourceIds: List<Long>, inLibrary: Boolean): MigrateMangaViewModel
    }

    private val _events = Channel<MigrationMangaEvent>(Channel.BUFFERED)
    val events: Flow<MigrationMangaEvent> = _events.receiveAsFlow()

    private val source = viewModelScope.async { sourceManager.getOrStub(sourceIds.first()) }
    private val selection = MutableStateFlow(emptySet<Long>())
    private val search = MutableStateFlow<String?>(null)
    private val filters = MutableStateFlow(emptyMap<Filter, TriState>())
    private val sort = preferenceStore.getObjectFromString(
        "daihon_source_library_sort_${sourceIds.distinct().sorted().joinToString("_")}",
        LibrarySort.default,
        LibrarySort.Serializer::serialize,
        LibrarySort.Serializer::deserialize,
    )

    private val favorites = if (inLibrary) {
        val sources = sourceIds.toSet()
        val sourcesById = viewModelScope.async { sources.associateWith { sourceManager.getOrStub(it) } }
        val mangas = getLibraryManga.subscribe()
            .onEmpty { error("Could not load library manga") }
            .map { library -> library.filter { it.manga.source in sources } }
        val badges = combine(
            libraryPreferences.downloadBadge.changes(),
            libraryPreferences.unreadBadge.changes(),
            libraryPreferences.localBadge.changes(),
            libraryPreferences.languageBadge.changes(),
            downloadCache.changes,
        ) { downloaded, unread, local, language, _ ->
            BadgeSettings(downloaded, unread, local, language)
        }
        val items = combine(mangas, badges) { library, badge ->
            val downloadCounts = if (badge.downloaded) {
                downloadManager.getDownloadCounts(library.map { it.manga })
            } else {
                emptyMap()
            }
            val sourceLookup = sourcesById.await()
            library.map { manga ->
                val local = manga.manga.isLocal()
                val source = sourceLookup.getValue(manga.manga.source)
                val downloads = downloadCounts[manga.id] ?: 0
                LibraryItem(
                    libraryManga = manga,
                    downloadCount = downloads,
                    unreadCount = manga.unreadCount,
                    isLocal = local,
                    sourceName = source.name.lowercase(),
                    sourceLanguage = source.lang,
                    badges = LibraryItem.Badges(
                        downloadCount = downloads,
                        unreadCount = if (badge.unread) manga.unreadCount else 0,
                        isLocal = badge.local && local,
                        sourceLanguage = if (badge.language) source.lang else "",
                    ),
                )
            }
        }
        combine(items, search, filters, sort.changes()) { library, query, activeFilters, order ->
            val searchTerm = query.orEmpty().trim()
            val filtered = library.filter { item ->
                val manga = item.libraryManga
                val matchesSearch = searchTerm.isEmpty() ||
                    manga.manga.title.contains(searchTerm, ignoreCase = true) ||
                    manga.manga.author?.contains(searchTerm, ignoreCase = true) == true ||
                    manga.manga.artist?.contains(searchTerm, ignoreCase = true) == true
                matchesSearch && activeFilters.all { (filter, state) ->
                    applyFilter(state) {
                        when (filter) {
                            Filter.Unread -> manga.unreadCount > 0
                            Filter.Started -> manga.hasStarted
                            Filter.Bookmarked -> manga.hasBookmarks
                            Filter.Completed -> manga.manga.status.toInt() == SManga.COMPLETED
                        }
                    }
                }
            }.sortedWith(libraryComparator(order))
            State(
                source = source.await(),
                titleList = filtered.map { it.libraryManga.manga },
                libraryItems = filtered,
                search = query,
                filters = activeFilters,
                sort = order,
            )
        }
    } else {
        combine(sourceIds.distinct().map(getFavorites::subscribe)) { lists ->
            State(
                source = source.await(),
                titleList = lists.flatMap { it }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, Manga::title)),
            )
        }
    }.catch {
        logcat(LogPriority.ERROR, it)
        _events.trySend(MigrationMangaEvent.FailedFetchingFavorites)
        emit(State(source = source.await(), titleList = emptyList()))
    }

    val state: StateFlow<State> = combine(favorites, selection) { state, selected ->
        if (selected.isEmpty()) {
            state
        } else {
            state.copy(selection = state.titles.asSequence().map(Manga::id).filter(selected::contains).toSet())
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), State())

    fun toggleSelection(item: Manga) {
        selection.update { selection ->
            selection.mutate { list ->
                if (!list.remove(item.id)) list.add(item.id)
            }
        }
    }

    fun selectAll() {
        val ids = state.value.titles.mapTo(mutableSetOf(), Manga::id)
        selection.update { if (it.containsAll(ids)) emptySet() else ids }
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun setSearch(query: String?) {
        search.value = query
    }

    fun setFilter(filter: Filter, state: TriState) {
        filters.update { if (state == TriState.DISABLED) it - filter else it + (filter to state) }
    }

    fun setSort(type: LibrarySort.Type) {
        val current = sort.get()
        val direction = if (current.type == type && current.isAscending) {
            LibrarySort.Direction.Descending
        } else {
            LibrarySort.Direction.Ascending
        }
        sort.set(LibrarySort(type, direction))
    }

    suspend fun getNextUnreadChapter(manga: Manga) = withContext(Dispatchers.Default) {
        getChaptersByMangaId.await(manga.id, applyScanlatorFilter = true).getNextUnread(manga, downloadManager)
    }

    @Immutable
    data class State(
        val source: Source? = null,
        val selection: Set<Long> = emptySet(),
        private val titleList: List<Manga>? = null,
        val libraryItems: List<LibraryItem> = emptyList(),
        val search: String? = null,
        val filters: Map<Filter, TriState> = emptyMap(),
        val sort: LibrarySort = LibrarySort.default,
    ) {

        val titles: List<Manga>
            get() = titleList.orEmpty()

        val isLoading: Boolean
            get() = titleList == null

        val isEmpty: Boolean
            get() = titles.isEmpty()

        val selectionMode = selection.isNotEmpty()
        val allSelected = selectionMode && selection.size == titles.size
    }

    enum class Filter {
        Unread,
        Started,
        Bookmarked,
        Completed,
    }

    private data class BadgeSettings(
        val downloaded: Boolean,
        val unread: Boolean,
        val local: Boolean,
        val language: Boolean,
    )

    private fun libraryComparator(sort: LibrarySort): Comparator<LibraryItem> {
        val alphabetic = compareBy<LibraryItem, String>(String.CASE_INSENSITIVE_ORDER) { it.libraryManga.manga.title }
        val byType = when (sort.type) {
            LibrarySort.Type.LastRead -> compareBy { it.libraryManga.lastRead }
            LibrarySort.Type.LastUpdate -> compareBy { it.libraryManga.manga.lastUpdate }
            LibrarySort.Type.UnreadCount -> compareBy { it.unreadCount }
            LibrarySort.Type.TotalChapters -> compareBy { it.libraryManga.totalChapters }
            LibrarySort.Type.LatestChapter -> compareBy { it.libraryManga.latestUpload }
            LibrarySort.Type.ChapterFetchDate -> compareBy { it.libraryManga.chapterFetchedAt }
            LibrarySort.Type.DateAdded -> compareBy { it.libraryManga.manga.favoriteAt }
            else -> alphabetic
        }
        return (if (sort.isAscending) byType else byType.reversed()).then(alphabetic).thenBy { it.id }
    }
}

sealed interface MigrationMangaEvent {
    data object FailedFetchingFavorites : MigrationMangaEvent
}
