package com.hugodehu.gitmiro.data.git.branches

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.git.WindowsFs
import com.hugodehu.gitmiro.data.mappers.JGitBranchMapper
import com.hugodehu.gitmiro.domain.ShellManager
import com.hugodehu.gitmiro.domain.errors.Either
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GetGoneBranchesGitActionTest {
    @TempDir
    lateinit var tempDir: File

    private val jgit = JGit { WindowsFs(ShellManager()) }
    private val getGoneBranches = GetGoneBranchesGitAction(JGitBranchMapper(), jgit)

    @Test
    fun `branches whose upstream was deleted on the remote are gone`() = runBlocking {
        val remoteDir = File(tempDir, "remote")
        val localDir = File(tempDir, "local")

        Git.init().setDirectory(remoteDir).setInitialBranch("main").call().use { remote ->
            File(remoteDir, "file.txt").writeText("base\n")
            remote.add().addFilepattern(".").call()
            remote.commit().setMessage("base").setAuthor("Test", "t@e.com").setCommitter("Test", "t@e.com")
                .setSign(false).call()

            for (branch in listOf("squashed", "still-open", "current-gone")) {
                remote.branchCreate().setName(branch).call()
            }

            Git.cloneRepository().setURI(remoteDir.toURI().toString()).setDirectory(localDir).call().use { local ->
                for (branch in listOf("squashed", "still-open", "current-gone")) {
                    local.branchCreate()
                        .setName(branch)
                        .setStartPoint("origin/$branch")
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                        .call()
                }
                local.branchCreate().setName("local-only").call()
                local.checkout().setName("current-gone").call()

                // The pull requests of "squashed" and "current-gone" are merged and their branches deleted
                remote.branchDelete().setBranchNames("squashed", "current-gone").setForce(true).call()
                local.fetch().setRemoveDeletedRefs(true).call()

                val result = getGoneBranches(localDir.absolutePath)

                // "current-gone" is gone too, but it's checked out so it can't be deleted
                assertEquals(listOf("squashed"), (result as Either.Ok).value.map { it.simpleName })
            }
        }
    }
}
