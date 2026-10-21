package com.hugodehu.gitmiro.di.modules

import com.hugodehu.gitmiro.data.services.GitProviderService
import com.hugodehu.gitmiro.domain.services.IGitProviderService
import dagger.Binds
import dagger.Module

@Module
interface ServicesModule {
    @Binds
    fun bindIGitProviderService(service: GitProviderService): IGitProviderService

}