package com.hugodehu.gitmiro.domain.interfaces

import com.hugodehu.gitmiro.domain.errors.Either
import com.hugodehu.gitmiro.domain.errors.FSWatchError
import com.hugodehu.gitmiro.domain.models.WatcherEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import org.eclipse.jgit.lib.Repository

interface IFileChangesWatcher {
    fun addPathToWatch(path: String, isRecursive: Boolean)
    fun removePathFromWatch(path: String)

    suspend fun observeEvents(): Flow<WatcherEvent>

    fun close()
}