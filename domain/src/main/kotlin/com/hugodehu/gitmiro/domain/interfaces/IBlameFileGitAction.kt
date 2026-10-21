package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import org.eclipse.jgit.blame.BlameResult

interface IBlameFileGitAction {
    suspend operator fun invoke(repositoryPath: String, filePath: String): Either<BlameResult, GitError>
}