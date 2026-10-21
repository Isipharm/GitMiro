package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import org.eclipse.jgit.api.RemoteSetUrlCommand
import org.eclipse.jgit.transport.RemoteConfig

interface IUpdateRemoteGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        remoteName: String,
        uri: String,
        uriType: RemoteSetUrlCommand.UriType
    ): Either<RemoteConfig?, GitError>
}