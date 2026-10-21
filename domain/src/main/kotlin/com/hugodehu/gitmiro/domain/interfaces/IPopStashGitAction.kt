package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Commit
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

interface IPopStashGitAction {
    suspend operator fun invoke(repositoryPath: String, stash: Commit): Either<Unit, GitError>
}