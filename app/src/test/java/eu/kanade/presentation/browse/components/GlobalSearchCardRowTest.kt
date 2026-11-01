package eu.kanade.presentation.browse.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class GlobalSearchCardRowTest {
    @Test
    fun `a partial card waits for the next horizontal page`() {
        searchResultRowCapacity(272, 88, 4) shouldBe 3
        searchResultRowCapacity(271, 88, 4) shouldBe 2
        searchResultRowCapacity(88, 88, 4) shouldBe 1
    }

    @Test
    fun `narrow rows retain one usable card slot`() {
        searchResultRowCapacity(0, 88, 4) shouldBe 1
        searchResultRowCapacity(40, 40, 4) shouldBe 1
    }
}
