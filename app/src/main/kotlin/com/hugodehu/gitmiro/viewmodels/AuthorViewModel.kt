package com.hugodehu.gitmiro.viewmodels

import com.hugodehu.gitmiro.TabViewModel
import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.models.AuthorInfo
import com.hugodehu.gitmiro.domain.models.Identity
import com.hugodehu.gitmiro.domain.models.emptyIdentity
import com.hugodehu.gitmiro.domain.usecases.GetAuthorUseCase
import com.hugodehu.gitmiro.domain.usecases.SaveAuthorUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class AuthorViewModel @Inject constructor(
    private val saveAuthorUseCase: SaveAuthorUseCase,
    private val getAuthorUseCase: GetAuthorUseCase,
) : TabViewModel() {
    val authorInfo: StateFlow<AuthorInfo> = flow {
        val author = when (val author = getAuthorUseCase()) {
            is Either.Ok -> author.value
            else -> AuthorInfo(emptyIdentity(), emptyIdentity())
        }

        emit(author)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = AuthorInfo(emptyIdentity(), emptyIdentity()),
        )


    fun saveAuthorInfo(globalName: String?, globalEmail: String?, name: String?, email: String?) = viewModelScope.launch {
        saveAuthorUseCase(
            AuthorInfo(Identity(globalName, globalEmail), Identity(name, email))
        )
    }
}
