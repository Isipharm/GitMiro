package com.hugodehu.gitmiro.di.modules

import com.hugodehu.gitmiro.common.TabScope
import com.hugodehu.gitmiro.data.repositories.InMemoryRepositoryDataRepository
import com.hugodehu.gitmiro.data.repositories.InMemoryRepositoryStateRepository
import com.hugodehu.gitmiro.domain.repositories.RepositoryDataRepository
import com.hugodehu.gitmiro.domain.repositories.RepositoryStateRepository
import dagger.Binds
import dagger.Module

@Module
interface TabRepositoriesModule {
    @TabScope
    @Binds
    fun statusRepository(repository: InMemoryRepositoryDataRepository): RepositoryDataRepository

    @TabScope
    @Binds
    fun repositoryStateRepository(repository: InMemoryRepositoryStateRepository): RepositoryStateRepository
}