package com.hugodehu.gitmiro.data.git.branches

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.bind
import com.hugodehu.gitmiro.domain.errors.either
import com.hugodehu.gitmiro.domain.interfaces.IGetCurrentBranchGitAction
import com.hugodehu.gitmiro.domain.models.Branch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

/**
 * Returns the current branch in [Ref]. If the repository is new, the current branch will be null.
 */
class GetCurrentBranchGitAction @Inject constructor(
    private val getBranchesGitAction: GetBranchesGitAction,
    private val jgit: JGit,
) : IGetCurrentBranchGitAction {
    override suspend operator fun invoke(git: Git): Either<Branch?, AppError> {
        return invoke(git.repository.directory.absolutePath)
    }

    override suspend operator fun invoke(path: String) = either {
        jgit.provide(path) { git ->
            val branchList = getBranchesGitAction(git).bind()
            val branchName =
                git
                    .repository
                    .fullBranch

            var branchFound = branchList.firstOrNull {
                it.name == branchName
            }

            if (branchFound == null) {
                branchFound = branchList.firstOrNull {
                    it.hash == branchName // Try to get the HEAD
                }
            }

            branchFound
        }
    }
}