package eu.kanade.tachiyomi.ui.reader.viewer

import android.view.MotionEvent
import kotlin.math.hypot

/** Samples touch movement without moving content; the owner commits once on release. */
internal class PaperReaderGesture(private val slop: Float) {
    data class Result(val dx: Float, val dy: Float, val scale: Float)

    var moving = false
        private set
    private var x = 0f
    private var y = 0f
    private var span = 0f
    private var scale = 1f
    private var pinching = false

    fun update(event: MotionEvent): Result? {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                x = event.x
                y = event.y
                span = 0f
                scale = 1f
                moving = false
                pinching = false
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    span = distance(event)
                    pinching = true
                    moving = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pinching && event.pointerCount >= 2 && span > 0f) {
                    scale = (distance(event) / span).coerceIn(.2f, 5f)
                }
                if (hypot(event.x - x, event.y - y) > slop) moving = true
            }
            MotionEvent.ACTION_UP -> if (moving) {
                return if (pinching) Result(0f, 0f, scale) else Result(event.x - x, event.y - y, 1f)
            }
        }
        return null
    }

    private fun distance(event: MotionEvent): Float =
        hypot(event.getX(1) - event.getX(0), event.getY(1) - event.getY(0))
}
