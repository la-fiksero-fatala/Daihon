package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import eu.kanade.tachiyomi.ui.reader.setting.LegacyImageRendering

/** Retain the tiled reader's decoder, crop, fit, zoom and pan; replace only bitmap sampling. */
internal class ReaderSamplingImageView(context: Context) : SubsamplingScaleImageView(context) {
    var rendering = LegacyImageRendering()
        set(value) {
            if (value == field) return
            field = value
            invalidate()
        }
    var paddingInfo: ReaderPaddingInfo? = null
        set(value) {
            if (value == field) return
            val wasNative = field?.layout?.native == true
            field = value
            if (wasNative != (value?.layout?.native == true)) {
                val native = value?.layout?.native == true
                setMinimumScaleType(if (native) SCALE_TYPE_CENTER_INSIDE else configuredScaleType)
                setMinimumTileDpi(if (native) 0 else 180)
            }
            refit = true
            invalidate()
        }
    var configuredScaleType = SCALE_TYPE_CENTER_INSIDE
        set(value) {
            field = value
            setMinimumScaleType(if (nativeCandidate) SCALE_TYPE_CENTER_INSIDE else value)
        }
    private var refit = false
    private var appliedNative = false
    val nativeCandidate get() = paddingInfo?.layoutFor(sWidth, sHeight)?.native == true
    private val sampler = ReaderBitmapSampler(context)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (paddingInfo?.settings?.dynamic == true || appliedNative) refit = true
    }

    override fun onDraw(canvas: Canvas) {
        val native = nativeCandidate
        val matches = paddingInfo?.matchesNativeViewport(width, height, sWidth, sHeight) == true
        if (isReady && refit && (!native || matches)) {
            if (native || appliedNative || paddingInfo?.settings?.dynamic == true) {
                setMinimumScaleType(if (native) SCALE_TYPE_CENTER_INSIDE else configuredScaleType)
                if (native) setScaleAndCenter(1f, PointF(sWidth / 2f, sHeight / 2f)) else resetScaleAndCenter()
            }
            appliedNative = native
            refit = false
        }
        super.onDraw(
            SamplingCanvas(canvas, sampler, rendering, { scale }, {
                if (nativeCandidate && paddingInfo?.matchesNativeViewport(width, height, sWidth, sHeight) != true) {
                    null
                } else {
                    nativeCandidate
                }
            }),
        )
    }

    /** Only the Canvas calls used by the pinned tiled view's onDraw are delegated here. */
    private class SamplingCanvas(
        private val target: Canvas,
        private val sampler: ReaderBitmapSampler,
        private val options: LegacyImageRendering,
        private val sourceScale: () -> Float,
        private val nativePixels: () -> Boolean?,
    ) : Canvas() {
        override fun getWidth() = target.width
        override fun getHeight() = target.height
        override fun getMaximumBitmapWidth() = target.maximumBitmapWidth
        override fun getMaximumBitmapHeight() = target.maximumBitmapHeight
        override fun drawBitmap(bitmap: Bitmap, matrix: Matrix, paint: Paint?) {
            val native = nativePixels() ?: return
            sampler.draw(target, bitmap, matrix, paint, options, sourceScale(), native)
        }
        override fun drawRect(rect: Rect, paint: Paint) = target.drawRect(rect, paint)
        override fun drawRect(rect: RectF, paint: Paint) = target.drawRect(rect, paint)
        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) =
            target.drawRect(left, top, right, bottom, paint)
        override fun drawText(text: String, x: Float, y: Float, paint: Paint) = target.drawText(text, x, y, paint)
        override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) =
            target.drawCircle(cx, cy, radius, paint)
    }
}
