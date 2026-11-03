package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.image.ImageDimensions
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.InputStream

open class ReaderPage(
    index: Int,
    url: String = "",
    imageUrl: String? = null,
    var stream: (() -> InputStream)? = null,
) : Page(index, url, imageUrl, null) {

    val sourceDimensions = MutableStateFlow<ImageDimensions?>(null)
    val croppedDimensions = MutableStateFlow<ImageDimensions?>(null)
    val displayScale = MutableStateFlow<Float?>(null)

    open lateinit var chapter: ReaderChapter

    /** Set by a viewer before the download; filled during [Page.State.DownloadImage]. */
    @Volatile
    var downloadStream: DownloadStream? = null
}
