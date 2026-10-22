package com.hugodehu.gitmiro.data.git.stash

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.git.WindowsFs
import com.hugodehu.gitmiro.domain.ShellManager
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.emptyIdentity
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ApplyStashGitActionTest {
    @TempDir
    lateinit var repoDir: File

    private val jgit = JGit { WindowsFs(ShellManager()) }
    private val stashChanges = StashChangesGitAction(jgit)
    private val applyStash = ApplyStashGitAction(jgit)

    private fun file(name: String) = File(repoDir, name)

    @Test
    fun `applying a conflicting stash keeps the new files of the stash`() = runBlocking {
        initRepo().use { git ->
            file("conflict.txt").writeText("base\n")
            commitAll(git, "base")

            // Changes to stash: a modified file, a new staged file and a new untracked file
            file("conflict.txt").writeText("stashed change\n")
            file("new-staged.txt").writeText("new staged\n")
            git.add().addFilepattern("new-staged.txt").call()
            file("new-untracked.txt").writeText("new untracked\n")

            stashChanges(repoDir.absolutePath, "my stash")
            val stash = git.stashList().call().first()

            // main changes the same file, so applying the stash conflicts
            file("conflict.txt").writeText("main change\n")
            commitAll(git, "main change")
            git.checkout().setCreateBranch(true).setName("feature").call()

            val result = applyStash(repoDir.absolutePath, stash.toCommit())

            // The conflict is still reported, so the user resolves it
            assertTrue(result is Either.Err, "$result")

            assertTrue(file("conflict.txt").readText().contains("<<<<<<<"), "conflict.txt should have conflict markers")
            assertTrue(file("new-staged.txt").exists(), "new staged file of the stash disappeared")
            assertTrue(file("new-untracked.txt").exists(), "new untracked file of the stash disappeared")
            assertEquals("new untracked\n", file("new-untracked.txt").readText())
            assertTrue("new-untracked.txt" in git.status().call().untracked, "restored file should stay untracked")
        }
    }

    @Test
    fun `applying a stash without conflict restores its new files`() = runBlocking {
        initRepo().use { git ->
            file("other.txt").writeText("base\n")
            commitAll(git, "base")

            file("other.txt").writeText("stashed change\n")
            file("new-untracked.txt").writeText("new untracked\n")
            stashChanges(repoDir.absolutePath, "my stash")
            val stash = git.stashList().call().first()

            git.checkout().setCreateBranch(true).setName("feature").call()
            applyStash(repoDir.absolutePath, stash.toCommit())

            assertTrue(file("new-untracked.txt").exists(), "new untracked file of the stash disappeared")
        }
    }

    private fun org.eclipse.jgit.revwalk.RevCommit.toCommit() = Commit(
        hash = name,
        message = fullMessage,
        committer = emptyIdentity(),
        author = emptyIdentity(),
        date = commitTime.toLong(),
        parentsHashes = parents.map { it.name },
    )

    private fun initRepo(): Git {
        val git = Git.init().setDirectory(repoDir).setInitialBranch("main").call()
        git.repository.config.apply {
            setBoolean("core", null, "autocrlf", false)
            save()
        }
        return git
    }

    private fun commitAll(git: Git, message: String) {
        git.add().addFilepattern(".").call()
        git.commit().setAll(true).setMessage(message).setAuthor("Test", "t@e.com").setCommitter("Test", "t@e.com")
            .setSign(false).call()
    }
}
