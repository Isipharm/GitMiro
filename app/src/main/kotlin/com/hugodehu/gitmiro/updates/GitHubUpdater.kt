package com.hugodehu.gitmiro.updates

import com.hugodehu.gitmiro.AppConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

private const val INSTALLER_PREFIX = "GitMiro_Windows_Installer_"
private const val CREDENTIAL_TIMEOUT_MINUTES = 2L

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubAsset> = emptyList(),
) {
    val installer get() = assets.firstOrNull { it.name.startsWith(INSTALLER_PREFIX) && it.name.endsWith(".exe") }
}

@Serializable
data class GitHubAsset(val name: String, val url: String, val size: Long)

class GitHubAccessException(statusCode: Int) : Exception(
    "GitHub refused access (HTTP $statusCode). Sign in to GitHub with Git (Git Credential Manager) using an account " +
            "that can read ${AppConstants.GITHUB_REPOSITORY}."
)

/**
 * Reads the releases of the private GitHub repository and downloads their installer, authenticated with the token
 * that Git stores for github.com, so it works for anyone who can read the repository.
 *
 * Uses the JDK HTTP client on purpose: the app's Ktor client trusts every certificate, which is no place for a token
 * or for an executable that is about to be run.
 */
object GitHubUpdater {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()

    /**
     * Published releases newer than the running version, newest first (GitHub lists them by creation date).
     */
    suspend fun newerReleases(): List<GitHubRelease> = withContext(Dispatchers.IO) {
        val request = apiRequest("https://api.github.com/repos/${AppConstants.GITHUB_REPOSITORY}/releases?per_page=30")
            .header("Accept", "application/vnd.github+json")
            .build()
        val response = client.send(request, BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw GitHubAccessException(response.statusCode())
        }

        json.decodeFromString<List<GitHubRelease>>(response.body())
            .filter { !it.draft && !it.prerelease }
            .takeWhile { it.tagName != "v${AppConstants.APP_VERSION}" }
    }

    suspend fun downloadInstaller(asset: GitHubAsset, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        // The asset endpoint redirects to a signed download URL, which must be requested without the token
        val redirectRequest = apiRequest(asset.url).header("Accept", "application/octet-stream").build()
        val redirect = client.send(redirectRequest, BodyHandlers.discarding())
        val location = redirect.headers().firstValue("Location").orElse(null)
            ?: throw GitHubAccessException(redirect.statusCode())

        val response = client.send(HttpRequest.newBuilder(URI(location)).build(), BodyHandlers.ofInputStream())
        if (response.statusCode() != 200) {
            response.body().close()
            throw IOException("Download failed (HTTP ${response.statusCode()})")
        }

        val installer = File(Files.createTempDirectory("gitmiro-update").toFile(), asset.name)

        response.body().use { input ->
            installer.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var downloaded = 0L

                while (true) {
                    ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break

                    output.write(buffer, 0, read)
                    downloaded += read
                    onProgress(downloaded.toFloat() / asset.size)
                }
            }
        }

        if (installer.length() != asset.size) {
            throw IOException("Incomplete download (${installer.length()} of ${asset.size} bytes)")
        }

        installer
    }

    /**
     * Runs the installer and quits so it can replace the app's files. Silent mode only shows the progress, and the
     * installer relaunches GitMiro when it's done (gitnuro.iss).
     */
    fun install(installer: File): Nothing {
        ProcessBuilder(installer.absolutePath, "/SILENT", "/SP-").start()
        exitProcess(0)
    }

    private fun apiRequest(url: String): HttpRequest.Builder {
        val builder = HttpRequest.newBuilder(URI(url)).header("X-GitHub-Api-Version", "2022-11-28")
        val token = gitHubToken()

        if (token != null) {
            builder.header("Authorization", "Bearer $token")
        }

        return builder
    }

    /**
     * The password Git stores for github.com: with Git Credential Manager, an OAuth token. Git Credential Manager may
     * show its sign-in window when nothing is stored yet. Null when Git isn't installed or has no credential.
     */
    private fun gitHubToken(): String? = try {
        val process = ProcessBuilder("git", "credential", "fill")
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .apply { environment()["GIT_TERMINAL_PROMPT"] = "0" }
            .start()

        process.outputStream.bufferedWriter().use { it.write("protocol=https\nhost=github.com\n\n") }

        if (process.waitFor(CREDENTIAL_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
            process.inputStream.bufferedReader().readLines()
                .firstOrNull { it.startsWith("password=") }
                ?.removePrefix("password=")
        } else {
            process.destroy()
            null
        }
    } catch (ex: IOException) {
        null
    }
}
