package com.hugodehu.gitmiro.ui.dialogs

import com.hugodehu.gitmiro.TabViewModel
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.usecases.DataToRefresh
import com.hugodehu.gitmiro.domain.usecases.GetWorktreeUseCase
import com.hugodehu.gitmiro.domain.usecases.OpenPathInSystemUseCase
import com.hugodehu.gitmiro.domain.usecases.RefreshDataUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

class QuickActionsViewModel @Inject constructor(
    private val refreshDataUseCase: RefreshDataUseCase,
    private val getWorktreeUseCase: GetWorktreeUseCase,
    private val openPathInSystemUseCase: OpenPathInSystemUseCase,
) : TabViewModel() {

    // TODO Implement bunch of methods

    fun refreshRepository() = refreshDataUseCase(DataToRefresh.ALL)

    fun openProjectInFileExplorer() {
        viewModelScope.launch {
            val worktree = getWorktreeUseCase()

            if (worktree is Either.Ok) {
                openPathInSystemUseCase(worktree.value)
            }
        }
    }
}