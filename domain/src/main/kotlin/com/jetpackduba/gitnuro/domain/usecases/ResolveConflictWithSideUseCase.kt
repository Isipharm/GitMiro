package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IResolveConflictWithSideGitAction
import com.jetpackduba.gitnuro.domain.models.ConflictSide
import com.jetpackduba.gitnuro.domain.models.TaskType
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
