package com.hugodehu.gitmiro.data.git.branches

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IDeleteBranchGitAction
import com.hugodehu.gitmiro.domain.models.Branch
import javax.inject.Inject

class DeleteBranchGitAction @Inject constructor(private val jgit: JGit) : IDeleteBranchGitAction {
    override suspend operator fun invoke(repositoryPath: String, branch: Branch) = jgit.provide(repositoryPath) { git ->
        git
            .branchDelete()
            .setBranchNames(branch.name)
            .setForce(true) // TODO Should it be forced?
            .call()

        Unit
    }
}