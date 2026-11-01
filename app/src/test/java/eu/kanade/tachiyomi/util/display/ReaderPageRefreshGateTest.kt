package eu.kanade.tachiyomi.util.display

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ReaderPageRefreshGateTest {
    @Test
    fun `first page waits for decode and refreshes only once`() {
        val gate = ReaderPageRefreshGate<Any>()
        val page = Any()
        gate.select(page)
        gate.takeReady() shouldBe null
        gate.decoded(page)
        gate.takeReady() shouldBe page
        gate.select(page)
        gate.takeReady() shouldBe null
    }

    @Test
    fun `preloads and replaced pending pages do not refresh`() {
        val gate = ReaderPageRefreshGate<Any>()
        val first = Any()
        val second = Any()
        gate.select(first)
        gate.decoded(second)
        gate.takeReady() shouldBe null
        gate.select(second)
        gate.decoded(first)
        gate.takeReady() shouldBe second
        gate.select(first)
        gate.takeReady() shouldBe first
    }

    @Test
    fun `released images must decode again`() {
        val gate = ReaderPageRefreshGate<Any>()
        val page = Any()
        gate.decoded(page)
        gate.released(page)
        gate.select(page)
        gate.takeReady() shouldBe null
        gate.decoded(page)
        gate.takeReady() shouldBe page
    }

    @Test
    fun `overlapping sections of a decoded strip count as page changes`() {
        val gate = ReaderPageRefreshGate<Any>()
        val page = Any()
        gate.select(page)
        gate.decoded(page)
        gate.takeReady() shouldBe page
        gate.select(page, sectionChanged = true)
        gate.takeReady() shouldBe page
        gate.takeReady() shouldBe null
    }

    @Test
    fun `chapter transitions cancel pending refresh even if old image finishes decoding`() {
        val gate = ReaderPageRefreshGate<Any>()
        val page = Any()
        gate.select(page)
        gate.clearSelection()
        gate.decoded(page)
        gate.takeReady() shouldBe null
        gate.select(page)
        gate.takeReady() shouldBe page
    }
}
