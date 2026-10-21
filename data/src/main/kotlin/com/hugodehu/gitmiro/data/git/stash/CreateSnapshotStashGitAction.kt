package com.hugodehu.gitmiro.data.git.stash

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.mappers.JGitCommitMapper
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.interfaces.ICreateSnapshotStashGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import javax.inject.Inject

class CreateSnapshotStashGitAction @Inject constructor(
    private val commitMapper: JGitCommitMapper,
    private val jgit: JGit,
) : ICreateSnapshotStashGitAction {
    override suspend fun invoke(
        repositoryPath: String,
        message: String,
        includeUntracked: Boolean,
    ): Either<Commit?, GitError> = jgit.provide(repositoryPath) { git ->
        SnapshotStashCreateCommand(
            repository = git.repository,
            workingDirectoryMessage = message,
            includeUntracked = true
        )
            .call()
            ?.let { commitMapper.toDomain(it) }
    }
}