package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.git.WindowsFs
import com.hugodehu.gitmiro.domain.ShellManager
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.models.DiscardScope
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DiscardChangesGitActionTest {
    @TempDir
    lateinit var repoDir: File

    private val jgit = JGit { WindowsFs(ShellManager()) }
    private val discardChanges = DiscardChangesGitAction(jgit)

    private fun file(name: String) = File(repoDir, name)

    /**
     * staged.txt: staged change. unstaged.txt: unstaged change. both.txt: staged change, then another unstaged one.
     * new-staged.txt: new file, staged. untracked.txt: new file. ignored.log: ignored file.
     */
    private fun initRepoWithChanges(): Git {
        val git = Git.init().setDirectory(repoDir).setInitialBranch("main").call()
        git.repository.config.apply {
            setBoolean("core", null, "autocrlf", false)
            save()
        }

        for (name in listOf("staged.txt", "unstaged.txt", "both.txt")) {
            file(name).writeText("committed\n")
        }
        file(".gitignore").writeText("*.log\n")
        git.add().addFilepattern(".").call()
        git.commit().setMessage("base").setAuthor("Test", "t@e.com").setCommitter("Test", "t@e.com")
            .setSign(false).call()

        file("staged.txt").writeText("staged\n")
        file("both.txt").writeText("staged\n")
        file("new-staged.txt").writeText("new\n")
        git.add().addFilepattern("staged.txt").addFilepattern("both.txt").addFilepattern("new-staged.txt").call()

        file("unstaged.txt").writeText("unstaged\n")
        file("both.txt").writeText("unstaged\n")
        file("untracked.txt").writeText("untracked\n")
        file("ignored.log").writeText("ignored\n")

        return git
    }

    @Test
    fun `discarding all changes goes back to the last commit`() = runBlocking {
        initRepoWithChanges().use { git ->
            assertTrue(discardChanges(repoDir.absolutePath, DiscardScope.ALL) is Either.Ok)

            for (name in listOf("staged.txt", "unstaged.txt", "both.txt")) {
                assertEquals("committed\n", file(name).readText(), name)
            }
            assertFalse(file("new-staged.txt").exists())
            assertFalse(file("untracked.txt").exists())
            assertTrue(file("ignored.log").exists(), "ignored files are kept")
            assertTrue(git.status().call().isClean)
        }
    }

    @Test
    fun `discarding staged changes keeps the other unstaged changes`() = runBlocking {
        initRepoWithChanges().use { git ->
            assertTrue(discardChanges(repoDir.absolutePath, DiscardScope.STAGED) is Either.Ok)

            assertEquals("committed\n", file("staged.txt").readText())
            assertEquals("committed\n", file("both.txt").readText(), "the whole file goes back to the last commit")
            assertFalse(file("new-staged.txt").exists())
            assertEquals("unstaged\n", file("unstaged.txt").readText())
            assertTrue(file("untracked.txt").exists())
            assertTrue(file("ignored.log").exists())

            val status = git.status().call()
            assertTrue(status.added.isEmpty() && status.changed.isEmpty() && status.removed.isEmpty(), "$status")
        }
    }

    @Test
    fun `discarding unstaged changes keeps the staged changes`() = runBlocking {
        initRepoWithChanges().use { git ->
            assertTrue(discardChanges(repoDir.absolutePath, DiscardScope.UNSTAGED) is Either.Ok)

            assertEquals("committed\n", file("unstaged.txt").readText())
            assertEquals("staged\n", file("both.txt").readText(), "back to the staged version")
            assertFalse(file("untracked.txt").exists())
            assertEquals("staged\n", file("staged.txt").readText())
            assertTrue(file("new-staged.txt").exists())
            assertTrue(file("ignored.log").exists())

            val status = git.status().call()
            assertEquals(setOf("staged.txt", "both.txt"), status.changed)
            assertEquals(setOf("new-staged.txt"), status.added)
            assertTrue(status.modified.isEmpty() && status.untracked.isEmpty(), "$status")
        }
    }
}
