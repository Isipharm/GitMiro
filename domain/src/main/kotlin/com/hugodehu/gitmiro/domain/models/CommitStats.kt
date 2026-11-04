package com.hugodehu.gitmiro.domain.models

import androidx.compose.runtime.Immutable

/**
 * Lines added and removed by a commit, compared with its first parent.
 */
@Immutable
data class CommitStats(val added: Int, val removed: Int)
