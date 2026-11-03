package eu.kanade.tachiyomi.ui.reader.viewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import eu.kanade.tachiyomi.ui.reader.setting.LegacyImageFilter
import eu.kanade.tachiyomi.ui.reader.setting.LegacyImageRendering
import eu.kanade.tachiyomi.ui.reader.setting.LegacyUpscaler
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.MR
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.round

/** Samples the existing decoded bitmap directly into the existing destination transform. */
internal class ReaderBitmapSampler(context: Context) {
    private val appContext = context.applicationContext
    private val values = FloatArray(9)
    private val paint = Paint()
    private val nativeMatrix = Matrix()
    private var shader: Any? = null

    fun draw(
        canvas: Canvas,
        bitmap: Bitmap,
        matrix: Matrix,
        original: Paint?,
        options: LegacyImageRendering,
        sourceScale: Float? = null,
        nativePixels: Boolean = false,
    ) {
        matrix.getValues(values)
        val scaleX = hypot(values[Matrix.MSCALE_X], values[Matrix.MSKEW_Y])
        val scaleY = hypot(values[Matrix.MSCALE_Y], values[Matrix.MSKEW_X])
        if (nativePixels && sourceScale == 1f) {
            // Never reveal a subsampled preview as native pixels; the full-resolution tile replaces it.
            if (abs(scaleX - 1f) > .000001f || abs(scaleY - 1f) > .000001f) return
            values[Matrix.MTRANS_X] = round(values[Matrix.MTRANS_X])
            values[Matrix.MTRANS_Y] = round(values[Matrix.MTRANS_Y])
            for (index in intArrayOf(Matrix.MSCALE_X, Matrix.MSCALE_Y, Matrix.MSKEW_X, Matrix.MSKEW_Y)) {
                values[index] = round(values[index])
            }
            nativeMatrix.setValues(values)
            if (original != null) paint.set(original) else paint.reset()
            paint.isFilterBitmap = false
            paint.isAntiAlias = false
            paint.isDither = false
            canvas.drawBitmap(bitmap, nativeMatrix, paint)
            return
        }
        val enlarging = sourceScale?.let { it > 1.001f }
            ?: (scaleX > 1.001f && scaleY > 1.001f)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || unavailable || !canvas.isHardwareAccelerated ||
            ((!enlarging || options.upscaler == LegacyUpscaler.System) && options.filter == LegacyImageFilter.None)
        ) {
            canvas.drawBitmap(bitmap, matrix, original)
            return
        }
        try {
            drawShader(canvas, bitmap, matrix, original, options, enlarging, scaleX, scaleY)
        } catch (e: IllegalArgumentException) {
            unavailable = true
            logcat(LogPriority.WARN, e) { "Legacy image sampling unavailable" }
            appContext.toast(MR.strings.reader_legacy_shader_unavailable)
            canvas.drawBitmap(bitmap, matrix, original)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun drawShader(
        canvas: Canvas,
        bitmap: Bitmap,
        matrix: Matrix,
        original: Paint?,
        options: LegacyImageRendering,
        enlarging: Boolean,
        scaleX: Float,
        scaleY: Float,
    ) {
        val program = (shader as? RuntimeShader) ?: RuntimeShader(
            appContext.assets.open("shaders/legacy_sampling.agsl").bufferedReader().use { it.readText() },
        ).also { shader = it }
        val upscaler = if (enlarging) options.upscaler else LegacyUpscaler.System
        val filtering = if (upscaler == LegacyUpscaler.System) {
            BitmapShader.FILTER_MODE_LINEAR
        } else {
            BitmapShader.FILTER_MODE_NEAREST
        }
        val source = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            filterMode = filtering
        }
        program.setInputShader("image", source)
        program.setIntUniform("kernel", upscaler.ordinal)
        program.setIntUniform("effect", options.filter.ordinal)
        program.setFloatUniform("pixelOrigin", values[Matrix.MTRANS_X], values[Matrix.MTRANS_Y])
        program.setFloatUniform("pixelStep", scaleX, scaleY)
        if (original != null) paint.set(original) else paint.reset()
        paint.shader = program
        paint.isAntiAlias = false
        if (options.filter == LegacyImageFilter.Monochrome) paint.isDither = false
        val save = canvas.save()
        try {
            canvas.concat(matrix)
            canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), paint)
        } finally {
            canvas.restoreToCount(save)
        }
    }

    companion object {
        @Volatile private var unavailable = false
    }
}
