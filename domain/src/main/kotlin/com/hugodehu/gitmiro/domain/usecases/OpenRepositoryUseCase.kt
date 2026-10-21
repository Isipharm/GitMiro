package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.AppStateManager
import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.okOrNull
import com.hugodehu.gitmiro.domain.interfaces.IOpenRepositoryGitAction
import com.hugodehu.gitmiro.domain.models.RepositorySelectionState
import com.hugodehu.gitmiro.domain.models.TaskType
import com.hugodehu.gitmiro.domain.repositories.FailureSeverity
import com.hugodehu.gitmiro.domain.repositories.RepositoryDataRepository
import com.hugodehu.gitmiro.domain.repositories.RepositoryStateRepository
import javax.inject.Inject

class OpenRepositoryUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val openRepositoryGitAction: IOpenRepositoryGitAction,
    private val refreshDataUseCase: RefreshDataUseCase,
    private val observeRepositoryToRefreshUseCase: ObserveRepositoryToRefreshUseCase,
    private val getWorktreeUseCase: GetWorktreeUseCase,
    private val appStateManager: AppStateManager,
) {
    suspend operator fun invoke(directory: String) {
        val repositoryPathResult = openRepositoryGitAction(directory)

        when (repositoryPathResult) {
            is Either.Err ->  {
                repositoryDataRepository.setRepositorySelectionState(RepositorySelectionState.None)
                repositoryStateRepository.addCompletedTaskFailed(
                    TaskType.RepositoryOpen,
                    repositoryPathResult.error,
                    FailureSeverity.HIGH,
                )
            }
            is Either.Ok -> {
                repositoryDataRepository.setRepositorySelectionState(RepositorySelectionState.Open(repositoryPathResult.value))

                val worktree = getWorktreeUseCase().okOrNull()
                if (worktree != null) {
                    appStateManager.repositoryTabChanged(worktree)
                }

                refreshDataUseCase(DataToRefresh.ALL)
                observeRepositoryToRefreshUseCase()
            }
        }
    }
}