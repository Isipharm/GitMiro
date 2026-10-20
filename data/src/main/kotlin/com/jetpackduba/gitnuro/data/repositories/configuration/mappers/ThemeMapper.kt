package com.jetpackduba.gitnuro.data.repositories.configuration.mappers

import com.jetpackduba.gitnuro.data.mappers.DataMapper
import com.jetpackduba.gitnuro.domain.models.ui.Theme
import javax.inject.Inject

private const val DARK = "dark"
private const val DARK_BLUE = "dark_blue"
private const val LIGHT = "light"
private const val CUSTOM = "custom"

class ThemeMapper @Inject constructor() : DataMapper<Theme?, String?> {
    override fun toData(value: Theme?): String? {
        return when (value) {
            Theme.Light -> LIGHT
            Theme.Dark -> DARK
            Theme.DarkBlue -> DARK_BLUE
            Theme.Custom -> CUSTOM
            null -> null
        }
    }


    override fun toDomain(value: String?): Theme? {
        return when (value) {
            LIGHT -> Theme.Light
            DARK -> Theme.Dark
            DARK_BLUE -> Theme.DarkBlue
            CUSTOM -> Theme.Custom
            null -> null
            // Unknown values (e.g. written by another Gitnuro version) fall back to the default theme
            else -> null
        }
    }
}