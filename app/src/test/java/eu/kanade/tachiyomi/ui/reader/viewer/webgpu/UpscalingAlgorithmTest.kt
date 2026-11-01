package eu.kanade.tachiyomi.ui.reader.viewer.webgpu

import ca.mpreg.webgpuviewer.renderer.ImageFilterWgsl
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.File

class UpscalingAlgorithmTest {
    private fun filter() = ImageFilterWgsl(File("src/main/assets/shaders/anime4k_dog.wgsl").readText())

    @Test
    fun `the optional scaler never changes native-size or downscaled pages`() {
        val filter = filter()
        filter.applies(.5f) shouldBe false
        filter.applies(1f) shouldBe false
    }

    @Test
    fun `the optional enhancement preserves source size for the single final resize`() {
        val filter = filter()
        listOf(1.25f, 1.5f, 2f, 3f).forEach { scale ->
            filter.applies(scale) shouldBe true
            filter.scale(scale) shouldBe 1f
        }
    }
}
