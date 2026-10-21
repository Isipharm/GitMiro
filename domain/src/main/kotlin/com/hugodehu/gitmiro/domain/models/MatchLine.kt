package com.hugodehu.gitmiro.domain.models

import com.hugodehu.gitmiro.domain.DiffMatchPatch

data class MatchLine(val diffs: List<DiffMatchPatch.Diff>)