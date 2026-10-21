package com.hugodehu.gitmiro.di.modules

import com.hugodehu.gitmiro.common.TabScope
import com.hugodehu.gitmiro.domain.TabCoroutineScope
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
class TabModule {    @TabScope
    @Provides
    fun provideTabCoroutineScope() = TabCoroutineScope()
}
