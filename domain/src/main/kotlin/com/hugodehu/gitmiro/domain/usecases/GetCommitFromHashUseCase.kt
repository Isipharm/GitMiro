package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.interfaces.IGetCommitFromHashGitAction
import com.hugodehu.gitmiro.domain.repositories.DataState
import com.hugodehu.gitmiro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class GetCommitFromHashUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val getCommitFromHashGitAction: IGetCommitFromHashGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
) {
    suspend operator fun invoke(commitHash: String) = useCaseExecutor.execute { repositoryPath ->
        val logDataState = repositoryDataRepository.log.value
        val commit = (logDataState as? DataState.Loaded)?.data[commitHash]?.commit

        if (commit != null) {
            return@execute Either.Ok(commit)
        }

        getCommitFromHashGitAction(repositoryPath, commitHash)
    }
}