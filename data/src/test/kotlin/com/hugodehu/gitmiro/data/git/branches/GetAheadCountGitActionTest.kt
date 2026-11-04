package com.hugodehu.gitmiro.data.git.branches

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.git.WindowsFs
import com.hugodehu.gitmiro.domain.ShellManager
import com.hugodehu.gitmiro.domain.errors.Either
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GetAheadCountGitActionTest {
    @TempDir
    lateinit var tempDir: File

    private val getAheadCount = GetAheadCountGitAction(JGit { WindowsFs(ShellManager()) })

    private fun Git.commitFile(name: String) {
        File(repository.workTree, name).writeText(name)
        add().addFilepattern(".").call()
        commit().setMessage(name).setAuthor("Test", "t@e.com").setCommitter("Test", "t@e.com").setSign(false).call()
    }

    @Test
    fun `ahead count is 0 without upstream and N after local commits`() = runBlocking {
        val remoteDir = File(tempDir, "remote")
        val localDir = File(tempDir, "local")

        Git.init().setDirectory(remoteDir).setInitialBranch("main").call().use { remote ->
            remote.commitFile("base.txt")

            Git.init().setDirectory(localDir).setInitialBranch("main").call().use { local ->
                local.repository.config.apply { setBoolean("core", null, "autocrlf", false); save() }
                local.commitFile("base.txt")

                assertEquals(0, (getAheadCount(localDir.absolutePath) as Either.Ok).value)

                // Configure main to track origin/main, which points at the first local commit
                local.repository.config.apply {
                    setString("remote", "origin", "url", remoteDir.toURI().toString())
                    setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*")
                    setString("branch", "main", "remote", "origin")
                    setString("branch", "main", "merge", "refs/heads/main")
                    save()
                }
                val head = local.repository.resolve("HEAD")
                local.repository.updateRef("refs/remotes/origin/main").apply {
                    setNewObjectId(head)
                    update()
                }

                assertEquals(0, (getAheadCount(localDir.absolutePath) as Either.Ok).value)

                local.commitFile("a.txt")
                local.commitFile("b.txt")

                assertEquals(2, (getAheadCount(localDir.absolutePath) as Either.Ok).value)
            }
        }
    }
}
