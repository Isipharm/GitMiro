package com.hugodehu.gitmiro.data.mappers

import com.hugodehu.gitmiro.data.git.log.graph.GraphNode
import com.hugodehu.gitmiro.domain.models.GraphCommit
import javax.inject.Inject

class GraphCommitMapper @Inject constructor(
    private val commitMapper: JGitCommitMapper,
): DataMapper<GraphCommit, GraphNode> {
    override fun toData(value: GraphCommit): Nothing {
        TODO("Not yet implemented")
    }

    override fun toDomain(value: GraphNode): GraphCommit {
        with (value) {
            return GraphCommit(
                commit = commitMapper.toDomain(value),
                lane = lane.position,
                passingLanes = passingLanes.map { it.position },
                mergingLanes = mergingLanes.map { it.position },
                forkingOffLanes = forkingOffLanes.map { it.position },
                childCount = childCount,
            )
        }
    }

}