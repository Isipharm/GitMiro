package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IDeleteTagGitAction
import com.hugodehu.gitmiro.domain.models.Tag
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class DeleteTagUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val deleteTagGitAction: IDeleteTagGitAction,
) {
    operator fun invoke(tag: Tag) = useCaseExecutor.executeLaunch(
        taskType = TaskType.DeleteTag,
        dataToRefresh = arrayOf(DataToRefresh.TAGS, DataToRefresh.LOG),
    ) { repositoryPath ->
        deleteTagGitAction(repositoryPath, tag)
    }
}