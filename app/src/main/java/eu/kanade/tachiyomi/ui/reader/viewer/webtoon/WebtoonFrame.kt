package eu.kanade.tachiyomi.ui.reader.viewer.webtoon

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import eu.kanade.tachiyomi.ui.reader.viewer.PaperReaderGesture

class WebtoonFrame(context: Context) : FrameLayout(context) {
    private val gesture = PaperReaderGesture(ViewConfiguration.get(context).scaledTouchSlop.toFloat())
    private var childCancelled = false
    private val recycler get() = getChildAt(0) as? WebtoonRecyclerView

    var doubleTapZoom = true
        set(value) {
            field = value
            recycler?.doubleTapZoom = value
        }
    var zoomOutDisabled = false
        set(value) {
            field = value
            recycler?.zoomOutDisabled = value
        }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) childCancelled = false
        val result = gesture.update(ev)
        if (gesture.moving) {
            if (!childCancelled) {
                val cancel = MotionEvent.obtain(ev).apply { action = MotionEvent.ACTION_CANCEL }
                super.dispatchTouchEvent(cancel)
                cancel.recycle()
                childCancelled = true
            }
            if (result != null) recycler?.applyPaperGesture(result.dx, result.dy, result.scale)
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
}
