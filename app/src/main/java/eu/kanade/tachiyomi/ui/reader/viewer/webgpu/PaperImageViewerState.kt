@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package eu.kanade.tachiyomi.ui.reader.viewer.webgpu

import androidx.webgpu.GPUColor
import androidx.webgpu.GPUCommandEncoder
import androidx.webgpu.GPURenderPassColorAttachment
import androidx.webgpu.GPURenderPassDescriptor
import androidx.webgpu.GPUTexture
import androidx.webgpu.LoadOp
import androidx.webgpu.StoreOp
import ca.mpreg.webgpuviewer.renderer.WebGpuRenderer
import ca.mpreg.webgpuviewer.viewer.ImagePage
import ca.mpreg.webgpuviewer.viewer.ImageViewerState

/**
 * Complete high-quality tiles before presenting a paged frame, rather than revealing them centre-out.
 *
 * The dependency is pinned to v54. Its full-tile path is internal, so isolate that access here until
 * an upstream public presentation policy exists. Sampling, image filters and the native cache stay intact.
 */
internal class PaperImageViewerState(isVertical: Boolean, isReversed: Boolean) :
    ImageViewerState(isVertical, isReversed) {

    private class Frame(val snapshot: Any, val page: ImagePage?)

    override fun captureRenderState(): Any? = super.captureRenderState()?.let { Frame(it, getPage(0)) }

    override suspend fun renderSnapshot(encoder: GPUCommandEncoder, texture: GPUTexture, snapshot: Any) {
        val frame = snapshot as Frame
        val page = frame.page as? ImagePage.ImageSingle
        if (page != null && page !is ImagePage.Compose && page.highQuality && !page.destroyed) {
            val tiles = (this as ImageViewerState).tiles
            tiles.newFrame()
            val preparation = WebGpuRenderer.device.createCommandEncoder()
            try {
                val view = texture.createView()
                val pass = preparation.beginRenderPass(
                    GPURenderPassDescriptor(
                        colorAttachments = arrayOf(
                            GPURenderPassColorAttachment(
                                view = view,
                                loadOp = LoadOp.Clear,
                                storeOp = StoreOp.Store,
                                clearValue = GPUColor(0.0, 0.0, 0.0, 0.0),
                            ),
                        ),
                    ),
                )
                try {
                    tiles.renderFullyTiled(pass, page, texture)
                } finally {
                    pass.end()
                    pass.close()
                    view.close()
                }
                // Tile generation submits its own commands. Discard this helper's blit: v54
                // destroys its transient instance buffer before a caller can submit the pass.
                // The base renderer below presents the completed cache using its retained buffer.
            } finally {
                preparation.close()
            }
        }
        super.renderSnapshot(encoder, texture, frame.snapshot)
    }
}
