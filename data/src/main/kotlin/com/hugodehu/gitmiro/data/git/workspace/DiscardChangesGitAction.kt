package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IDiscardChangesGitAction
import com.hugodehu.gitmiro.domain.models.DiscardScope
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import java.io.File
import javax.inject.Inject

class DiscardChangesGitAction @Inject constructor(
    private val jgit: JGit,
) : IDiscardChangesGitAction {
    override suspend operator fun invoke(repositoryPath: String, scope: DiscardScope) = jgit.provide(repositoryPath) { git ->
        when (scope) {
            DiscardScope.ALL -> {
                git.reset().setMode(ResetCommand.ResetType.HARD).call()
                deleteUntrackedFiles(git)
            }

            DiscardScope.UNSTAGED -> {
                git.checkout().setAllPaths(true).call()
                deleteUntrackedFiles(git)
            }

            DiscardScope.STAGED -> discardStaged(git)
        }
    }

    private fun discardStaged(git: Git) {
        val status = git.status().call()
        // Not in the last commit: unstaging them would leave them as untracked files, so they are deleted
        val newFiles = status.added
        val changedFiles = status.changed + status.removed

        if (newFiles.isEmpty() && changedFiles.isEmpty()) {
            return
        }

        git.reset()
            .apply { (newFiles + changedFiles).forEach { addPath(it) } }
            .call()

        if (changedFiles.isNotEmpty()) {
            git.checkout()
                .apply { changedFiles.forEach { addPath(it) } }
                .call()
        }

        val workTree = git.repository.workTree
        newFiles.forEach { File(workTree, it).delete() }
    }

    /**
     * Like `git clean -fd`: ignored files and nested repositories are kept
     */
    private fun deleteUntrackedFiles(git: Git) {
        git.clean().setCleanDirectories(true).call()
    }
}
