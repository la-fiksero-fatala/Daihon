package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.viewpager.widget.DirectionalViewPager
import eu.kanade.tachiyomi.ui.reader.viewer.GestureDetectorWithLongTap
import eu.kanade.tachiyomi.ui.reader.viewer.PaperReaderGesture

/**
 * Pager implementation that listens for tap and long tap and allows temporarily disabling touch
 * events in order to work with child views that need to disable touch events on this parent. The
 * pager can also be declared to be vertical by creating it with [isHorizontal] to false.
 */
open class Pager(
    context: Context,
    private val isHorizontal: Boolean = true,
) : DirectionalViewPager(context, isHorizontal) {

    private val paperGesture = PaperReaderGesture(ViewConfiguration.get(context).scaledTouchSlop.toFloat())
    private var childCancelled = false
    var onPaperGesture: ((Float, Float, Float) -> Boolean)? = null

    /**
     * Tap listener function to execute when a tap is detected.
     */
    var tapListener: ((MotionEvent) -> Unit)? = null

    /**
     * Long tap listener function to execute when a long tap is detected.
     */
    var longTapListener: ((MotionEvent) -> Boolean)? = null

    /**
     * Gesture listener that implements tap and long tap events.
     */
    private val gestureListener = object : GestureDetectorWithLongTap.Listener() {
        override fun onSingleTapConfirmed(ev: MotionEvent): Boolean {
            tapListener?.invoke(ev)
            return true
        }

        override fun onLongTapConfirmed(ev: MotionEvent) {
            val listener = longTapListener
            if (listener != null && listener.invoke(ev)) {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        }
    }

    /**
     * Gesture detector which handles motion events.
     */
    private val gestureDetector = GestureDetectorWithLongTap(context, gestureListener)

    /**
     * Whether the gesture detector is currently enabled.
     */
    private var isGestureDetectorEnabled = true

    /**
     * Dispatches a touch event.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) childCancelled = false
        val result = paperGesture.update(ev)
        if (paperGesture.moving) {
            if (!childCancelled) {
                val cancel = MotionEvent.obtain(ev).apply { action = MotionEvent.ACTION_CANCEL }
                super.dispatchTouchEvent(cancel)
                gestureDetector.onTouchEvent(cancel)
                cancel.recycle()
                childCancelled = true
            }
            if (result != null && isGestureDetectorEnabled) {
                val handled = onPaperGesture?.invoke(result.dx, result.dy, result.scale) == true
                if (!handled && result.scale == 1f) {
                    val distance = if (isHorizontal) result.dx else result.dy
                    val perpendicular = if (isHorizontal) result.dy else result.dx
                    if (kotlin.math.abs(distance) > kotlin.math.abs(perpendicular)) {
                        setCurrentItem(currentItem + if (distance < 0) 1 else -1, false)
                    }
                }
            }
            return true
        }
        val handled = super.dispatchTouchEvent(ev)
        if (isGestureDetectorEnabled) gestureDetector.onTouchEvent(ev)
        return handled
    }

    /**
     * Whether the given [ev] should be intercepted. Only used to prevent crashes when child
     * views manipulate [requestDisallowInterceptTouchEvent].
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return false
    }

    /**
     * Handles a touch event. Only used to prevent crashes when child views manipulate
     * [requestDisallowInterceptTouchEvent].
     */
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return true
    }

    /**
     * Executes the given key event when this pager has focus. Just do nothing because the reader
     * already dispatches key events to the viewer and has more control than this method.
     */
    override fun executeKeyEvent(event: KeyEvent): Boolean {
        // Disable viewpager's default key event handling
        return false
    }

    /**
     * Enables or disables the gesture detector.
     */
    fun setGestureDetectorEnabled(enabled: Boolean) {
        isGestureDetectorEnabled = enabled
    }
}
