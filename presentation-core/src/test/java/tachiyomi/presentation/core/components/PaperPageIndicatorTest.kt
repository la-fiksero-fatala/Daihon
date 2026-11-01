package tachiyomi.presentation.core.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PaperPageIndicatorTest {
    @Test
    fun `an unmeasured popup waits instead of exposing its temporary single page`() {
        val state = PaperPageIndicatorState(header = true)
        val owner = Any()
        state.register(owner, "1 / 1", paged = false, ready = false, visible = false) {}
        state.readyToDraw shouldBe false
        state.revealIfReady() shouldBe false
    }

    @Test
    fun `a measured single page appears without a counter`() {
        val state = PaperPageIndicatorState(header = true)
        val owner = Any()
        state.register(owner, "1 / 1", paged = false, ready = true, visible = true) {}
        state.headerToken shouldBe null
        state.revealIfReady() shouldBe true
    }

    @Test
    fun `a multipage popup waits for the matching header to be composed`() {
        val state = PaperPageIndicatorState(header = true)
        val owner = Any()
        state.register(owner, "1 / 3", paged = true, ready = true, visible = true) {}
        val oldHeader = state.headerToken
        state.revealIfReady() shouldBe false
        state.update(owner, "1 / 4", paged = true, ready = true, visible = true)
        state.headerComposed(oldHeader)
        state.revealIfReady() shouldBe false
        state.headerComposed(state.headerToken)
        state.revealIfReady() shouldBe true
    }

    @Test
    fun `page changes do not hide an already visible popup`() {
        val state = PaperPageIndicatorState(header = true)
        val owner = Any()
        state.register(owner, "1 / 3", paged = true, ready = true, visible = true) {}
        state.headerComposed(state.headerToken)
        state.revealIfReady() shouldBe true
        state.update(owner, "2 / 3", paged = true, ready = true, visible = true)
        state.revealIfReady() shouldBe true
        state.remove(owner)
        state.headerToken shouldBe null
    }
}
