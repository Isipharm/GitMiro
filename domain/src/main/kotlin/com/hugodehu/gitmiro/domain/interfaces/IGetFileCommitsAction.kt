package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Commit
import org.eclipse.jgit.api.Git

interface IGetFileCommitsAction {
    suspend operator fun invoke(
        repositoryPath: String,
        filePath: String
    ): Either<List<Commit>, GitError>
}