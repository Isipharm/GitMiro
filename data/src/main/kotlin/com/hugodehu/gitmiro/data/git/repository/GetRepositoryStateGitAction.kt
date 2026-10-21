package com.hugodehu.gitmiro.data.git.repository

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.mappers.RepositoryStateMapper
import com.hugodehu.gitmiro.domain.interfaces.IGetRepositoryStateGitAction
import javax.inject.Inject

class GetRepositoryStateGitAction @Inject constructor(
    private val jgit: JGit,
    private val repositoryStateMapper: RepositoryStateMapper,
) : IGetRepositoryStateGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        repositoryStateMapper.toDomain(git.repository.repositoryState)
    }
}