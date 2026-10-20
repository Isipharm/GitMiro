package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.common.OS
import com.jetpackduba.gitnuro.common.currentOs
import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.domain.ShellManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "RevealFileInSystemUseCase"

/**
 * Opens the file explorer with the file selected.
 * If the file doesn't exist (deleted or only present in an older commit), the closest existing parent directory is opened.
 */
class RevealFileInSystemUseCase @Inject constructor(
    /**
     * Same as [OpenPathInSystemUseCase], commands must run without the flatpak prefix
     */
    private val shellManager: ShellManager,
    private val openPathInSystemUseCase: OpenPathInSystemUseCase,
) {
    suspend operator fun invoke(file: File) = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            val existingParent = generateSequence(file.parentFile) { it.parentFile }.firstOrNull { it.isDirectory }

            if (existingParent != null) {
                openPathInSystemUseCase(existingParent.absolutePath)
            }

            return@withContext
        }

        val path = file.absolutePath

        val isRevealed = when (currentOs) {
            // Explorer returns a non-zero exit code even on success, so it's not checked
            OS.WINDOWS -> runCommand(listOf("explorer", "/select,", path), waitForSuccess = false)
            OS.MAC -> runCommand(listOf("open", "-R", path), waitForSuccess = false)
            // Supported by most file managers (Nautilus, Dolphin, Nemo, Thunar...).
            // ponytail: dbus-send splits array items by commas, so paths containing commas select the wrong item
            OS.LINUX -> runCommand(
                listOf(
                    "dbus-send",
                    "--session",
                    "--print-reply",
                    "--dest=org.freedesktop.FileManager1",
                    "--type=method_call",
                    "/org/freedesktop/FileManager1",
                    "org.freedesktop.FileManager1.ShowItems",
                    "array:string:${file.toPath().toUri()}",
                    "string:",
                ),
                waitForSuccess = true,
            )

            else -> false
        }

        if (!isRevealed) {
            openPathInSystemUseCase(file.parentFile.absolutePath)
        }
    }

    private fun runCommand(command: List<String>, waitForSuccess: Boolean): Boolean {
        return try {
            val process = shellManager.runCommandProcess(command)

            if (waitForSuccess) {
                process.waitFor(2, TimeUnit.SECONDS) && process.exitValue() == 0
            } else {
                true
            }
        } catch (ex: Exception) {
            printError(TAG, "Failed to reveal file: ${ex.message}")
            false
        }
    }
}
