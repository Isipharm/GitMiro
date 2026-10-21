package com.hugodehu.gitmiro.domain.models

/**
 * Side of a conflict to keep. Current is the HEAD version (index stage 2, "ours") and incoming is the version
 * being merged, rebased, cherry-picked or reverted (index stage 3, "theirs").
 */
enum class ConflictSide {
    CURRENT,
    INCOMING,
}
