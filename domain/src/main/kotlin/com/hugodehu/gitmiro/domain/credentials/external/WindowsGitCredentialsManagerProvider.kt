package com.hugodehu.gitmiro.domain.credentials.external

import java.io.File
import javax.inject.Inject

class WindowsGitCredentialsManagerProvider @Inject constructor() : IGitCredentialsManagerProvider {
    override fun loadPath(): String? {
        return lookForGitCredentialsManager(
            listOf(
                "git-credential-manager.exe",
                "git-credential-manager-core.exe"
            )
        )
    }

    fun lookForGitCredentialsManager(binariesNames: List<String>): String? {
        val credentialManagerBinary = searchForBinariesInPath(binariesNames)

        if (credentialManagerBinary != null) {
            return credentialManagerBinary
        }

        val git = searchForBinariesInPath(listOf("git.exe")) ?: return null

        return findInGitInstallation(File(git))
    }

    /**
     * git.exe is either in `<git>/cmd` or in `<git>/<environment>/bin`, and GCM is in `<git>/<environment>/bin`, where
     * the environment is `mingw64` in older Git for Windows versions and `ucrt64` since 2.56.
     */
    fun findInGitInstallation(gitExe: File): String? {
        val baseDir = gitExe.parentFile?.parentFile ?: return null
        val gitDir = if (isEnvironmentDir(baseDir)) baseDir.parentFile else baseDir

        return gitDir.listFiles()
            .orEmpty()
            .filter { isEnvironmentDir(it) }
            .map { File(it, "bin/git-credential-manager.exe") }
            .firstOrNull { it.isFile }
            ?.absolutePath
    }

    private fun isEnvironmentDir(dir: File) = ENVIRONMENT_PREFIXES.any { dir.name.startsWith(it) }

    private companion object {
        val ENVIRONMENT_PREFIXES = listOf("mingw", "ucrt", "clang")
    }

    fun searchForBinariesInPath(binariesNames: List<String>): String? {
        val path = System.getenv("PATH")
        val pathDirs = path.split(";")

        for (dir in pathDirs) {
            for (names in binariesNames) {
                val file = File(dir, names)
                if (file.exists() && file.isFile) {
                    return file.absolutePath
                }
            }
        }

        return null
    }
}