package com.hugodehu.gitmiro.data.git.stash

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.mappers.JGitCommitMapper
import com.hugodehu.gitmiro.domain.interfaces.IGetStashListGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class GetStashListGitAction @Inject constructor(
    private val commitMapper: JGitCommitMapper,
    private val jgit: JGit,
) : IGetStashListGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        invoke(git)
            .map { commitMapper.toDomain(it) }
    }

    suspend operator fun invoke(git: Git): List<RevCommit> = withContext(Dispatchers.IO) {
        return@withContext git
            .stashList()
            .call()
            .toList()
    }
}
