package eu.kanade.tachiyomi.ui.reader.viewer.webgpu

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import ca.mpreg.webgpuviewer.viewer.ImagePage
import ca.mpreg.webgpuviewer.viewer.ImageViewer
import ca.mpreg.webgpuviewer.viewer.ImageViewerContinuous
import ca.mpreg.webgpuviewer.viewer.ImageViewerContinuousState
import eu.kanade.tachiyomi.ui.reader.viewer.PaperReaderGesture
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderThemedContent
import eu.kanade.tachiyomi.ui.reader.viewer.mouseWheelPaging

/** Keep the renderer's measured viewport intact; commit touch movement only on release. */
@Composable
internal fun PaperWebGpuContent(viewer: WebGpuViewer, modifier: Modifier) {
    AndroidView(
        modifier = modifier.mouseWheelPaging(viewer),
        factory = { context ->
            PaperWebGpuTouchHost(context, viewer).apply {
                addView(
                    ComposeView(context).apply {
                        setViewCompositionStrategy(
                            ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool,
                        )
                        setContent {
                            ReaderThemedContent(viewer.readerThemedContext) {
                                if (viewer.isContinuous) {
                                    ImageViewerContinuous(
                                        Modifier.fillMaxSize(),
                                        viewer.viewerState as ImageViewerContinuousState,
                                    )
                                } else {
                                    ImageViewer(Modifier.fillMaxSize(), viewer.viewerState)
                                }
                            }
                        }
                    },
                    LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT),
                )
            }
        },
    )
}

private class PaperWebGpuTouchHost(context: Context, private val viewer: WebGpuViewer) : FrameLayout(context) {
    private val gesture = PaperReaderGesture(ViewConfiguration.get(context).scaledTouchSlop.toFloat())
    private var chrome = true
    private var cancelled = false
    private fun position(event: MotionEvent) = Offset(
        event.x / width.coerceAtLeast(1),
        event.y / height.coerceAtLeast(1),
    )
    private val taps = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (!viewer.doubleTapEnabled) viewer.viewerState.onTap?.invoke(position(e))
                return true
            }
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (viewer.doubleTapEnabled) viewer.viewerState.onTap?.invoke(position(e))
                return true
            }
            override fun onDoubleTap(e: MotionEvent) = true
            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                if (e.actionMasked == MotionEvent.ACTION_UP && viewer.doubleTapEnabled) {
                    viewer.paperDoubleTap(position(e))
                }
                return true
            }
            override fun onLongPress(e: MotionEvent) {
                viewer.viewerState.onLongTap?.invoke(position(e))
            }
        },
    )

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            chrome = hasControls(viewer.viewerState.getPage(0))
            cancelled = false
        }
        val result = gesture.update(event)
        if (!chrome) taps.onTouchEvent(event)
        if (gesture.moving) {
            if (chrome && !cancelled) {
                val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                super.dispatchTouchEvent(cancel)
                cancel.recycle()
                cancelled = true
            }
            result?.let { viewer.paperGesture(it.dx, it.dy, it.scale) }
            return true
        }
        // Retry, chapter transition and other Compose page controls retain native touch handling.
        return if (chrome) super.dispatchTouchEvent(event) else true
    }

    private fun hasControls(page: ImagePage?): Boolean = when (page) {
        null, is ImagePage.Compose -> true
        is ImagePage.ImageSpread -> listOfNotNull(page.left, page.right).any(::hasControls)
        else -> false
    }
}
