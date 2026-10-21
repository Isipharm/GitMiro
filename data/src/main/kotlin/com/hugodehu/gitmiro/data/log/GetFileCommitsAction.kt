package com.hugodehu.gitmiro.data.log

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.mappers.JGitCommitMapper
import com.hugodehu.gitmiro.domain.interfaces.IGetFileCommitsAction
import javax.inject.Inject

class GetFileCommitsAction @Inject constructor(
    private val commitMapper: JGitCommitMapper,
    private val jgit: JGit,
) : IGetFileCommitsAction {
    override suspend fun invoke(
        repositoryPath: String,
        filePath: String
    ) = jgit.provide(repositoryPath) { git ->
        git.log()
            .addPath(filePath)
            .call()
            .toList()
            .map { commitMapper.toDomain(it) }
    }
}