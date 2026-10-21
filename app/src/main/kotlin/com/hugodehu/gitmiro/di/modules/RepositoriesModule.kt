package com.hugodehu.gitmiro.di.modules

import com.hugodehu.gitmiro.data.repositories.CredentialsCacheRepository
import com.hugodehu.gitmiro.data.repositories.InMemoryNotificationsRepository
import com.hugodehu.gitmiro.data.repositories.JvmSystemProxyRepository
import com.hugodehu.gitmiro.data.repositories.NetworkLfsRepository
import com.hugodehu.gitmiro.domain.repositories.AppSettingsRepository
import com.hugodehu.gitmiro.data.repositories.configuration.DataStoreAppSettingsRepository
import com.hugodehu.gitmiro.domain.repositories.CredentialsRepository
import com.hugodehu.gitmiro.domain.repositories.LfsRepository
import com.hugodehu.gitmiro.domain.repositories.NotificationsRepository
import com.hugodehu.gitmiro.domain.repositories.SystemProxyRepository
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
interface RepositoriesModule {
    @Binds
    fun notificationsRepository(repository: InMemoryNotificationsRepository): NotificationsRepository

    @Binds
    fun systemProxyRepository(repository: JvmSystemProxyRepository): SystemProxyRepository

    @Singleton
    @Binds
    fun appSettingsRepository(repository: DataStoreAppSettingsRepository): AppSettingsRepository

    @Singleton
    @Binds
    fun credentialsRepository(repository: CredentialsCacheRepository): CredentialsRepository

    @Binds
    fun lfsRepository(repository: NetworkLfsRepository): LfsRepository
}