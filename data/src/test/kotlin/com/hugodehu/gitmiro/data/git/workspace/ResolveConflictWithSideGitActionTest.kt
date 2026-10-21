package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.git.WindowsFs
import com.hugodehu.gitmiro.domain.ShellManager
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.models.ConflictSide
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ResolveConflictWithSideGitActionTest {
    @TempDir
    lateinit var repoDir: File

    private val action = ResolveConflictWithSideGitAction(JGit { WindowsFs(ShellManager()) })

    private val file get() = File(repoDir, "file.txt")

    @Test
    fun `keep current uses the HEAD version and resolves the conflict`() = runBlocking {
        initRepo().use { git ->
            createConflict(git, mainContent = "main\n", otherContent = "other\n")

            val result = action(repoDir.absolutePath, "file.txt", ConflictSide.CURRENT)

            assertTrue(result is Either.Ok, "$result")
            assertEquals("main\n", file.readText())
            assertTrue(git.status().call().conflicting.isEmpty())
        }
    }

    @Test
    fun `keep incoming uses the merged branch version and resolves the conflict`() = runBlocking {
        initRepo().use { git ->
            createConflict(git, mainContent = "main\n", otherContent = "other\n")

            val result = action(repoDir.absolutePath, "file.txt", ConflictSide.INCOMING)

            assertTrue(result is Either.Ok, "$result")
            assertEquals("other\n", file.readText())
            assertTrue(git.status().call().conflicting.isEmpty())
        }
    }

    @Test
    fun `keeping the side that deleted the file removes it`() = runBlocking {
        initRepo().use { git ->
            createConflict(git, mainContent = null, otherContent = "other\n")

            val result = action(repoDir.absolutePath, "file.txt", ConflictSide.CURRENT)

            assertTrue(result is Either.Ok, "$result")
            assertFalse(file.exists())
            assertTrue(git.status().call().conflicting.isEmpty())
        }
    }

    @Test
    fun `a file that is not in conflict is left untouched`() = runBlocking {
        initRepo().use { git ->
            file.writeText("base\n")
            commitAll(git, "base")

            val result = action(repoDir.absolutePath, "file.txt", ConflictSide.INCOMING)

            assertTrue(result is Either.Err, "$result")
            assertEquals("base\n", file.readText())
        }
    }

    // Line endings must not depend on the global "core.autocrlf" of the machine running the tests
    private fun initRepo(): Git {
        val git = Git.init().setDirectory(repoDir).setInitialBranch("main").call()
        git.repository.config.apply {
            setBoolean("core", null, "autocrlf", false)
            save()
        }
        return git
    }

    /**
     * Creates "file.txt" in a base commit, changes it differently in "main" and "other" (null deletes it),
     * then merges "other" into "main".
     */
    private fun createConflict(git: Git, mainContent: String?, otherContent: String) {
        file.writeText("base\n")
        commitAll(git, "base")

        git.checkout().setCreateBranch(true).setName("other").call()
        file.writeText(otherContent)
        commitAll(git, "other change")

        git.checkout().setName("main").call()
        if (mainContent == null) {
            git.rm().addFilepattern("file.txt").call()
        } else {
            file.writeText(mainContent)
        }
        commitAll(git, "main change")

        val mergeResult = git.merge().include(git.repository.resolve("other")).call()
        assertEquals(MergeResult.MergeStatus.CONFLICTING, mergeResult.mergeStatus)
    }

    private fun commitAll(git: Git, message: String) {
        git.add().addFilepattern(".").call()
        git.commit().setAll(true).setMessage(message).setAuthor("Test", "test@example.com")
            .setCommitter("Test", "test@example.com").setSign(false).call()
    }
}
