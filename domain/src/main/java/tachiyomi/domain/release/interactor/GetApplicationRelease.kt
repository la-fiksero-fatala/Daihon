package tachiyomi.domain.release.interactor

import dev.zacsweers.metro.Inject
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService

@Inject
class GetApplicationRelease(
    private val service: ReleaseService,
) {
    suspend fun await(arguments: Arguments): Result {
        val release = service.latest(arguments) ?: return Result.NoNewUpdate

        // Check if latest version is different from current version
        val isNewVersion = isNewVersion(
            arguments.isNightly,
            arguments.commitCount,
            arguments.versionName,
            release.version,
        )
        return when {
            isNewVersion -> Result.NewUpdate(release)
            else -> Result.NoNewUpdate
        }
    }

    private fun isNewVersion(
        isNightly: Boolean,
        commitCount: Int,
        versionName: String,
        versionTag: String,
    ): Boolean {
        val rollingPattern = Regex("^(.+)-[0-9]{8}-[0-9]{6}-r([0-9]+)$")
        val currentRolling = rollingPattern.matchEntire(versionName)
        val newRolling = rollingPattern.matchEntire(versionTag)
        if (newRolling != null) {
            val sameBranch = currentRolling == null ||
                currentRolling.groupValues[1] == newRolling.groupValues[1]
            val revision = newRolling.groupValues[2].toLongOrNull() ?: return false
            return sameBranch && revision > commitCount
        }
        if (currentRolling != null) return false

        // Retain compatibility with the older revision and semantic-version tags.
        val newVersion = versionTag.removePrefix("v").removePrefix("r")
        if (isNightly) return (newVersion.toIntOrNull() ?: return false) > commitCount

        val newParts = newVersion.split(".").map { it.toIntOrNull() ?: return false }
        val oldParts = versionName.removePrefix("v").split(".").map { it.toIntOrNull() ?: return false }
        for (index in 0 until maxOf(newParts.size, oldParts.size)) {
            val comparison = (newParts.getOrElse(index) { 0 }).compareTo(oldParts.getOrElse(index) { 0 })
            if (comparison != 0) return comparison > 0
        }
        return false
    }

    data class Arguments(
        val isFoss: Boolean,
        val isNightly: Boolean,
        val commitCount: Int,
        val versionName: String,
        val repository: String,
        val forceCheck: Boolean = false,
    )

    sealed interface Result {
        data class NewUpdate(val release: Release) : Result
        data object NoNewUpdate : Result
        data object OsTooOld : Result
    }
}
