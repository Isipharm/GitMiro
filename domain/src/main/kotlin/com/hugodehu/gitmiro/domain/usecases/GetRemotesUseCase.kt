package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.GitError
import com.hugodehu.gitmiro.domain.errors.bind
import com.hugodehu.gitmiro.domain.errors.either
import com.hugodehu.gitmiro.domain.interfaces.IGetRemoteBranchesGitAction
import com.hugodehu.gitmiro.domain.interfaces.IGetRemotesGitAction
import com.hugodehu.gitmiro.domain.models.RemoteInfo
import javax.inject.Inject

class GetRemotesUseCase @Inject constructor(
    private val getRemotesGitAction: IGetRemotesGitAction,
    private val getRemoteBranchesGitAction: IGetRemoteBranchesGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke() = either {
        useCaseExecutor.execute { repositoryPath ->
            getRemoteInfoList(repositoryPath)
        }
    }
    private suspend fun getRemoteInfoList(repositoryPath: String) = either<List<RemoteInfo>, GitError> {
        val allRemoteBranches = getRemoteBranchesGitAction(repositoryPath).bind()
        val remoteInfoList = getRemotesGitAction(repositoryPath).bind()

        val remotes = remoteInfoList.map { remote ->
            val remoteBranches = allRemoteBranches.filter { branch ->
                branch.name.startsWith("refs/remotes/${remote.name}")
            }
            RemoteInfo(remote, remoteBranches)
        }

        Either.Ok(remotes)
    }
}