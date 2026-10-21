package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.lfs.LfsSshAuthenticateResult
import com.hugodehu.gitmiro.domain.models.OperationType

interface IAuthenticateLfsServerWithSshGitAction {
    suspend operator fun invoke(
        lfsServerUrl: String,
        operationType: OperationType,
    ): LfsSshAuthenticateResult
}