package com.hugodehu.gitmiro.data.git.log.graph

interface IGraphNode {
    val graphParentCount: Int
    fun getGraphParent(nth: Int): GraphNode
}