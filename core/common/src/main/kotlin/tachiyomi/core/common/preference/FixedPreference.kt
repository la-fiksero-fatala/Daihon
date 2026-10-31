package tachiyomi.core.common.preference

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** A product policy, deliberately unaffected by saved or restored settings. */
class FixedPreference<T>(private val name: String, private val value: T) : Preference<T> {
    private val state = MutableStateFlow(value)
    override fun key() = Preference.appStateKey(name)
    override fun get() = value
    override fun set(value: T) = Unit
    override fun isSet() = false
    override fun delete() = Unit
    override fun defaultValue() = value
    override fun changes(): StateFlow<T> = state
    override fun stateIn(scope: CoroutineScope): StateFlow<T> = state
}
