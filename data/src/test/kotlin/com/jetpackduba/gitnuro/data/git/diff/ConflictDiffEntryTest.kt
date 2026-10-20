package com.jetpackduba.gitnuro.data.git.diff

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File

class ConflictDiffEntryTest {
    @TempDir
    lateinit var repoDir: File

    @Test
    fun `diff goes from the current version to the incoming one`() {
        Git.init().setDirectory(repoDir).setInitialBranch("main").call().use { git ->
            val file = File(repoDir, "file.txt")
            fun commit(content: String) {
                file.writeText(content)
                git.add().addFilepattern(".").call()
                git.commit().setMessage(content).setAuthor("Test", "t@e.com").setCommitter("Test", "t@e.com")
                    .setSign(false).call()
            }

            commit("base\n")
            git.checkout().setCreateBranch(true).setName("other").call()
            commit("incoming\n")
            git.checkout().setName("main").call()
            commit("current\n")
            git.merge().include(git.repository.resolve("other")).call()

            val entry = ConflictDiffEntry.fromIndex(git.repository.readDirCache(), "file.txt")!!
            val patch = ByteArrayOutputStream().also { output ->
                DiffFormatter(output).use { it.setRepository(git.repository); it.format(entry) }
            }.toString()

            assertEquals(DiffEntry.ChangeType.MODIFY, entry.changeType)
            assertTrue(patch.contains("-current"), patch)
            assertTrue(patch.contains("+incoming"), patch)
        }
    }

    @Test
    fun `no entry when the file is not in conflict`() {
        Git.init().setDirectory(repoDir).call().use { git ->
            File(repoDir, "file.txt").writeText("content\n")
            git.add().addFilepattern(".").call()

            assertNull(ConflictDiffEntry.fromIndex(git.repository.readDirCache(), "file.txt"))
        }
    }
}
