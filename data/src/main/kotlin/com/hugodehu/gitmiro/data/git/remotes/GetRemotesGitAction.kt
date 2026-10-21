package com.hugodehu.gitmiro.data.git.remotes

import com.hugodehu.gitmiro.data.git.JGit
import com.hugodehu.gitmiro.data.mappers.RemoteConfigToRemoteMapper
import com.hugodehu.gitmiro.domain.interfaces.IGetRemotesGitAction
import javax.inject.Inject

class GetRemotesGitAction @Inject constructor(
    private val remoteMapper: RemoteConfigToRemoteMapper,
    private val jgit: JGit,
) : IGetRemotesGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        git
            .remoteList()
            .call()
            .map { remoteConfig -> remoteMapper.toDomain(remoteConfig) }
    }
}