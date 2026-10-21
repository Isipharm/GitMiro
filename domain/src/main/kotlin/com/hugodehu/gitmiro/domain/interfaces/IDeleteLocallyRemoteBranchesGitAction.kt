package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import org.eclipse.jgit.api.Git

interface IDeleteLocallyRemoteBranchesGitAction {
    suspend operator fun invoke(repositoryPath: String, branches: List<String>): Either<List<String>, GitError>
}