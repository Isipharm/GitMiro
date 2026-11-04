package com.hugodehu.gitmiro.ui.dialogs

import com.hugodehu.gitmiro.ui.components.AppProgressBar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hugodehu.gitmiro.AppConstants
import com.hugodehu.gitmiro.common.OS
import com.hugodehu.gitmiro.common.currentOs
import com.hugodehu.gitmiro.extensions.handOnHover
import com.hugodehu.gitmiro.theme.dialogOverlay
import com.hugodehu.gitmiro.theme.onBackgroundSecondary
import com.hugodehu.gitmiro.theme.textButtonColors
import com.hugodehu.gitmiro.ui.components.PrimaryButton
import com.hugodehu.gitmiro.ui.components.ScrollableLazyColumn
import com.hugodehu.gitmiro.ui.dialogs.base.MaterialDialog
import com.hugodehu.gitmiro.updates.GitHubRelease
import com.hugodehu.gitmiro.updates.GitHubUpdater
import com.hugodehu.gitmiro.updates.Update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// " by @someone in https://github.com/owner/repo/pull/12" in the notes GitHub generates
private val pullRequestAuthorRegex = Regex(""" by @\S+ in https://github\.com/\S+/pull/(\d+)""")

@Composable
fun UpdateDialog(
    update: Update,
    onClose: () -> Unit,
    onOpenUrlInBrowser: (String) -> Unit,
) {
    var releases by remember { mutableStateOf<List<GitHubRelease>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf<Float?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            releases = GitHubUpdater.newerReleases()
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            error = ex.message ?: ex.toString()
        }
    }

    val installer = releases?.firstOrNull()?.installer
    val closeIfNotDownloading = { if (downloadProgress == null) onClose() }

    // Same window as the dialogs shown through navigation (AppTab), so it sits above the screen
    Dialog(
        onDismissRequest = closeIfNotDownloading,
        properties = DialogProperties(
            scrimColor = MaterialTheme.colors.dialogOverlay,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        MaterialDialog(onCloseRequested = closeIfNotDownloading) {
            Column(
                modifier = Modifier
                    .width(600.dp)
                    .height(500.dp)
            ) {
                Text(
                    "GitMiro ${update.appVersion} is available",
                    style = MaterialTheme.typography.h3,
                )

                Text(
                    "You are using version ${AppConstants.APP_VERSION}. Changes since then:",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val currentReleases = releases
                    val currentError = error

                    when {
                        currentError != null -> Text(
                            currentError,
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.error,
                        )

                        currentReleases == null -> Text("Loading the changes…", style = MaterialTheme.typography.body1)
                        else -> ReleaseNotes(currentReleases)
                    }
                }

                downloadProgress?.let { progress ->
                    AppProgressBar(
                        progress = progress,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        modifier = Modifier.handOnHover(),
                        onClick = { onOpenUrlInBrowser(update.downloadUrl) },
                        colors = textButtonColors(),
                    ) {
                        Text("Open on GitHub")
                    }

                    TextButton(
                        modifier = Modifier.padding(end = 8.dp).handOnHover(),
                        enabled = downloadProgress == null,
                        onClick = onClose,
                        colors = textButtonColors(),
                    ) {
                        Text("Later")
                    }

                    if (currentOs == OS.WINDOWS) {
                        PrimaryButton(
                            text = if (downloadProgress == null) "Download and install" else "Downloading…",
                            enabled = installer != null && downloadProgress == null,
                            onClick = {
                                val asset = installer ?: return@PrimaryButton

                                scope.launch {
                                    try {
                                        downloadProgress = 0f
                                        val file = GitHubUpdater.downloadInstaller(asset) { downloadProgress = it }
                                        GitHubUpdater.install(file)
                                    } catch (ex: CancellationException) {
                                        throw ex
                                    } catch (ex: Exception) {
                                        downloadProgress = null
                                        error = "Download failed: ${ex.message ?: ex.toString()}"
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseNotes(releases: List<GitHubRelease>) {
    if (releases.isEmpty()) {
        Text("No release notes found.", style = MaterialTheme.typography.body1)
        return
    }

    SelectionContainer {
        ScrollableLazyColumn(modifier = Modifier.fillMaxSize()) {
            items(releases) { release ->
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        release.tagName,
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Text(
                        releaseNotesText(release.body),
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/**
 * Turns GitHub's generated markdown ("## What's Changed", "* Title by @user in <pull request url>",
 * "**Full Changelog**: ...") into a plain list of changes.
 */
private fun releaseNotesText(body: String?): String {
    val lines = body.orEmpty().lines()
        .map { it.trim() }
        .filterNot { it.isEmpty() || it.startsWith("#") || it.startsWith("**Full Changelog**") }
        .map { line ->
            line.replaceFirst(Regex("""^[*-] """), "• ")
                .replace(pullRequestAuthorRegex, " (#$1)")
        }

    return lines.joinToString("\n").ifEmpty { "No details." }
}
