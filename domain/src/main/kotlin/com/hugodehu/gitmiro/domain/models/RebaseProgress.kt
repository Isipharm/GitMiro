package com.hugodehu.gitmiro.domain.models

import androidx.compose.runtime.Immutable

/**
 * Commits replayed by a rebase that is currently in progress.
 *
 * @param branch branch being rebased
 * @param onto name of the commit or branch the commits are replayed on
 */
@Immutable
data class RebaseProgress(
    val branch: String,
    val onto: String,
    val steps: List<RebaseProgressStep>,
) {
    val doneCount get() = steps.count { it.status == RebaseProgressStep.Status.DONE }

    /**
     * One based position of the commit being replayed, null if all of them are applied.
     */
    val currentPosition get() = steps.indexOfFirst { it.status == RebaseProgressStep.Status.CURRENT }.takeIf { it >= 0 }?.plus(1)
}

@Immutable
data class RebaseProgressStep(
    val commit: String,
    val message: String,
    val status: Status,
) {
    enum class Status { DONE, CURRENT, TODO }
}
