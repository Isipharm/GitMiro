package com.hugodehu.gitmiro.di

import com.hugodehu.gitmiro.common.TabScope
import com.hugodehu.gitmiro.di.modules.FileWatcherModule
import com.hugodehu.gitmiro.di.modules.TabModule
import com.hugodehu.gitmiro.di.modules.TabRepositoriesModule
import com.hugodehu.gitmiro.di.modules.TabScopeGitActionsModule
import com.hugodehu.gitmiro.repositoryopen.RepositoryOpenViewModel
import com.hugodehu.gitmiro.ui.dialogs.*
import com.hugodehu.gitmiro.viewmodels.*
import com.hugodehu.gitmiro.viewmodels.sidepanel.SubmoduleDialogViewModel
import dagger.Subcomponent

@TabScope
@Subcomponent(
    modules = [
        TabModule::class,
        TabRepositoriesModule::class,
        FileWatcherModule::class,
        TabScopeGitActionsModule::class,
    ],
)
interface TabComponent {
    @Subcomponent.Factory
    interface Factory {
        fun create(): TabComponent
    }

    fun cloneViewModel(): CloneViewModel
    fun settingsViewModel(): GlobalSettingsViewModel
    fun repositoryTabViewModelFactory(): RepositoryTabViewModel.Factory
    fun repositoryOpenViewModel(): RepositoryOpenViewModel
    fun historyViewModel(): HistoryViewModel
    fun authorViewModel(): AuthorViewModel
    fun stashWithMessageViewModel(): StashWithMessageViewModel
    fun quickActionsViewModel(): QuickActionsViewModel
    fun setUpstreamBranchDialogViewModelFactory(): SetUpstreamBranchDialogViewModel.Factory
    fun renameBranchDialogViewModelFactory(): RenameBranchDialogViewModel.Factory
    fun createBranchViewModelFactory(): CreateBranchViewModel.Factory
    fun createTagViewModelFactory(): CreateTagViewModel.Factory
    fun resetBranchViewModelFactory(): ResetBranchViewModel.Factory
    fun addEditRemoteViewModelFactory(): AddEditRemoteViewModel.Factory
    fun submoduleDialogViewModel(): SubmoduleDialogViewModel
    fun signOffDialogViewModel(): SignOffDialogViewModel
}