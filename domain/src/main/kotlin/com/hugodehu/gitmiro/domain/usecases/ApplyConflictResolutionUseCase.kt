package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IWriteConflictResolutionGitAction
import com.hugodehu.gitmiro.domain.models.TaskType
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
