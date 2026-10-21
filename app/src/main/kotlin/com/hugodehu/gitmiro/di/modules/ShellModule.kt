package com.hugodehu.gitmiro.di.modules

import com.hugodehu.gitmiro.AppEnvInfo
import com.hugodehu.gitmiro.common.OS
import com.hugodehu.gitmiro.common.currentOs
import com.hugodehu.gitmiro.domain.FlatpakShellManager
import com.hugodehu.gitmiro.domain.IShellManager
import com.hugodehu.gitmiro.domain.ShellManager
import com.hugodehu.gitmiro.terminal.ITerminalProvider
import com.hugodehu.gitmiro.terminal.LinuxTerminalProvider
import com.hugodehu.gitmiro.terminal.MacTerminalProvider
import com.hugodehu.gitmiro.terminal.WindowsTerminalProvider
import dagger.Module
import dagger.Provides
import javax.inject.Provider

@Module
class ShellModule {
    @Provides
    fun provideShellManager(
        appEnvInfo: AppEnvInfo,
        shellManager: Provider<ShellManager>,
        flatpakShellManager: Provider<FlatpakShellManager>,
    ): IShellManager {
        return if (appEnvInfo.isFlatpak)
            flatpakShellManager.get()
        else
            shellManager.get()
    }

    @Provides
    fun provideTerminalProvider(
        linuxTerminalProvider: Provider<LinuxTerminalProvider>,
        windowsTerminalProvider: Provider<WindowsTerminalProvider>,
        macTerminalProvider: Provider<MacTerminalProvider>,
    ): ITerminalProvider {
        return when (currentOs) {
            OS.LINUX -> linuxTerminalProvider.get()
            OS.WINDOWS -> windowsTerminalProvider.get()
            OS.MAC -> macTerminalProvider.get()
            OS.UNKNOWN -> throw NotImplementedError("Unknown operating system")
        }
    }
}