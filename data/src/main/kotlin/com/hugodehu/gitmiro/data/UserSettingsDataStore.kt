package com.hugodehu.gitmiro.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

class UserSettingsDataStore(
    val preferences: DataStore<Preferences>
)