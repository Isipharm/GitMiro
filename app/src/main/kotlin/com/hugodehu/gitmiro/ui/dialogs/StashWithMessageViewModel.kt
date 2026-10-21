package com.hugodehu.gitmiro.ui.dialogs

import com.hugodehu.gitmiro.TabViewModel
import com.hugodehu.gitmiro.domain.usecases.ResetBranchUseCase
import com.hugodehu.gitmiro.domain.usecases.ResetType
import com.hugodehu.gitmiro.domain.usecases.StashChangesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class StashWithMessageViewModel @Inject constructor(
    private val stashChangesUseCase: StashChangesUseCase,
) : TabViewModel() {

    fun stash(message: String) {
        stashChangesUseCase(message)
    }
}