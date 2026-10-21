package com.hugodehu.gitmiro.domain.usecases

import com.hugodehu.gitmiro.domain.UseCaseExecutor
import com.hugodehu.gitmiro.domain.interfaces.IGetFileCommitsAction
import javax.inject.Inject

class GetFileCommitsUseCase @Inject constructor(
    private val getFileCommitsAction: IGetFileCommitsAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(filePath: String) = useCaseExecutor.execute(
    ) { repositoryPath ->
        getFileCommitsAction(repositoryPath, filePath)
    }
}