package com.hugodehu.gitmiro

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppEnvInfo @Inject constructor() {
    var isFlatpak = false
}