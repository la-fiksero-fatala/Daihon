package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Animatable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.AttrRes
import androidx.annotation.CallSuper
import androidx.annotation.StyleRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.postDelayed
import androidx.core.view.isVisible
import coil3.asDrawable
import coil3.dispose
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import coil3.size.Size
import coil3.size.ViewSizeResolver
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.EASE_IN_OUT_QUAD
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.EASE_OUT_QUAD
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE
import com.github.chrisbanes.photoview.PhotoView
import eu.kanade.tachiyomi.data.coil.ImageDecoder
import eu.kanade.tachiyomi.data.coil.cropBorders
import eu.kanade.tachiyomi.data.coil.webtoonStrips
import eu.kanade.tachiyomi.ui.reader.setting.LegacyImageRendering
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonStripImageView
import eu.kanade.tachiyomi.util.image.ImageDimensions
import eu.kanade.tachiyomi.util.system.animatorDurationScale
import eu.kanade.tachiyomi.util.view.isVisibleOnScreen
import mihon.app.di.appGraph
import okio.BufferedSource

/**
 * A wrapper view for showing page image.
 *
 * Animated image will be drawn by [PhotoView] while [SubsamplingScaleImageView] will take non-animated image.
 *
 * @param isWebtoon if true, [WebtoonStripImageView] will be used instead of [SubsamplingScaleImageView]
 * and [AppCompatImageView] will be used instead of [PhotoView]
 */
