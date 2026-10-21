package com.hugodehu.gitmiro.data.git.log

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.ICheckoutCommitGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import javax.inject.Inject

class CheckoutCommitGitAction @Inject constructor(
    private val jgit: JGit,
) : ICheckoutCommitGitAction {
    override suspend operator fun invoke(repositoryPath: String, commit: Commit) =
        this@CheckoutCommitGitAction(repositoryPath, commit.hash)

    override suspend operator fun invoke(repositoryPath: String, hash: String) = jgit.provide(repositoryPath) { git ->
        git
            .checkout()
            .setName(hash)
            .call()

        Unit
    }
}
