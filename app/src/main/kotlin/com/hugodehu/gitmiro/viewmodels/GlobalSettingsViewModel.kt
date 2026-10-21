package com.hugodehu.gitmiro.viewmodels

import androidx.compose.runtime.Immutable
import com.hugodehu.gitmiro.LogsRepository
import com.hugodehu.gitmiro.TabViewModel
import com.hugodehu.gitmiro.common.flows.combine
import com.hugodehu.gitmiro.common.printError
import com.hugodehu.gitmiro.domain.models.AppConfig
import com.hugodehu.gitmiro.domain.models.AvatarProviderType
import com.hugodehu.gitmiro.domain.models.ProxyType
import com.hugodehu.gitmiro.domain.models.ui.LinesHeightType
import com.hugodehu.gitmiro.domain.models.ui.Theme
import com.hugodehu.gitmiro.domain.services.AppSettingsService
import com.hugodehu.gitmiro.extensions.stateIn
import com.hugodehu.gitmiro.system.OpenUrlInBrowserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SettingsViewModel"

class GlobalSettingsViewModel @Inject constructor(
    private val appSettingsService: AppSettingsService,
    private val logsRepository: LogsRepository,
    private val openUrlInBrowserUseCase: OpenUrlInBrowserUseCase,
) : TabViewModel() {
    val settingsViewState = settingsState()
        .stateIn(emptySettingsState())

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetConfig -> setAppConfiguration(action.configuration)
            SettingsAction.OpenLogsFolder -> openLogsFolderInFileExplorer()
        }
    }

    private fun setAppConfiguration(appConfig: AppConfig) = viewModelScope.launch {
        appSettingsService.setConfiguration(appConfig)
    }

    fun openLogsFolderInFileExplorer() {
        try {
            openUrlInBrowserUseCase(logsRepository.logsDirectory.absolutePath)
        } catch (e: Exception) {
            printError(TAG, "Failed to open logs dir: ${e.message.orEmpty()}", e)
        }
    }

    private fun settingsState(): Flow<SettingsViewState> {
        return combine(
            appSettingsService.scaleUi,
            appSettingsService.theme,
            appSettingsService.customTheme,
            appSettingsService.linesHeightType,
            appSettingsService.dateFormatUseDefault,
            appSettingsService.dateFormatCustomFormat,
            appSettingsService.dateFormatIs24h,
            appSettingsService.dateFormatUseRelative,
            appSettingsService.avatarProvider,
            appSettingsService.swapStatusPanes,
            appSettingsService.pullWithRebase,
            appSettingsService.pushWithLease,
            appSettingsService.fastForwardMerge,
            appSettingsService.autoStashOnMerge,
            appSettingsService.cloneDefaultDirectory,
            appSettingsService.useProxy,
            appSettingsService.proxyUseAuth,
            appSettingsService.proxyType,
            appSettingsService.proxyHostName,
            appSettingsService.proxyPortNumber,
            appSettingsService.proxyHostUser,
            appSettingsService.proxyHostPassword,
            appSettingsService.verifySsl,
            appSettingsService.cacheCredentialsInMemory,
            appSettingsService.terminalPath,
        ) { scaleUi,
            theme,
            customTheme,
            linesHeightType,
            dateFormatUseDefault,
            dateFormatCustomFormat,
            dateFormatIs24h,
            dateFormatUseRelative,
            avatarProvider,
            swapStatusPanes,
            pullWithRebase,
            pushWithLease,
            fastForwardMerge,
            autoStashOnMerge,
            cloneDefaultDirectory,
            useProxy,
            proxyUseAuth,
            proxyType,
            proxyHostName,
            proxyPortNumber,
            proxyHostUser,
            proxyHostPassword,
            verifySsl,
            cacheCredentialsInMemory,
            terminalPath ->

            SettingsViewState(
                scaleUi,
                theme,
                customTheme,
                linesHeightType,
                dateFormatUseDefault,
                dateFormatCustomFormat,
                dateFormatIs24h,
                dateFormatUseRelative,
                avatarProvider,
                swapStatusPanes,
                pullWithRebase,
                pushWithLease,
                fastForwardMerge,
                autoStashOnMerge,
                cloneDefaultDirectory,
                useProxy,
                proxyUseAuth,
                proxyType,
                proxyHostName,
                proxyPortNumber,
                proxyHostUser,
                proxyHostPassword,
                verifySsl,
                cacheCredentialsInMemory,
                terminalPath,
            )
        }
    }

    private fun emptySettingsState(): SettingsViewState {
        return SettingsViewState(
            scaleUi = null,
            theme = Theme.Light,
            customTheme = "",
            linesHeightType = LinesHeightType.SPACED,
            dateFormatUseDefault = false,
            dateFormatCustomFormat = "",
            dateFormatIs24h = false,
            dateFormatUseRelative = false,
            avatarProvider = AvatarProviderType.Gravatar,
            swapStatusPanes = false,
            pullWithRebase = false,
            pushWithLease = false,
            fastForwardMerge = false,
            autoStashOnMerge = false,
            cloneDefaultDirectory = "",
            useProxy = false,
            proxyUseAuth = false,
            proxyType = ProxyType.HTTP,
            proxyHostName = "",
            proxyPortNumber = null,
            proxyHostUser = "",
            proxyHostPassword = "",
            verifySsl = false,
            cacheCredentialsInMemory = false,
            terminalPath = "",
        )
    }
}

@Immutable
data class SettingsViewState(
    val scaleUi: Float?,
    val theme: Theme,
    val customTheme: String?,
    val linesHeightType: LinesHeightType,
    val dateFormatUseDefault: Boolean,
    val dateFormatCustomFormat: String,
    val dateFormatIs24h: Boolean,
    val dateFormatUseRelative: Boolean,
    val avatarProvider: AvatarProviderType,
    val swapStatusPanes: Boolean,
    val pullWithRebase: Boolean,
    val pushWithLease: Boolean,
    val fastForwardMerge: Boolean,
    val autoStashOnMerge: Boolean,
    val cloneDefaultDirectory: String?,
    val useProxy: Boolean,
    val proxyUseAuth: Boolean,
    val proxyType: ProxyType,
    val proxyHostName: String?,
    val proxyPortNumber: Int?,
    val proxyHostUser: String?,
    val proxyHostPassword: String?,
    val verifySsl: Boolean,
    val cacheCredentialsInMemory: Boolean,
    val terminalPath: String?,
)

sealed interface SettingsAction {
    data class SetConfig(val configuration: AppConfig) : SettingsAction
    data object OpenLogsFolder : SettingsAction
}