open class ReaderPageImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttrs: Int = 0,
    @StyleRes defStyleRes: Int = 0,
    private val isWebtoon: Boolean = false,
) : FrameLayout(context, attrs, defStyleAttrs, defStyleRes) {

    private var pageView: View? = null

    /**
     * The reader's grayscale and inverted colours for the page image alone, so the transition and error views
     * keep theirs - see [LocalPageColorFilter].
     */
    var pageColorFilter: ColorFilter? = null
        set(value) {
            if (field == value) return
            field = value
            applyPageColorFilter()
        }

    private fun applyPageColorFilter() {
        val filter = pageColorFilter
        when (val view = pageView) {
            is ImageView -> view.colorFilter = filter
            is WebtoonStripImageView -> view.colorFilter = filter
            // Drawn with its own paints: a layer takes the filter instead, as wide as the screen it fills.
            is SubsamplingScaleImageView -> view.setLayerType(
                if (filter == null) LAYER_TYPE_NONE else LAYER_TYPE_HARDWARE,
                filter?.let { Paint().apply { colorFilter = it } },
            )
        }
    }

    var paddingInfo: ReaderPaddingInfo? = null
        set(value) {
            field = value
            (pageView as? ReaderSamplingImageView)?.paddingInfo = value
        }

    var imageRendering = LegacyImageRendering()
        set(value) {
            field = value
            applyImageRendering()
        }

    private fun applyImageRendering() {
        when (val view = pageView) {
            is ReaderSamplingImageView -> {
                view.rendering = imageRendering
                view.paddingInfo = paddingInfo
            }
            is WebtoonStripImageView -> view.rendering = imageRendering
        }
    }

    private var stripRequest: Disposable? = null

    private var config: Config? = null

    /** Decoded source pixels after the region decoder's crop, independent of view scaling. */
    val croppedImageDimensions: ImageDimensions?
        get() = (pageView as? SubsamplingScaleImageView)?.takeIf { it.isReady }?.let {
            ImageDimensions(it.sWidth, it.sHeight)
        }

    var onImageLoaded: (() -> Unit)? = null
    var onImageLoadError: ((Throwable?) -> Unit)? = null
    var onScaleChanged: ((newScale: Float) -> Unit)? = null
    var onViewClicked: (() -> Unit)? = null

    /**
     * For automatic background. Will be set as background color when [onImageLoaded] is called.
     */
    var pageBackground: Drawable? = null

    @CallSuper
    open fun onImageLoaded() {
        onImageLoaded?.invoke()
        background = pageBackground
    }

    @CallSuper
    open fun onImageLoadError(error: Throwable?) {
        onImageLoadError?.invoke(error)
    }

    @CallSuper
    open fun onScaleChanged(newScale: Float) {
        onScaleChanged?.invoke(newScale)
    }

    @CallSuper
    open fun onViewClicked() {
        onViewClicked?.invoke()
    }

    open fun onPageSelected(forward: Boolean) {
        with(pageView as? SubsamplingScaleImageView) {
            if (this == null) return
            if (isReady) {
                landscapeZoom(forward)
            } else {
                setOnImageEventListener(
                    object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                        override fun onReady() {
                            setupZoom(config)
                            landscapeZoom(forward)
                            this@ReaderPageImageView.onImageLoaded()
                        }

                        override fun onImageLoadError(e: Exception) {
                            onImageLoadError(e)
                        }
                    },
                )
            }
        }
    }

    private fun SubsamplingScaleImageView.landscapeZoom(forward: Boolean) {
        if ((this as? ReaderSamplingImageView)?.nativeCandidate == true) return
        if (
            config != null &&
            config!!.landscapeZoom &&
            config!!.minimumScaleType == SCALE_TYPE_CENTER_INSIDE &&
            sWidth > sHeight &&
            scale == minScale
        ) {
            run {
                val point = when (config!!.zoomStartPosition) {
                    ZoomStartPosition.LEFT -> if (forward) PointF(0F, 0F) else PointF(sWidth.toFloat(), 0F)
                    ZoomStartPosition.RIGHT -> if (forward) PointF(sWidth.toFloat(), 0F) else PointF(0F, 0F)
                    ZoomStartPosition.CENTER -> center
                }

                val targetScale = height.toFloat() / sHeight.toFloat()
                setScaleAndCenter(targetScale, point)
            }
        }
    }

    fun setImage(drawable: Drawable, config: Config) {
        this.config = config
        if (drawable is Animatable) {
            prepareAnimatedImageView()
            setAnimatedImage(drawable, config)
        } else {
            prepareNonAnimatedImageView()
            setNonAnimatedImage(drawable, config)
        }
    }

    fun setImage(source: BufferedSource, isAnimated: Boolean, config: Config) {
        this.config = config
        if (isAnimated) {
            prepareAnimatedImageView()
            setAnimatedImage(source, config)
        } else if (isWebtoon) {
            prepareStripImageView()
            setStripImage(source, config)
        } else {
            prepareNonAnimatedImageView()
            setNonAnimatedImage(source, config)
        }
    }

    private val paperGesture = PaperReaderGesture(android.view.ViewConfiguration.get(context).scaledTouchSlop.toFloat())
    private var childCancelled = false

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) childCancelled = false
        val result = paperGesture.update(event)
        if (paperGesture.moving) {
            if (!childCancelled) {
                val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                super.dispatchTouchEvent(cancel)
                cancel.recycle()
                childCancelled = true
            }
            if (result != null) applyPaperGesture(result.dx, result.dy, result.scale)
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    /** Commit a pinch or pan once, after the finger has been lifted. */
    fun applyPaperGesture(dx: Float, dy: Float, factor: Float): Boolean {
        when (val view = pageView) {
            is SubsamplingScaleImageView -> {
                val center = view.center ?: return false
                if (factor != 1f || view.scale > view.minScale * 1.01f) {
                    val targetScale = (view.scale * factor).coerceIn(view.minScale, view.maxScale)
                    view.setScaleAndCenter(targetScale, PointF(center.x - dx / view.scale, center.y - dy / view.scale))
                    return true
                }
            }
            is PhotoView -> {
                if (factor != 1f) {
                    view.setScale((view.scale * factor).coerceIn(view.minimumScale, view.maximumScale), false)
                    return true
                }
                if (view.scale > view.minimumScale * 1.01f) {
                    val matrix = android.graphics.Matrix()
                    view.getDisplayMatrix(matrix)
                    matrix.postTranslate(dx, dy)
                    view.setDisplayMatrix(matrix)
                    return true
                }
            }
        }
        return false
    }

    fun recycle() {
        stripRequest?.dispose()
        stripRequest = null
        pageView?.let {
            when (it) {
                is SubsamplingScaleImageView -> it.recycle()
                is AppCompatImageView -> it.dispose()
                is WebtoonStripImageView -> it.recycle()
            }
            it.isVisible = false
        }
    }

    /**
     * Check if the image can be panned to the left
     */
    fun canPanLeft(): Boolean = canPan { it.left }

    /**
     * Check if the image can be panned to the right
     */
    fun canPanRight(): Boolean = canPan { it.right }

    /**
     * Check whether the image can be panned.
     * @param fn a function that returns the direction to check for
     */
    private fun canPan(fn: (RectF) -> Float): Boolean {
        (pageView as? SubsamplingScaleImageView)?.let { view ->
            RectF().let {
                view.getPanRemaining(it)
                return fn(it) > 1
            }
        }
        return false
    }

    /**
     * Pans the image to the left by a screen's width worth.
     */
    fun panLeft() {
        pan { center, view -> center.also { it.x -= view.width / view.scale } }
    }

    /**
     * Pans the image to the right by a screen's width worth.
     */
    fun panRight() {
        pan { center, view -> center.also { it.x += view.width / view.scale } }
    }

    /**
     * Pans the image.
     * @param fn a function that computes the new center of the image
     */
    private fun pan(fn: (PointF, SubsamplingScaleImageView) -> PointF) {
        (pageView as? SubsamplingScaleImageView)?.let { view ->

            val target = fn(view.center ?: return, view)
            view.setScaleAndCenter(view.scale, target)
        }
    }

    private fun prepareNonAnimatedImageView() {
        if (pageView is SubsamplingScaleImageView) return
        removeView(pageView)

        pageView = ReaderSamplingImageView(context).apply {
            setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER_IMMEDIATE)
            setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
            setMinimumTileDpi(180)
            setOnStateChangedListener(
                object : SubsamplingScaleImageView.OnStateChangedListener {
                    override fun onScaleChanged(newScale: Float, origin: Int) {
                        this@ReaderPageImageView.onScaleChanged(newScale)
                    }

                    override fun onCenterChanged(newCenter: PointF?, origin: Int) {
                        // Not used
                    }
                },
            )
            setOnClickListener { this@ReaderPageImageView.onViewClicked() }
        }
        addView(pageView, MATCH_PARENT, MATCH_PARENT)
        applyPageColorFilter()
        applyImageRendering()
    }

    private fun SubsamplingScaleImageView.setupZoom(config: Config?) {
        // 5x zoom
        maxScale = scale * MAX_ZOOM_SCALE
        setDoubleTapZoomScale(scale * 2)

        when (config?.zoomStartPosition) {
            ZoomStartPosition.LEFT -> setScaleAndCenter(scale, PointF(0F, 0F))
            ZoomStartPosition.RIGHT -> setScaleAndCenter(scale, PointF(sWidth.toFloat(), 0F))
            ZoomStartPosition.CENTER -> setScaleAndCenter(scale, center)
            null -> {}
        }
    }

    private fun setNonAnimatedImage(
        data: Any,
        config: Config,
    ) = (pageView as? SubsamplingScaleImageView)?.apply {
        setDoubleTapZoomDuration(config.zoomDuration.getSystemScaledDuration())
        if (this is ReaderSamplingImageView) {
            configuredScaleType = config.minimumScaleType
        } else {
            setMinimumScaleType(config.minimumScaleType)
        }
        setMinimumDpi(1) // Just so that very small image will be fit for initial load
        setCropBorders(config.cropBorders)
        setOnImageEventListener(
            object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                override fun onReady() {
                    setupZoom(config)
                    if (isVisibleOnScreen()) landscapeZoom(true)
                    this@ReaderPageImageView.onImageLoaded()
                }

                override fun onImageLoadError(e: Exception) {
                    this@ReaderPageImageView.onImageLoadError(e)
                }
            },
        )

        when (data) {
            is BitmapDrawable -> {
                setImage(ImageSource.bitmap(data.bitmap))
                isVisible = true
            }
            is BufferedSource -> {
                setImage(ImageSource.inputStream(data.inputStream()))
                isVisible = true
            }
            else -> {
                throw IllegalArgumentException("Not implemented for class ${data::class.simpleName}")
            }
        }
    }

    private fun prepareStripImageView() {
        if (pageView is WebtoonStripImageView) return
        removeView(pageView)

        pageView = WebtoonStripImageView(context)
        addView(pageView, MATCH_PARENT, WRAP_CONTENT)
        applyPageColorFilter()
        applyImageRendering()
    }

    private fun setStripImage(data: BufferedSource, config: Config) {
        val view = pageView as? WebtoonStripImageView ?: return
        view.onReload = { requestStrips(view, data, config, isReload = true) }
        requestStrips(view, data, config, isReload = false)
    }

    private fun requestStrips(view: WebtoonStripImageView, data: BufferedSource, config: Config, isReload: Boolean) {
        // A recycled view is reused for another page, where a request still running for the old one must not land
        stripRequest?.dispose()
        stripRequest = ImageRequest.Builder(context)
            // Peeking leaves the data whole for decoding again after the strips were freed off screen
            .data(data.peek())
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .target(
                onSuccess = { result ->
                    view.setImage(result as ImageDecoder.StripsImage)
                    if (!isReload) {
                        view.isVisible = true
                        onImageLoaded()
                    }
                },
            )
            .listener(
                onError = { _, result ->
                    onImageLoadError(result.throwable)
                },
            )
            .size(ViewSizeResolver(this))
            .precision(Precision.INEXACT)
            .cropBorders(config.cropBorders)
            .webtoonStrips(true)
            .crossfade(false)
            .build()
            .let(context.imageLoader::enqueue)
    }

    private fun prepareAnimatedImageView() {
        if (pageView is AppCompatImageView) return
        removeView(pageView)

        pageView = if (isWebtoon) {
            AppCompatImageView(context)
        } else {
            PhotoView(context)
        }.apply {
            adjustViewBounds = true

            if (this is PhotoView) {
                setScaleLevels(1F, 2F, MAX_ZOOM_SCALE)
                // Force 2 scale levels on double tap
                setOnDoubleTapListener(
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onDoubleTap(e: MotionEvent): Boolean {
                            if (scale > 1F) {
                                setScale(1F, e.x, e.y, false)
                            } else {
                                setScale(2F, e.x, e.y, false)
                            }
                            return true
                        }

                        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                            this@ReaderPageImageView.onViewClicked()
                            return super.onSingleTapConfirmed(e)
                        }
                    },
                )
                setOnScaleChangeListener { _, _, _ ->
                    this@ReaderPageImageView.onScaleChanged(scale)
                }
            }
        }
        addView(pageView, MATCH_PARENT, MATCH_PARENT)
        applyPageColorFilter()
        applyImageRendering()
    }

    private fun setAnimatedImage(
        data: Any,
        config: Config,
    ) = (pageView as? AppCompatImageView)?.apply {
        if (this is PhotoView) {
            setZoomTransitionDuration(config.zoomDuration.getSystemScaledDuration())
        }

        val request = ImageRequest.Builder(context)
            .data(data)
            .apply {
                if (!isWebtoon && context.appGraph.readerPreferences.dynamicPagePadding.get()) size(Size.ORIGINAL)
            }
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .target(
                onSuccess = { result ->
                    val drawable = result.asDrawable(context.resources)
                    (drawable as? Animatable)?.stop()
                    if (!isWebtoon && context.appGraph.readerPreferences.dynamicPagePadding.get()) {
                        // Freeze at decoded source size, then use the same integer-aligned paged path.
                        val frame = drawable.toBitmap(result.width, result.height)
                        prepareNonAnimatedImageView()
                        setNonAnimatedImage(BitmapDrawable(context.resources, frame), config)
                    } else {
                        setImageDrawable(drawable)
                        isVisible = true
                        this@ReaderPageImageView.onImageLoaded()
                    }
                },
            )
            .listener(
                onError = { _, result ->
                    onImageLoadError(result.throwable)
                },
            )
            .crossfade(false)
            .build()
        context.imageLoader.enqueue(request)
    }

    private fun Int.getSystemScaledDuration(): Int {
        return (this * context.animatorDurationScale).toInt().coerceAtLeast(1)
    }

    /**
     * All of the config except [zoomDuration] will only be used for non-animated image.
     */
    data class Config(
        val zoomDuration: Int,
        val minimumScaleType: Int = SCALE_TYPE_CENTER_INSIDE,
        val cropBorders: Boolean = false,
        val zoomStartPosition: ZoomStartPosition = ZoomStartPosition.CENTER,
        val landscapeZoom: Boolean = false,
    )

    enum class ZoomStartPosition {
        LEFT,
        CENTER,
        RIGHT,
    }
}

private const val MAX_ZOOM_SCALE = 5F
