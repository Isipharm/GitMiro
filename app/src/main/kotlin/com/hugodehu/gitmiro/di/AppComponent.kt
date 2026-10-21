package com.hugodehu.gitmiro.di

import com.hugodehu.gitmiro.App
import com.hugodehu.gitmiro.data.di.DatastoreModule
import com.hugodehu.gitmiro.di.modules.*
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ShellModule::class,
        NetworkModule::class,
        GitCredentialsManagerModule::class,
        RepositoriesModule::class,
        DatastoreModule::class,
        ServicesModule::class,
    ]
)
interface AppComponent {
    fun app(): App
    fun tabComponentFactory(): TabComponent.Factory
}
