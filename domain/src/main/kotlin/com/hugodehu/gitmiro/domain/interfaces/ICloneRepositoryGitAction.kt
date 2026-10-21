package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.models.CloneState
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ICloneRepositoryGitAction {
    operator fun invoke(directory: File, url: String, cloneSubmodules: Boolean): Flow<CloneState>
}