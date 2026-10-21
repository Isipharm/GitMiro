package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.StatusEntry
import org.eclipse.jgit.api.Git

interface IDeleteFileGitAction {
    suspend operator fun invoke(repositoryPath: String, filePath: String): Either<Unit, GitError>
}