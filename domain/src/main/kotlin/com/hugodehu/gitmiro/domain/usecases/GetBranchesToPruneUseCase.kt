package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.bind
import com.hugodehu.gitmiro.domain.errors.mapOk
import com.hugodehu.gitmiro.domain.interfaces.IFetchAllRemotesGitAction
import com.hugodehu.gitmiro.domain.interfaces.IGetGoneBranchesGitAction
import com.hugodehu.gitmiro.domain.models.TaskType
import javax.inject.Inject

/**
 * Never offered for pruning, even when their remote branch is gone
 */
private val PROTECTED_BRANCHES = setOf("main", "master", "develop", "dev")

/**
 * Fetches all the remotes, which removes the remote branches deleted on the server, then lists the local branches
 * whose upstream is gone: typically pull requests merged with squash, which a merge check can't detect.
 */
class GetBranchesToPruneUseCase @Inject constructor(
    private val fetchAllRemotesGitAction: IFetchAllRemotesGitAction,
    private val getGoneBranchesGitAction: IGetGoneBranchesGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke() = useCaseExecutor.executeLaunchAsync(
        taskType = TaskType.Fetch,
        dataToRefresh = arrayOf(DataToRefresh.LOG, DataToRefresh.REMOTES),
    ) { repositoryPath ->
        fetchAllRemotesGitAction(repositoryPath, null).bind()

        getGoneBranchesGitAction(repositoryPath).mapOk { branches ->
            branches.filterNot { it.simpleName in PROTECTED_BRANCHES }
        }
    }
}
