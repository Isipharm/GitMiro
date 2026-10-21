package com.hugodehu.gitmiro.data.git.branches

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.mappers.JGitBranchMapper
import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.interfaces.IGetBranchesGitAction
import com.hugodehu.gitmiro.domain.models.Branch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class GetBranchesGitAction @Inject constructor(
    private val jGitBranchMapper: JGitBranchMapper,
    private val jgit: JGit,
) : IGetBranchesGitAction {
    // TODO after refactor remove this overload
    override suspend operator fun invoke(git: Git): Either<List<Branch>, AppError> {
        return invoke(git.repository.directory.absolutePath)
    }

    override suspend operator fun invoke(repository: String): Either<List<Branch>, AppError> {
        return jgit.provide(repository) { git ->
            git
                .branchList()
                .call()
                .mapNotNull { jGitBranchMapper.toDomain(it) }
        }
    }
}