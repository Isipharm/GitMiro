package com.hugodehu.gitmiro.data.git.branches

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IDeleteLocallyRemoteBranchesGitAction
import javax.inject.Inject

class DeleteLocallyRemoteBranchesGitAction @Inject constructor(private val jgit: JGit) :
    IDeleteLocallyRemoteBranchesGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        branches: List<String>
    ) = jgit.provide(repositoryPath) { git ->
        git
            .branchDelete()
            .setBranchNames(*branches.toTypedArray())
            .setForce(true)
            .call()
    }
}