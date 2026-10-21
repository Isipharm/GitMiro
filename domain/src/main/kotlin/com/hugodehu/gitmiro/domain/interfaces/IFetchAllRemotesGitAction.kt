package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Remote

interface IFetchAllRemotesGitAction {
    suspend operator fun invoke(repositoryPath: String, specificRemote: Remote? = null): Either<Unit, GitError>
}