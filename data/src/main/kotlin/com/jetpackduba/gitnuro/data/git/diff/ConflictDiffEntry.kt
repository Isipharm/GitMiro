package com.jetpackduba.gitnuro.data.git.diff

import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.lib.AbbreviatedObjectId
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectId

/**
 * Diff entry comparing both sides of a conflict: current (index stage 2) as the old side and incoming (index stage 3)
 * as the new side. A side that deleted the file is shown as a missing file.
 */
class ConflictDiffEntry(path: String, current: DirCacheEntry?, incoming: DirCacheEntry?) : DiffEntry() {
    init {
        oldPath = if (current == null) DEV_NULL else path
        newPath = if (incoming == null) DEV_NULL else path
        oldMode = current?.fileMode ?: FileMode.MISSING
        newMode = incoming?.fileMode ?: FileMode.MISSING
        oldId = AbbreviatedObjectId.fromObjectId(current?.objectId ?: ObjectId.zeroId())
        newId = AbbreviatedObjectId.fromObjectId(incoming?.objectId ?: ObjectId.zeroId())
        changeType = when {
            current == null -> ChangeType.ADD
            incoming == null -> ChangeType.DELETE
            else -> ChangeType.MODIFY
        }
    }

    companion object {
        /**
         * Returns null if neither side has the file (both deleted it), as there is nothing to compare.
         */
        fun fromIndex(dirCache: DirCache, path: String): ConflictDiffEntry? {
            val entries = dirCache.entriesForPath(path)
            val current = entries.firstOrNull { it.stage == DirCacheEntry.STAGE_2 }
            val incoming = entries.firstOrNull { it.stage == DirCacheEntry.STAGE_3 }

            return if (current == null && incoming == null) null else ConflictDiffEntry(path, current, incoming)
        }
    }
}

/**
 * All the index entries of a path, one per stage when the path is in conflict.
 */
fun DirCache.entriesForPath(path: String): List<DirCacheEntry> {
    val firstEntryIndex = findEntry(path)

    return if (firstEntryIndex < 0) {
        emptyList()
    } else {
        (firstEntryIndex until nextEntry(firstEntryIndex)).map { getEntry(it) }
    }
}
