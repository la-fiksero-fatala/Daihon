package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
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
    private val sampler = ReaderBitmapSampler(context)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(SamplingCanvas(canvas, sampler, rendering) { scale })
    }

    /** Only the Canvas calls used by the pinned tiled view's onDraw are delegated here. */
    private class SamplingCanvas(
        private val target: Canvas,
        private val sampler: ReaderBitmapSampler,
        private val options: LegacyImageRendering,
        private val sourceScale: () -> Float,
    ) : Canvas() {
        override fun getWidth() = target.width
        override fun getHeight() = target.height
        override fun getMaximumBitmapWidth() = target.maximumBitmapWidth
        override fun getMaximumBitmapHeight() = target.maximumBitmapHeight
        override fun drawBitmap(bitmap: Bitmap, matrix: Matrix, paint: Paint?) =
            sampler.draw(target, bitmap, matrix, paint, options, sourceScale())
        override fun drawRect(rect: Rect, paint: Paint) = target.drawRect(rect, paint)
        override fun drawRect(rect: RectF, paint: Paint) = target.drawRect(rect, paint)
        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float, paint: Paint) =
            target.drawRect(left, top, right, bottom, paint)
        override fun drawText(text: String, x: Float, y: Float, paint: Paint) = target.drawText(text, x, y, paint)
        override fun drawCircle(cx: Float, cy: Float, radius: Float, paint: Paint) =
            target.drawCircle(cx, cy, radius, paint)
    }
}
