package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.GenericError
import com.hugodehu.gitmiro.domain.errors.bind
import com.hugodehu.gitmiro.domain.errors.raiseError
import com.hugodehu.gitmiro.domain.interfaces.IGetStashListGitAction
import com.hugodehu.gitmiro.domain.interfaces.IPopStashGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class PopStashUseCase @Inject constructor(
    private val popStashGitAction: IPopStashGitAction,
    private val getStashListGitAction: IGetStashListGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(commit: Commit?) = useCaseExecutor.executeLaunch(
        taskType = TaskType.PopStash,
        refreshEvenIfFailed = true,
        dataToRefresh = arrayOf(DataToRefresh.STATUS, DataToRefresh.LOG, DataToRefresh.STASHES),
    ) { repositoryPath ->
        val stashCommit = commit ?: getStashListGitAction(repositoryPath).bind().firstOrNull()

        if (stashCommit == null) {
            raiseError(GenericError("No stashes found")) // TODO Refactor this to a proper type
        }

        popStashGitAction(repositoryPath, stashCommit)
    }
}