package eu.kanade.domain.source.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SourceLanguageSelectionTest {

    @Test
    fun `regional variants and multi sources survive the global filter`() {
        val selection = SourceLanguageSelection(setOf("pt", "zh-Hant"))
        listOf("pt-BR", "zh-CN", "all", "Multi", "mul", "other", "und", "").forEach { language ->
            selection.isEnabled(1, language) shouldBe true
        }
        selection.isEnabled(1, "fr") shouldBe false
    }

    @Test
    fun `existing disables and explicit enables override global defaults independently`() {
        val selection = SourceLanguageSelection(setOf("en"), setOf("12", "+13", "14"))
        selection.isEnabled(11, "en") shouldBe true
        selection.isEnabled(12, "en") shouldBe false
        selection.isEnabled(13, "ja") shouldBe true
        selection.isEnabled(14, "all") shouldBe false
        selection.isEnabled(15, "ja") shouldBe false
    }
}
