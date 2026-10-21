package com.hugodehu.gitmiro.ui.dialogs

import com.hugodehu.gitmiro.TabViewModel
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.usecases.ResetBranchUseCase
import com.hugodehu.gitmiro.domain.usecases.ResetType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.eclipse.jgit.revwalk.RevCommit

class ResetBranchViewModel @AssistedInject constructor(
    private val resetBranchUseCase: ResetBranchUseCase,
    @Assisted private val targetCommit: Commit,
) : TabViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(commit: Commit): ResetBranchViewModel
    }

    fun reset(resetType: ResetType) {
        resetBranchUseCase(targetCommit, resetType)
    }
}