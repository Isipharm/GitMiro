package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IFetchAllRemotesGitAction
import com.hugodehu.gitmiro.domain.models.Remote
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class FetchRemotesUseCase @Inject constructor(
    private val fetchAllRemotesGitAction: IFetchAllRemotesGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(specificRemote: Remote? = null) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.Fetch,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            fetchAllRemotesGitAction(repositoryPath, specificRemote)
        }
    }
}