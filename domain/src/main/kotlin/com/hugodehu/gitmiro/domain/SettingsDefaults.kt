package com.hugodehu.gitmiro.domain

import com.hugodehu.gitmiro.domain.models.AvatarProviderType
import com.hugodehu.gitmiro.domain.models.DateTimeFormat

object SettingsDefaults {
    val defaultAvatarProviderType = AvatarProviderType.Gravatar
    val defaultDateTimeFormat = DateTimeFormat(
        useSystemDefault = true,
        customFormat = "dd MMM yyyy",
        is24hours = true,
        useRelativeDate = true,
    )
}