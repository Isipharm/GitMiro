package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.ICreateTagGitAction
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class CreateTagUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val createTagGitAction: ICreateTagGitAction,
) {
    operator fun invoke(tag: String, revCommit: Commit) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.CreateTag,
            dataToRefresh = arrayOf(DataToRefresh.LOG, DataToRefresh.TAGS),
        ) { repositoryPath ->
            createTagGitAction(repositoryPath, tag, revCommit)
        }
    }
}