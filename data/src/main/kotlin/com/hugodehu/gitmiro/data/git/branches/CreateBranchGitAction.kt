package com.hugodehu.gitmiro.data.git.branches

import com.hugodehu.gitmiro.common.extensions.runIfNotNull
import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.errors.CreateBranchError
import com.hugodehu.gitmiro.domain.errors.GenericError
import com.hugodehu.gitmiro.domain.errors.mapErr
import com.hugodehu.gitmiro.domain.interfaces.ICreateBranchGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import javax.inject.Inject

class CreateBranchGitAction @Inject constructor(
    private val jgit: JGit,
) : ICreateBranchGitAction {
    override suspend operator fun invoke(repositoryPath: String, branchName: String, targetCommit: Commit?) =
        jgit.provide(repositoryPath) { git ->
            git
                .checkout()
                .setCreateBranch(true)
                .setName(branchName)
                .runIfNotNull(targetCommit) { commit ->
                    setStartPoint(commit.hash)
                }
                .call()

            Unit
        }.mapErr {
            if (it is GenericError) {
                if (it.message == "Ref $branchName already exists") {
                    CreateBranchError.BranchAlreadyExists(branchName)
                } else if (it.message == "Branch name $branchName is not allowed") {
                    CreateBranchError.NameNotAllowed(branchName)
                } else {
                    it
                }
            } else {
                it
            }
        }
}