package com.hugodehu.gitmiro

import androidx.compose.runtime.compositionLocalOf
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