package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IResolveConflictWithSideGitAction
import com.hugodehu.gitmiro.domain.models.ConflictSide
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class ResolveConflictWithSideUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val resolveConflictWithSideGitAction: IResolveConflictWithSideGitAction,
) {
    operator fun invoke(filePath: String, side: ConflictSide) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.ResolveConflict,
            dataToRefresh = arrayOf(DataToRefresh.STATUS),
        ) { repositoryPath ->
            resolveConflictWithSideGitAction(repositoryPath, filePath, side)
        }
    }
}
