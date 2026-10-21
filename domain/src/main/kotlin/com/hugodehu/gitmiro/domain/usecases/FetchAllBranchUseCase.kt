package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IFetchAllRemotesGitAction
import com.hugodehu.gitmiro.domain.models.Remote
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class FetchAllBranchUseCase @Inject constructor(
    private val fetchAllGitAction: IFetchAllRemotesGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(specificRemote: Remote? = null) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.Fetch,
            refreshEvenIfFailed = true,
            dataToRefresh = arrayOf(DataToRefresh.LOG),
        ) { repositoryPath ->
            fetchAllGitAction(repositoryPath, specificRemote)
        }
    }
}