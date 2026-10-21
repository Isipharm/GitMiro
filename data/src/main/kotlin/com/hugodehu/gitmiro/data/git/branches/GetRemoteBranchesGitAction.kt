package com.hugodehu.gitmiro.data.git.branches

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.mappers.JGitBranchMapper
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.interfaces.IGetRemoteBranchesGitAction
import com.hugodehu.gitmiro.domain.models.Branch
import org.eclipse.jgit.api.ListBranchCommand
import javax.inject.Inject

class GetRemoteBranchesGitAction @Inject constructor(
    private val jGitBranchMapper: JGitBranchMapper,
    private val jgit: JGit,
) : IGetRemoteBranchesGitAction {
    override suspend operator fun invoke(repositoryPath: String): Either<List<Branch>, GitError> {
        return jgit.provide(repositoryPath) { git ->
            git
                .branchList()
                .setListMode(ListBranchCommand.ListMode.REMOTE)
                .call()
                .mapNotNull { jGitBranchMapper.toDomain(it) }
        }
    }
}