package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import dev.zacsweers.metro.Inject
import eu.kanade.domain.chapter.model.toSChapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.util.image.ImageDimensions
import eu.kanade.tachiyomi.util.image.readImageDimensions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.data.cache.ChapterCache
import okio.buffer
import okio.source
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import java.io.IOException
import kotlin.coroutines.coroutineContext

internal data class ChapterResolutionProgress(
    val checked: Int = 0,
    val total: Int = 0,
    val failed: Int = 0,
    val median: ImageDimensions? = null,
    val complete: Boolean = false,
)

@Inject
class ChapterResolutionChecker(
    private val context: Context,
    private val downloads: DownloadManager,
    private val downloadProvider: DownloadProvider,
    private val cache: ChapterCache,
) {
    internal suspend fun check(
        manga: Manga,
        chapter: Chapter,
        source: Source,
        onProgress: (ChapterResolutionProgress) -> Unit,
    ) =
        withContext(Dispatchers.IO) {
            val dimensions = mutableListOf<ImageDimensions>()
            var failed = 0
            suspend fun probe(pages: List<Page>, read: suspend (Page) -> ImageDimensions?) {
                if (pages.isEmpty()) throw IOException("No pages available")
                onProgress(ChapterResolutionProgress(total = pages.size))
                pages.forEachIndexed { index, page ->
                    coroutineContext.ensureActive()
                    val result = try {
                        read(page)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        null
                    }
                    if (result != null) dimensions += result else failed++
                    onProgress(
                        ChapterResolutionProgress(index + 1, pages.size, failed, ImageDimensions.median(dimensions)),
                    )
                }
                onProgress(
                    ChapterResolutionProgress(pages.size, pages.size, failed, ImageDimensions.median(dimensions), true),
                )
            }
            if (source is HttpSource &&
                !downloads.isChapterDownloadedOnDisk(chapter.name, chapter.scanlator, chapter.url, manga.title, source)
            ) {
                val pages = cache.getPageList(chapter) ?: source.getPageList(chapter.toSChapter())
                probe(pages) { page -> remote(source, page) }
            } else {
                coroutineScope {
                    val readerChapter = ReaderChapter(chapter)
                    val loader =
                        ChapterLoader(context, downloads, downloadProvider, cache, this, { false }, manga, source)
                    try {
                        loader.loadChapter(readerChapter)
                        val pages = readerChapter.pages.orEmpty()
                        probe(pages) { page ->
                            val readerPage = pages.first { it.index == page.index }
                            readerChapter.pageLoader?.loadPage(readerPage)
                            readerPage.stream?.invoke()?.source()?.buffer()?.use(::readImageDimensions)
                        }
                    } finally {
                        readerChapter.pageLoader?.recycle()
                    }
                }
            }
        }

    private suspend fun remote(source: HttpSource, page: Page): ImageDimensions? {
        if (page.imageUrl.isNullOrEmpty()) page.imageUrl = source.getImageUrl(page)
        cache.openImage(page.imageUrl!!)?.source()?.buffer()?.use { cached ->
            readImageDimensions(cached)?.let { return it }
        }
        // Some servers reject Range, or keep dimensions after long JPEG metadata: retry the original request.
        for (range in listOf(true, false)) {
            coroutineContext.ensureActive()
            val call = source.newImageProbeCall(page, range)
            var response: okhttp3.Response? = null
            coroutineScope {
                val cancellation = launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
                    try {
                        awaitCancellation()
                    } finally {
                        call.cancel()
                    }
                }
                try {
                    response = call.awaitSuccess()
                    readImageDimensions(response.body.source())?.let { return@coroutineScope it }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    if (!range) throw e
                } finally {
                    // Cancel before close so OkHttp does not drain the remaining image into its connection pool.
                    cancellation.cancel()
                    call.cancel()
                    response?.close()
                }
                null
            }?.let { return it }
        }
        return null
    }
}
