package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.Pagination
import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.errors.AppError
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.repositories.DataState
import com.hugodehu.gitmiro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class IncreaseLogCountUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val useCaseExecutor: UseCaseExecutor,
    private val getLogUseCase: GetLogUseCase,
) {
    suspend operator fun invoke(newLimit: Int): Either<Unit, AppError> {
        return useCaseExecutor.execute { repositoryPath ->
            // If the data is being loaded or failed, do not try to load more items
            val log = (repositoryDataRepository.log.value as? DataState.Loaded)?.data ?: return@execute Either.Ok(Unit)

            if (newLimit > repositoryDataRepository.maxCommitsToLoadLimit) {
                repositoryDataRepository.maxCommitsToLoadLimit = newLimit
                repositoryDataRepository.updateLog {
                    getLogUseCase(
                        repositoryPath,
                        pagination = Pagination.Paginated(log)
                    )
                }
            }

            Either.Ok(Unit)
        }
    }
}