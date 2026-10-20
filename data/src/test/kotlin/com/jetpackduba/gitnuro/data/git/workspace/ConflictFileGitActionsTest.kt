package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.git.WindowsFs
import com.jetpackduba.gitnuro.domain.ShellManager
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.models.ConflictChoice
import com.jetpackduba.gitnuro.domain.models.ConflictChunk
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ConflictFileGitActionsTest {
    @TempDir
    lateinit var repoDir: File

    private val jgit = JGit { WindowsFs(ShellManager()) }
    private val getConflictFile = GetConflictFileGitAction(jgit)
    private val writeConflictResolution = WriteConflictResolutionGitAction(jgit)
    private val file get() = File(repoDir, "file.txt")

    @Test
    fun `splits the file in common and conflicting blocks, then writes the chosen blocks`() = runBlocking {
        // CRLF line endings must be kept as they are
        createConflict(
            base = "1\r\n2\r\n3\r\n4\r\n5\r\n",
            current = "1\r\n2 current\r\n3\r\n4 current\r\n5\r\n",
            incoming = "1\r\n2 incoming\r\n3\r\n4 incoming\r\n5\r\n",
        ).use { git ->
            val conflictFile = getConflictFile(repoDir.absolutePath, "file.txt").okOrNull()!!

            val conflicts = conflictFile.conflicts
            assertEquals(2, conflicts.size)
            assertEquals(listOf("2 current"), conflicts[0].current.map { it.text })
            assertEquals(listOf("2 incoming"), conflicts[0].incoming.map { it.text })
            assertEquals(listOf("1"), (conflictFile.chunks.first() as ConflictChunk.Common).lines.map { it.text })

            val choices = mapOf(
                conflicts[0].index to ConflictChoice(keepCurrent = true),
                conflicts[1].index to ConflictChoice(keepIncoming = true),
            )
            val result = writeConflictResolution(repoDir.absolutePath, "file.txt", conflictFile.resolve(choices))

            assertTrue(result is Either.Ok, "$result")
            assertEquals("1\r\n2 current\r\n3\r\n4 incoming\r\n5\r\n", file.readText())
            assertTrue(git.status().call().conflicting.isEmpty())
        }
    }

    @Test
    fun `no blocks when a side deleted the file`() = runBlocking {
        createConflict(base = "1\n", current = null, incoming = "1 incoming\n").use {
            val result = getConflictFile(repoDir.absolutePath, "file.txt")

            assertTrue(result is Either.Ok, "$result")
            assertNull(result.okOrNull())
        }
    }

    @Test
    fun `writing is refused for a file that is not in conflict`() = runBlocking {
        Git.init().setDirectory(repoDir).call().use { git ->
            file.writeText("content\n")
            commitAll(git, "base")

            val result = writeConflictResolution(repoDir.absolutePath, "file.txt", "other\n".toByteArray())

            assertTrue(result is Either.Err, "$result")
            assertEquals("content\n", file.readText())
        }
    }

    /**
     * Commits [base], changes it to [current] in "main" (null deletes it) and to [incoming] in "other",
     * then merges "other" into "main".
     */
    private fun createConflict(base: String, current: String?, incoming: String): Git {
        val git = Git.init().setDirectory(repoDir).setInitialBranch("main").call()
        // Line endings must not depend on the global "core.autocrlf" of the machine running the tests
        git.repository.config.apply {
            setBoolean("core", null, "autocrlf", false)
            save()
        }

        file.writeText(base)
        commitAll(git, "base")

        git.checkout().setCreateBranch(true).setName("other").call()
        file.writeText(incoming)
        commitAll(git, "incoming")

        git.checkout().setName("main").call()
        if (current == null) git.rm().addFilepattern("file.txt").call() else file.writeText(current)
        commitAll(git, "current")

        git.merge().include(git.repository.resolve("other")).call()
        return git
    }

    private fun commitAll(git: Git, message: String) {
        git.add().addFilepattern(".").call()
        git.commit().setAll(true).setMessage(message).setAuthor("Test", "t@e.com").setCommitter("Test", "t@e.com")
            .setSign(false).call()
    }
}
