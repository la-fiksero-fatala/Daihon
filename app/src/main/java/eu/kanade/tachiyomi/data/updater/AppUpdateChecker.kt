package eu.kanade.tachiyomi.data.updater

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.domain.release.interactor.GetApplicationRelease

@Inject
class AppUpdateChecker(
    private val getApplicationRelease: GetApplicationRelease,
) {

    suspend fun checkForUpdate(forceCheck: Boolean = false): GetApplicationRelease.Result {
        // Disable app update checks for older Android versions that we're going to drop support for
        // if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        //     return GetApplicationRelease.Result.OsTooOld
        // }

        return withContext(Dispatchers.IO) {
            val result = getApplicationRelease.await(
                GetApplicationRelease.Arguments(
                    isFoss = false,
                    isNightly = false,
                    BuildConfig.COMMIT_COUNT.toInt(),
                    BuildConfig.VERSION_NAME,
                    GITHUB_REPO,
                    forceCheck,
                ),
            )

            result
        }
    }
}

const val GITHUB_REPO = "la-fiksero-fatala/Daihon"

val RELEASE_TAG: String = BuildConfig.VERSION_NAME

val RELEASE_URL = "https://github.com/$GITHUB_REPO/releases/tag/$RELEASE_TAG"
