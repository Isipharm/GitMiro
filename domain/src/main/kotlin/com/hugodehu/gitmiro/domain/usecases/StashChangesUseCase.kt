package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.bind
import com.hugodehu.gitmiro.domain.extensions.nullIfEmpty
import com.hugodehu.gitmiro.domain.interfaces.IStageUntrackedFileGitAction
import com.hugodehu.gitmiro.domain.interfaces.IStashChangesGitAction
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class StashChangesUseCase @Inject constructor(
    private val stageUntrackedFileGitAction: IStageUntrackedFileGitAction,
    private val stashChangesGitAction: IStashChangesGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(message: String?) = useCaseExecutor.executeLaunch(
        taskType = TaskType.Stash,
        dataToRefresh = arrayOf(DataToRefresh.STATUS, DataToRefresh.STASHES, DataToRefresh.LOG),
    ) { repositoryPath ->
        stageUntrackedFileGitAction(repositoryPath).bind()

        stashChangesGitAction(repositoryPath, message?.nullIfEmpty)
    }
}