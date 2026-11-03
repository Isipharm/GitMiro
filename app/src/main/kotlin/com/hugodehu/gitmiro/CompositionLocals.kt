package com.hugodehu.gitmiro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.FocusRequester
import com.hugodehu.gitmiro.avatarproviders.AvatarProvider
import com.hugodehu.gitmiro.avatarproviders.NoneAvatarProvider
import com.hugodehu.gitmiro.domain.SettingsDefaults
import com.hugodehu.gitmiro.viewmodels.RepositoryTabViewModel

val LocalTab =
    compositionLocalOf<RepositoryTabViewModel> { throw IllegalStateException("Tab information requested but not provided") }
val LocalTabFocusRequester = compositionLocalOf { FocusRequester() }
val LocalAvatarProvider = compositionLocalOf<AvatarProvider> { NoneAvatarProvider() }
val LocalDateTimeFormat = compositionLocalOf { SettingsDefaults.defaultDateTimeFormat }

// Lets the open tab place its own actions (pull, push...) in the window title bar
val LocalTitleBarActions = compositionLocalOf<MutableState<(@Composable () -> Unit)?>> { mutableStateOf(null) }