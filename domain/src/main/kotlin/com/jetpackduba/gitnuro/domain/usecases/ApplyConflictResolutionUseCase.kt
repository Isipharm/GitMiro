package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IWriteConflictResolutionGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class ApplyConflictResolutionUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val writeConflictResolutionGitAction: IWriteConflictResolutionGitAction,
) {
    operator fun invoke(filePath: String, content: ByteArray) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.ResolveConflict,
            dataToRefresh = arrayOf(DataToRefresh.STATUS),
        ) { repositoryPath ->
            writeConflictResolutionGitAction(repositoryPath, filePath, content)
        }
    }
}
