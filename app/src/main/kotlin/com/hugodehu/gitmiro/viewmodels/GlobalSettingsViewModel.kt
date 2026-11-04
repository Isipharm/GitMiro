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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine as flowCombine
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SettingsViewModel"

class GlobalSettingsViewModel @Inject constructor(
    private val appSettingsService: AppSettingsService,
    private val logsRepository: LogsRepository,
    private val openUrlInBrowserUseCase: OpenUrlInBrowserUseCase,
) : TabViewModel() {
    // Edits wait here until they are saved, the last one made for a setting is the one that counts
    private val draft = MutableStateFlow<List<AppConfig>>(emptyList())

    /**
     * The saved settings with the edits that are not saved yet applied on top, so the screen shows what was edited.
     */
    val settingsViewState = settingsState()
        .flowCombine(draft) { saved, edits -> edits.fold(saved) { state, edit -> state.with(edit) } }
        .stateIn(emptySettingsState())

    val hasPendingChanges = draft
        .map { it.isNotEmpty() }
        .stateIn(false)

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetConfig -> editConfiguration(action.configuration)
            SettingsAction.Save -> saveConfiguration()
            SettingsAction.Discard -> draft.value = emptyList()
            SettingsAction.OpenLogsFolder -> openLogsFolderInFileExplorer()
        }
    }

    private fun editConfiguration(appConfig: AppConfig) {
        draft.value = draft.value.filterNot { it::class == appConfig::class } + appConfig
    }

    private fun saveConfiguration() = viewModelScope.launch {
        val edits = draft.value

        draft.value = emptyList()
        edits.forEach { appSettingsService.setConfiguration(it) }
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
            appSettingsService.mergeTrackedBranchesInLog,
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
            mergeTrackedBranchesInLog,
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
                mergeTrackedBranchesInLog,
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
            mergeTrackedBranchesInLog = true,
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

/**
 * This state as it would be once [config] is saved. Settings that this state doesn't show are left as they are.
 */
private fun SettingsViewState.with(config: AppConfig): SettingsViewState = when (config) {
    is AppConfig.ScaleUi -> copy(scaleUi = config.value)
    is AppConfig.LinesHeight -> copy(linesHeightType = config.value ?: linesHeightType)
    is AppConfig.PullWithRebase -> copy(pullWithRebase = config.value)
    is AppConfig.PushWithLease -> copy(pushWithLease = config.value)
    is AppConfig.FastForwardMerge -> copy(fastForwardMerge = config.value)
    is AppConfig.AutoStashOnMerge -> copy(autoStashOnMerge = config.value)
    is AppConfig.UseProxy -> copy(useProxy = config.value)
    is AppConfig.ProxyUseAuth -> copy(proxyUseAuth = config.value)
    is AppConfig.ProxyProxyType -> copy(proxyType = config.value)
    is AppConfig.ProxyHostName -> copy(proxyHostName = config.value)
    is AppConfig.ProxyPortNumber -> copy(proxyPortNumber = config.value)
    is AppConfig.ProxyHostUser -> copy(proxyHostUser = config.value)
    is AppConfig.ProxyHostPassword -> copy(proxyHostPassword = config.value)
    is AppConfig.DateFormatUseDefault -> copy(dateFormatUseDefault = config.value)
    is AppConfig.DateFormatCustomFormat -> copy(dateFormatCustomFormat = config.value)
    is AppConfig.DateFormatIs24h -> copy(dateFormatIs24h = config.value)
    is AppConfig.DateFormatUseRelative -> copy(dateFormatUseRelative = config.value)
    is AppConfig.CloneDefaultDirectory -> copy(cloneDefaultDirectory = config.value)
    is AppConfig.CacheCredentialsInMemory -> copy(cacheCredentialsInMemory = config.value)
    is AppConfig.AvatarProvider -> copy(avatarProvider = config.value)
    is AppConfig.Theme -> copy(theme = config.value)
    is AppConfig.CustomTheme -> copy(customTheme = config.value)
    is AppConfig.SwapStatusPanes -> copy(swapStatusPanes = config.value)
    is AppConfig.MergeTrackedBranchesInLog -> copy(mergeTrackedBranchesInLog = config.value)
    is AppConfig.TerminalPath -> copy(terminalPath = config.value)
    is AppConfig.DiffDisplayFullFile,
    is AppConfig.DiffTextViewType,
    is AppConfig.ShowChangesAsTree -> this
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
    val mergeTrackedBranchesInLog: Boolean,
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
    /** Edits a setting, it is only kept once [Save] is used */
    data class SetConfig(val configuration: AppConfig) : SettingsAction
    data object Save : SettingsAction
    data object Discard : SettingsAction
    data object OpenLogsFolder : SettingsAction
}