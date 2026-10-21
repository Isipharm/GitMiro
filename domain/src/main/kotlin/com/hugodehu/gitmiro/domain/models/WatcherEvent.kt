package com.hugodehu.gitmiro.domain.models

import com.hugodehu.gitmiro.FileChanged

sealed interface WatcherEvent {
    data class WatchInitError(val code: Int) : WatcherEvent
    data class ChangesDetected(val changes: List<FileChanged>) : WatcherEvent
}