package com.example.paxik.ui_config.notes

import com.example.paxik.data.Folder
import com.example.paxik.data.Link
import com.example.paxik.data.Note
import com.example.paxik.data.NodeType
import com.example.paxik.data.Space

data class NodeRef(val type: NodeType, val id: Long)

val NodeType.glyph: String
    get() = when (this) {
        NodeType.SPACE -> "🗃"
        NodeType.FOLDER -> "📁"
        NodeType.NOTE -> "📝"
    }

val NodeType.label: String
    get() = when (this) {
        NodeType.SPACE -> "Пространство"
        NodeType.FOLDER -> "Папка"
        NodeType.NOTE -> "Заметка"
    }

val NodeType.order: Int
    get() = when (this) {
        NodeType.NOTE -> 0
        NodeType.FOLDER -> 1
        NodeType.SPACE -> 2
    }

val Link.from: NodeRef get() = NodeRef(fromType, fromId)

val Link.to: NodeRef get() = NodeRef(toType, toId)

data class GraphNode(
    val ref: NodeRef,
    val title: String,
    val spaceId: Long,
    val spaceName: String,
    val parent: NodeRef?
) {
    val displayTitle: String get() = "${ref.type.glyph} $title"

    val subtitle: String
        get() = if (ref.type == NodeType.SPACE) ref.type.label else "${ref.type.label} · $spaceName"
}

data class Relations(
    val outgoing: List<GraphNode>,
    val incoming: List<GraphNode>,
    val candidates: List<GraphNode>
)

data class GraphData(
    val nodes: List<GraphNode>,
    val links: List<Link>
) {
    val byRef: Map<NodeRef, GraphNode> = nodes.associateBy { it.ref }

    fun relationsOf(ref: NodeRef): Relations {
        val outgoing = links.filter { it.from == ref }.mapNotNull { byRef[it.to] }
        val incoming = links.filter { it.to == ref }.mapNotNull { byRef[it.from] }
        val linked = outgoing.map { it.ref }.toSet()
        val mySpace = byRef[ref]?.spaceId
        val candidates = nodes
            .filter { it.ref != ref && it.ref !in linked }
            .sortedWith(
                compareBy<GraphNode>(
                    { it.spaceId != mySpace },
                    { it.ref.type.order },
                    { it.title.lowercase() }
                )
            )
        return Relations(outgoing, incoming, candidates)
    }

    companion object {
        val EMPTY = GraphData(emptyList(), emptyList())

        fun build(
            spaces: List<Space>,
            folders: List<Folder>,
            notes: List<Note>,
            links: List<Link>
        ): GraphData {
            val spaceNames = spaces.associate { it.id to it.name }
            val nodes = ArrayList<GraphNode>()
            spaces.forEach {
                nodes.add(GraphNode(NodeRef(NodeType.SPACE, it.id), it.name, it.id, it.name, null))
            }
            folders.forEach {
                nodes.add(
                    GraphNode(
                        ref = NodeRef(NodeType.FOLDER, it.id),
                        title = it.name,
                        spaceId = it.spaceId,
                        spaceName = spaceNames[it.spaceId].orEmpty(),
                        parent = it.parentId?.let { parent -> NodeRef(NodeType.FOLDER, parent) }
                            ?: NodeRef(NodeType.SPACE, it.spaceId)
                    )
                )
            }
            notes.forEach {
                nodes.add(
                    GraphNode(
                        ref = NodeRef(NodeType.NOTE, it.id),
                        title = noteLabel(it),
                        spaceId = it.spaceId,
                        spaceName = spaceNames[it.spaceId].orEmpty(),
                        parent = it.folderId?.let { folder -> NodeRef(NodeType.FOLDER, folder) }
                            ?: NodeRef(NodeType.SPACE, it.spaceId)
                    )
                )
            }
            val present = nodes.map { it.ref }.toSet()
            return GraphData(nodes, links.filter { it.from in present && it.to in present })
        }
    }
}
