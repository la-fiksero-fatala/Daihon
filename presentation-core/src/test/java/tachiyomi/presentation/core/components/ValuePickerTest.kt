package tachiyomi.presentation.core.components

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ValuePickerTest {
    @Test
    fun `numeric entry accepts equivalent decimal representations`() {
        valuePickerInputIndex(listOf("0", "1.5", "2.0"), " 02 ") shouldBe 2
        valuePickerInputIndex(listOf("0", "1.5", "2.0"), "1,5") shouldBe 1
    }

    @Test
    fun `default remains distinct from numeric values`() {
        val values = listOf("Default", "1", "2")
        valuePickerInputIndex(values, "default") shouldBe 0
        valuePickerInputIndex(values, "2") shouldBe 2
        valuePickerInputIndex(values, "0") shouldBe null
    }

    @Test
    fun `invalid and out of range input cannot select a stale value`() {
        val values = listOf("0", "1", "2")
        for (input in listOf("", "-1", "3", "1.3", "NaN", "Infinity", "abc")) {
            valuePickerInputIndex(values, input) shouldBe null
        }
        valuePickerInputIndex(emptyList(), "1") shouldBe null
    }

    @Test
    fun `numeric comparison does not lose precision for large values`() {
        valuePickerInputIndex(listOf("9007199254740992", "9007199254740993"), "9007199254740993.0") shouldBe 1
    }
}
