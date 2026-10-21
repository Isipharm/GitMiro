package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.models.Branch
import com.hugodehu.gitmiro.domain.models.TrackingBranch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IGetTrackingBranchGitAction {
    suspend operator fun invoke(repositoryPath: String, branch: Branch): Either<TrackingBranch?, GitError>
    suspend operator fun invoke(repositoryPath: String, refName: String): Either<TrackingBranch?, GitError>
}