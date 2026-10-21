package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IDeleteLocallyRemoteBranchesGitAction
import com.hugodehu.gitmiro.domain.interfaces.IDeleteRemoteGitAction
import com.hugodehu.gitmiro.domain.models.RemoteInfo
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class DeleteRemoteInfoUseCase @Inject constructor(
    private val deleteRemoteGitAction: IDeleteRemoteGitAction,
    private val deleteLocallyRemoteBranchesGitAction: IDeleteLocallyRemoteBranchesGitAction,
    private val useCaseExecutor: UseCaseExecutor,

    ) {
    operator fun invoke(remoteInfo: RemoteInfo) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.DeleteRemote,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            deleteRemoteGitAction(repositoryPath, remoteInfo.remote.name)

            val remoteBranchesToDelete = remoteInfo.branchesList

            deleteLocallyRemoteBranchesGitAction(repositoryPath, remoteBranchesToDelete.map { it.name })
        }
    }
}
