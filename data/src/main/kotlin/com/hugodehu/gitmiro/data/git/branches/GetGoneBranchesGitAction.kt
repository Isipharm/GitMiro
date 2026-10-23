package com.hugodehu.gitmiro.data.git.branches

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.mappers.JGitBranchMapper
import com.hugodehu.gitmiro.domain.interfaces.IGetGoneBranchesGitAction
import org.eclipse.jgit.lib.BranchConfig
import org.eclipse.jgit.lib.Repository
import javax.inject.Inject

class GetGoneBranchesGitAction @Inject constructor(
    private val jGitBranchMapper: JGitBranchMapper,
    private val jgit: JGit,
) : IGetGoneBranchesGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        val repository = git.repository
        val currentBranch = repository.fullBranch

        git
            .branchList()
            .call()
            .filter { ref ->
                if (ref.name == currentBranch) {
                    return@filter false
                }

                val branchConfig = BranchConfig(repository.config, Repository.shortenRefName(ref.name))
                // Null when the branch has no upstream, or tracks another local branch
                val remoteTrackingRef = branchConfig.remoteTrackingBranch

                remoteTrackingRef != null && repository.exactRef(remoteTrackingRef) == null
            }
            .mapNotNull { jGitBranchMapper.toDomain(it) }
    }
}
