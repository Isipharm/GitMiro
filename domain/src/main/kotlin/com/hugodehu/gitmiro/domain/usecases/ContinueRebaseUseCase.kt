package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.bind
import com.hugodehu.gitmiro.domain.interfaces.IContinueRebaseGitAction
import com.hugodehu.gitmiro.domain.interfaces.IGetRebaseInteractiveStateGitAction
import com.hugodehu.gitmiro.domain.interfaces.IGetRepositoryStateGitAction
import com.hugodehu.gitmiro.domain.models.Identity
import com.hugodehu.gitmiro.domain.models.RebaseInteractiveState
import com.hugodehu.gitmiro.domain.models.RepositoryState
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

class ContinueRebaseUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val continueRebaseGitAction: IContinueRebaseGitAction,
    private val doCommitUseCase: DoCommitUseCase,
) {
    operator fun invoke(
        message: String,
        isAmendRebaseInteractive: Boolean,
        repositoryState: RepositoryState,
        rebaseInteractiveState: RebaseInteractiveState,
        onIdentityRequest: suspend () -> Identity?,
    ) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.ContinueRebase,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            if (
                repositoryState == RepositoryState.REBASING_INTERACTIVE &&
                rebaseInteractiveState is RebaseInteractiveState.ProcessingCommits &&
                rebaseInteractiveState.isCurrentStepAmenable &&
                isAmendRebaseInteractive
            ) {
                val amendCommitId = rebaseInteractiveState.commitToAmendId

                if (!amendCommitId.isNullOrBlank()) {
                    doCommitUseCase(message, true, onIdentityRequest())
                }
            }

            continueRebaseGitAction(repositoryPath)
        }
    }
}