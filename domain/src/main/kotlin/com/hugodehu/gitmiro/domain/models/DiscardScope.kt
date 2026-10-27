package com.hugodehu.gitmiro.domain.models

/**
 * Which uncommitted changes to throw away. Ignored files are never touched.
 */
enum class DiscardScope {
    /**
     * Everything back to the last commit: staged and unstaged changes, new files deleted
     */
    ALL,

    /**
     * Staged files back to the last commit (their unstaged changes too), new staged files deleted
     */
    STAGED,

    /**
     * Working tree back to what is staged, untracked files deleted
     */
    UNSTAGED,
}
