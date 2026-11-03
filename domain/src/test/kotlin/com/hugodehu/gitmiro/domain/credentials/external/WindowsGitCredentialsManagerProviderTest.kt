package com.hugodehu.gitmiro.domain.credentials.external

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class WindowsGitCredentialsManagerProviderTest {
    @TempDir
    lateinit var gitDir: File

    private val provider = WindowsGitCredentialsManagerProvider()

    private fun createFile(path: String) = File(gitDir, path).apply {
        parentFile.mkdirs()
        createNewFile()
    }

    @Test
    fun `finds GCM in ucrt64 when git is in cmd`() {
        val git = createFile("cmd/git.exe")
        createFile("mingw64/bin/other.exe")
        val gcm = createFile("ucrt64/bin/git-credential-manager.exe")

        assertEquals(gcm.absolutePath, provider.findInGitInstallation(git))
    }

    @Test
    fun `finds GCM in a sibling environment when git is in an environment's bin`() {
        val git = createFile("mingw64/bin/git.exe")
        val gcm = createFile("ucrt64/bin/git-credential-manager.exe")

        assertEquals(gcm.absolutePath, provider.findInGitInstallation(git))
    }

    @Test
    fun `returns null when GCM is not installed`() {
        val git = createFile("cmd/git.exe")
        createFile("ucrt64/bin/git.exe")

        assertNull(provider.findInGitInstallation(git))
    }
}
