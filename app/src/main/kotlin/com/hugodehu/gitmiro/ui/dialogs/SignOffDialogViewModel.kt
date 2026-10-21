package com.hugodehu.gitmiro.ui.dialogs

import com.hugodehu.gitmiro.TabViewModel
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.models.SignOffConfig
import com.hugodehu.gitmiro.domain.usecases.LoadSignOffConfigUseCase
import com.hugodehu.gitmiro.domain.usecases.SaveSignOffConfigUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class SignOffDialogViewModel @Inject constructor(
    private val loadSignOffConfigUseCase: LoadSignOffConfigUseCase,
    private val saveSignOffConfigUseCase: SaveSignOffConfigUseCase,
) : TabViewModel() {
    private val _state = MutableStateFlow<SignOffState>(SignOffState.Loading)
    val state = _state.asStateFlow()

    fun loadSignOffFormat() {
        viewModelScope.launch {
            val signOffConfig = loadSignOffConfigUseCase()

            if (signOffConfig is Either.Ok) {
                _state.value = SignOffState.Loaded(signOffConfig.value)
            }
        }
    }

    fun saveSignOffFormat(newIsEnabled: Boolean, newFormat: String) {
        viewModelScope.launch {
            saveSignOffConfigUseCase(SignOffConfig(newIsEnabled, newFormat))
        }
    }
}

sealed interface SignOffState {
    object Loading : SignOffState
    data class Loaded(val signOffConfig: SignOffConfig) : SignOffState
}