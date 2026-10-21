package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Branch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

typealias IsMultiStep = Boolean

interface IRebaseBranchGitAction {
    suspend operator fun invoke(repositoryPath: String, branch: Branch): Either<IsMultiStep, GitError>
}