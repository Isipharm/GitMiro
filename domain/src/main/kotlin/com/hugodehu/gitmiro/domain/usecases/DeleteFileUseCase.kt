package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.interfaces.IDeleteFileGitAction
import com.hugodehu.gitmiro.domain.interfaces.IDiscardEntriesGitAction
import com.hugodehu.gitmiro.domain.models.StatusEntry
import com.hugodehu.gitmiro.domain.models.TaskType
import java.io.File
import javax.inject.Inject

class DeleteFileUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val deleteFileGitAction: IDeleteFileGitAction,
) {
    operator fun invoke(filePath: String) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.DiscardFile,
            dataToRefresh = arrayOf(DataToRefresh.STATUS, DataToRefresh.LOG),
        ) { repositoryPath ->
            deleteFileGitAction(repositoryPath, filePath)
        }
    }
}
