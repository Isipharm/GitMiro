package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.AuthorInfo
import org.eclipse.jgit.api.Git

interface ILoadAuthorGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<AuthorInfo, GitError>
}