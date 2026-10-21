package com.hugodehu.gitmiro.di.modules

import com.hugodehu.gitmiro.data.git.FileChangesWatcher
import com.hugodehu.gitmiro.domain.interfaces.IFileChangesWatcher
import dagger.Binds
import dagger.Module

@Module
interface FileWatcherModule {
    @Binds
    fun bindFileWatcher(watcher: FileChangesWatcher) : IFileChangesWatcher
}