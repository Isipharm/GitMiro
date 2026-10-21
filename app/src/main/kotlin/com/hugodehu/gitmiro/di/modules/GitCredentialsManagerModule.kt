package com.hugodehu.gitmiro.di.modules

import com.hugodehu.gitmiro.common.OS
import com.hugodehu.gitmiro.common.currentOs
import com.hugodehu.gitmiro.domain.credentials.external.IGitCredentialsManagerProvider
import com.hugodehu.gitmiro.domain.credentials.external.NixGitCredentialsManagerProvider
import com.hugodehu.gitmiro.domain.credentials.external.WindowsGitCredentialsManagerProvider
import dagger.Module
import dagger.Provides
import javax.inject.Provider

@Module
class GitCredentialsManagerModule {
    @Provides
    fun providesGitCredentialsManagerProvider(
        windowsGitCredentialsManagerProvider: Provider<WindowsGitCredentialsManagerProvider>,
        nixGitCredentialsManagerProvider: Provider<NixGitCredentialsManagerProvider>,
    ): IGitCredentialsManagerProvider {
        return when (currentOs) {
            OS.LINUX, OS.MAC ->  nixGitCredentialsManagerProvider.get() // TODO Test this on MacOs
            OS.WINDOWS -> windowsGitCredentialsManagerProvider.get()
            OS.UNKNOWN -> throw IllegalStateException("Unknown OS")
        }
    }
}