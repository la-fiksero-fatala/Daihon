package eu.kanade.domain.source.service

import java.util.Locale

/** Numeric overrides retain the stored disabled sources. A plus prefix enables a source explicitly. */
data class SourceLanguageSelection(val languages: Set<String>, val overrides: Set<String> = emptySet()) {
    private val languageFamilies = languages.mapTo(hashSetOf(), ::family)

    fun followsDefault(language: String): Boolean {
        val family = family(language)
        return family in unclassified || family in languageFamilies
    }

    fun isEnabled(id: Long, language: String): Boolean = when {
        id.toString() in overrides -> false
        "+$id" in overrides -> true
        else -> followsDefault(language)
    }

    private companion object {
        val unclassified = setOf("", "all", "multi", "mul", "other", "und")

        fun family(language: String): String =
            Locale.forLanguageTag(language.replace('_', '-')).language.lowercase(Locale.ROOT)
    }
}
