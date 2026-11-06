package com.hugodehu.gitmiro.data.git.rebase

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.git.WindowsFs
import com.hugodehu.gitmiro.domain.ShellManager
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GenericError
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ContinueRebaseGitActionTest {
    @TempDir
    lateinit var repoDir: File

    private val action = ContinueRebaseGitAction(JGit { WindowsFs(ShellManager()) })

    private fun write(name: String, content: String) = File(repoDir, name).writeText(content)

    private fun Git.commitAll(message: String) {
        add().addFilepattern(".").call()
        commit().setMessage(message).setSign(false).call()
    }

    /**
     * "feature" changes a.txt then b.txt, "main" changes a.txt: the rebase of "feature" onto "main" stops on a.txt.
     */
    private fun rebaseStoppedOnConflict(): Git {
        val git = Git.init().setDirectory(repoDir).setInitialBranch("main").call()
        git.repository.config.apply { setBoolean("core", null, "autocrlf", false); save() }

        write("a.txt", "base\n"); write("b.txt", "base\n"); git.commitAll("base")
        git.branchCreate().setName("feature").call()
        write("a.txt", "main\n"); git.commitAll("main a")
        git.checkout().setName("feature").call()
        write("a.txt", "feature\n"); git.commitAll("feature a")
        write("b.txt", "feature\n"); git.commitAll("feature b")

        git.rebase().setUpstream(git.repository.resolve("main")).call()
        assertEquals(RepositoryState.REBASING_MERGE, git.repository.repositoryState)

        return git
    }

    @Test
    fun `continue finishes the rebase once the conflict is staged`() = runBlocking {
        rebaseStoppedOnConflict().use { git ->
            write("a.txt", "resolved\n"); git.add().addFilepattern("a.txt").call()

            val result = action(repoDir.absolutePath)

            assertTrue(result is Either.Ok, "$result")
            assertEquals(RepositoryState.SAFE, git.repository.repositoryState)
        }
    }

    @Test
    fun `continue reports the unstaged change that stops the next commit`() = runBlocking {
        rebaseStoppedOnConflict().use { git ->
            write("a.txt", "resolved\n"); git.add().addFilepattern("a.txt").call()
            write("b.txt", "dirty\n")

            val result = action(repoDir.absolutePath)

            assertTrue(result is Either.Err && result.error is GenericError, "$result")
            assertEquals(RepositoryState.REBASING_MERGE, git.repository.repositoryState)
        }
    }

    @Test
    fun `continue with nothing to commit asks to skip`() = runBlocking {
        rebaseStoppedOnConflict().use { git ->
            write("a.txt", "main\n"); git.add().addFilepattern("a.txt").call()

            val result = action(repoDir.absolutePath)

            assertTrue(result is Either.Err && (result.error as GenericError).message.contains("Skip"), "$result")
        }
    }
}
