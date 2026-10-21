package com.hugodehu.gitmiro.data.git.workspace

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.git.diff.entriesForPath
import com.hugodehu.gitmiro.domain.errors.GenericError
import com.hugodehu.gitmiro.domain.errors.raiseError
import com.hugodehu.gitmiro.domain.interfaces.IResolveConflictWithSideGitAction
import com.hugodehu.gitmiro.domain.models.ConflictSide
import org.eclipse.jgit.api.CheckoutCommand
import org.eclipse.jgit.dircache.DirCacheEntry
import javax.inject.Inject

/**
 * Resolves a conflicting file by keeping one side entirely, like `git checkout --ours/--theirs` followed by `git add`.
 * If the chosen side deleted the file (delete/modify conflict), the file is removed instead.
 */
class ResolveConflictWithSideGitAction @Inject constructor(
    private val jgit: JGit,
) : IResolveConflictWithSideGitAction {
    override suspend fun invoke(
        repositoryPath: String,
        filePath: String,
        side: ConflictSide,
    ) = jgit.provide(repositoryPath) { git ->
        val stages = git.repository.readDirCache().entriesForPath(filePath).map { it.stage }

        // Without this check, a non conflicting file would be deleted by the "rm" below
        if (stages.none { it != DirCacheEntry.STAGE_0 }) {
            raiseError(GenericError("\"$filePath\" is not in conflict"))
        }

        val (stage, checkoutStage) = when (side) {
            ConflictSide.CURRENT -> DirCacheEntry.STAGE_2 to CheckoutCommand.Stage.OURS
            ConflictSide.INCOMING -> DirCacheEntry.STAGE_3 to CheckoutCommand.Stage.THEIRS
        }

        if (stage in stages) {
            git.checkout()
                .setStage(checkoutStage)
                .addPath(filePath)
                .call()

            git.add()
                .addFilepattern(filePath)
                .call()
        } else {
            git.rm()
                .addFilepattern(filePath)
                .call()
        }

        Unit
    }
}
