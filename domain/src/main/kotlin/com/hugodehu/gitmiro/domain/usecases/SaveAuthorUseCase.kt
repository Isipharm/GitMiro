package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.ISaveAuthorGitAction
import com.hugodehu.gitmiro.domain.models.AuthorInfo
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class SaveAuthorUseCase @Inject constructor(
    private val saveAuthorGitAction: ISaveAuthorGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(authorInfo: AuthorInfo) {
        // TODO This should be "execute" and the UI should handle the error
        useCaseExecutor.executeLaunch(
            taskType = TaskType.SaveAuthor,
            dataToRefresh = arrayOf(DataToRefresh.GIT_CONFIG),
        ) { repositoryPath ->
            saveAuthorGitAction(repositoryPath, authorInfo)
        }
    }
}