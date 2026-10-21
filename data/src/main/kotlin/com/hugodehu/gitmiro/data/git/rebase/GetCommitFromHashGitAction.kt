package com.hugodehu.gitmiro.data.git.rebase

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.mappers.JGitCommitMapper
import com.hugodehu.gitmiro.domain.interfaces.IGetCommitFromHashGitAction
import org.eclipse.jgit.lib.ObjectId
import javax.inject.Inject

class GetCommitFromHashGitAction @Inject constructor(
    private val commitMapper: JGitCommitMapper,
    private val jgit: JGit,
) : IGetCommitFromHashGitAction {
    override suspend operator fun invoke(repositoryPath: String, commitHash: String) =
        jgit.provide(repositoryPath) { git ->
            git
                .repository
                .parseCommit(ObjectId.fromString(commitHash))
                ?.let { commit -> commitMapper.toDomain(commit) }
        }
}