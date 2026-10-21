package com.hugodehu.gitmiro.ui.dialogs

import com.hugodehu.gitmiro.TabViewModel
import com.hugodehu.gitmiro.domain.models.Commit
import com.hugodehu.gitmiro.domain.usecases.CreateTagUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.eclipse.jgit.revwalk.RevCommit

class CreateTagViewModel @AssistedInject constructor(
    private val createTagUseCase: CreateTagUseCase,
    @Assisted private val targetCommit: Commit,
) : TabViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(commit: Commit): CreateTagViewModel
    }

    fun createTag(name: String) {
        createTagUseCase(name, targetCommit)
    }
}