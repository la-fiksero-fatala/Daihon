package tachiyomi.core.common.preference

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Test

class FixedPreferenceTest {
    @Test
    fun `restored settings and deletion cannot change a product policy`() {
        val preference = FixedPreference("paper_policy", true)
        preference.set(false)
        preference.delete()
        preference.get() shouldBe true
        preference.defaultValue() shouldBe true
        preference.changes().value shouldBe true
        preference.stateIn(CoroutineScope(Dispatchers.Unconfined)).value shouldBe true
        preference.isSet() shouldBe false
    }
}
