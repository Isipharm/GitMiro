package com.hugodehu.gitmiro.data.git.branches

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.domain.interfaces.IGetAheadCountGitAction
import org.eclipse.jgit.lib.BranchTrackingStatus
import javax.inject.Inject

class GetAheadCountGitAction @Inject constructor(
    private val jgit: JGit,
) : IGetAheadCountGitAction {
    override suspend operator fun invoke(repositoryPath: String) =
        jgit.provide(repositoryPath) { git ->
            val repository = git.repository
            val branch = repository.fullBranch
            if (branch == null || !branch.startsWith("refs/heads/")) {
                0
            } else {
                BranchTrackingStatus.of(repository, branch)?.aheadCount ?: 0
            }
        }
}
