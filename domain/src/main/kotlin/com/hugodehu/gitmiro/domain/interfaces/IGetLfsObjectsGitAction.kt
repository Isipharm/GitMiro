package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.LfsError
import com.hugodehu.gitmiro.domain.lfs.LfsObjectBatch
import com.hugodehu.gitmiro.domain.lfs.LfsObjects
import com.hugodehu.gitmiro.domain.models.OperationType

interface IGetLfsObjectsGitAction {
    suspend operator fun invoke(
        lfsServerUrl: String,
        operationType: OperationType,
        branch: String,
        lfsObjectBatches: List<LfsObjectBatch>,
        headers: Map<String, String>,
    ): Either<LfsObjects, LfsError>
}