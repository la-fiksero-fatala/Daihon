package mihon.gradle

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

private val logger = Logging.getLogger("mihon.gradle.BuildInfo")

fun Project.getRollingVersionName(): Provider<String> {
    val githubBranch = providers.environmentVariable("GITHUB_HEAD_REF").orElse("")
        .zip(providers.environmentVariable("GITHUB_REF_NAME").orElse("")) { head, ref -> head.ifBlank { ref } }
    val branch = git("branch", "--show-current", default = provider { "detached" })
        .zip(githubBranch) { (local, _), remote ->
            local.ifBlank { remote.ifBlank { "detached" } }
                .replace(Regex("[^A-Za-z0-9-]+"), "-")
                .trim('-')
                .ifBlank { "detached" }
        }
    val timestamp = getLatestCommitTime().map {
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT).withZone(ZoneOffset.UTC)
            .format(java.time.Instant.parse(it))
    }
    return branch.zip(timestamp) { name, time -> "$name-$time" }
        .zip(getLatestCommitCount()) { name, revision -> "$name-r$revision" }
}

fun Project.getLatestCommitCount(): Provider<String> {
    return git("rev-list", "--count", "HEAD", default = provider { "0" }).map { it.first }
}

fun Project.getLatestCommitSha(): Provider<String> {
    return git("rev-parse", "--short", "HEAD", default = provider { "unknown" }).map { it.first }
}

fun Project.getLatestCommitTime(): Provider<String> {
    return git("log", "-1", "--format=%ct", default = getCurrentTime()).map { (value, isDefault) ->
        if (isDefault) value else Instant.fromEpochSeconds(value.toLong()).toString()
    }
}

fun Project.getCurrentTime(): Provider<String> {
    return providers.of(CurrentTimeValueSource::class.java) {}
}

private fun Project.git(vararg command: String, default: Provider<String>): Provider<Pair<String, Boolean>> {
    val workTree = layout.settingsDirectory
    return providers.of(GitValueSource::class.java) {
        parameters.command.set(listOf("git", *command))
        parameters.workTree.set(workTree)
        parameters.default.set(default)
    }
}

private abstract class GitValueSource : ValueSource<Pair<String, Boolean>, GitValueSource.Parameters> {

    interface Parameters : ValueSourceParameters {
        val command: ListProperty<String>
        val workTree: DirectoryProperty
        val default: Property<String>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): Pair<String, Boolean> {
        val command = parameters.command.get()
        val output = ByteArrayOutputStream()
        val error = ByteArrayOutputStream()

        val reason = try {
            val result = execOperations.exec {
                commandLine = command
                workingDir = parameters.workTree.get().asFile
                standardOutput = output
                errorOutput = error
                isIgnoreExitValue = true
            }
            if (result.exitValue == 0) return output.toString().trim() to false
            error.toString().trim().ifEmpty { "exited with code ${result.exitValue}" }
        } catch (e: Exception) {
            e.message?.trim().orEmpty().ifEmpty { e.toString() }
        }

        val default = parameters.default.get()
        logger.warn("`${command.joinToString(" ")}` failed, defaulting to \"$default\": $reason")
        return default to true
    }
}

private abstract class CurrentTimeValueSource : ValueSource<String, ValueSourceParameters.None> {

    override fun obtain(): String {
        val now = Clock.System.now()
        return (now - now.nanosecondsOfSecond.nanoseconds).toString()
    }
}
