package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IUnstageByDirectoryGitAction {
    suspend operator fun invoke(repositoryPath: String, dir: String): Either<Unit, GitError>
}