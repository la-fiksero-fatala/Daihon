package tachiyomi.domain.release.interactor

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService

class GetApplicationReleaseTest {

    private lateinit var getApplicationRelease: GetApplicationRelease
    private lateinit var releaseService: ReleaseService

    @BeforeEach
    fun beforeEach() {
        releaseService = mockk()

        getApplicationRelease = GetApplicationRelease(releaseService)
    }

    @ParameterizedTest
    @CsvSource(
        "main-20261107-024618-r8306, main-20261107-030000-r8307, 8306, true",
        "main-20261107-024618-r8306, main-20261107-024618-r8307, 8306, true",
        "main-20261107-024618-r8306, main-20261106-120000-r8307, 8306, true",
        "main-20261107-024618-r8306, main-20261107-024618-r8306, 8306, false",
        "main-20261107-024618-r8306, main-20261108-120000-r8305, 8306, false",
        "main-20261107-024618-r8306, feature-reader-20261108-120000-r8307, 8306, false",
        "main-20261107-024618-r8306, v0.21.3, 8306, false",
        "main-20261107-024618-r8306, invalid, 8306, false",
        "main-20261107-024618-r8306, main-20261108-120000-r9999999999999999999999, 8306, false",
        "0.21.3-8306, main-20261107-030000-r8307, 8306, true",
    )
    fun `rolling updates compare revisions within the same branch`(
        currentVersion: String,
        releaseVersion: String,
        commitCount: Int,
        expectedUpdate: Boolean,
    ) = runTest {
        val release = Release(releaseVersion, "info", "https://example.com/release", "https://example.com/app.apk")
        coEvery { releaseService.latest(any()) } returns release
        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isNightly = false,
                commitCount = commitCount,
                versionName = currentVersion,
                repository = "test",
            ),
        )
        result shouldBe if (expectedUpdate) {
            GetApplicationRelease.Result.NewUpdate(release)
        } else {
            GetApplicationRelease.Result.NoNewUpdate
        }
    }

    @Test
    fun `When has update but is nightly expect new update`() = runTest {
        val release = Release(
            "r2000",
            "info",
            "http://example.com/release_link",
            "http://example.com/release_link.apk",
        )

        coEvery { releaseService.latest(any()) } returns release

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isNightly = true,
                commitCount = 1000,
                versionName = "",
                repository = "test",
            ),
        )

        (result as GetApplicationRelease.Result.NewUpdate).release shouldBe GetApplicationRelease.Result.NewUpdate(
            release,
        ).release
    }

    @Test
    fun `When has update expect new update`() = runTest {
        val release = Release(
            "v2.0.0",
            "info",
            "http://example.com/release_link",
            "http://example.com/release_link.apk",
        )

        coEvery { releaseService.latest(any()) } returns release

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isNightly = false,
                commitCount = 0,
                versionName = "v1.0.0",
                repository = "test",
            ),
        )

        (result as GetApplicationRelease.Result.NewUpdate).release shouldBe GetApplicationRelease.Result.NewUpdate(
            release,
        ).release
    }

    @Test
    fun `When has no update expect no new update`() = runTest {
        val release = Release(
            "v1.0.0",
            "info",
            "http://example.com/release_link",
            "http://example.com/release_link.apk",
        )

        coEvery { releaseService.latest(any()) } returns release

        val result = getApplicationRelease.await(
            GetApplicationRelease.Arguments(
                isFoss = false,
                isNightly = false,
                commitCount = 0,
                versionName = "v2.0.0",
                repository = "test",
            ),
        )

        result shouldBe GetApplicationRelease.Result.NoNewUpdate
    }
}
