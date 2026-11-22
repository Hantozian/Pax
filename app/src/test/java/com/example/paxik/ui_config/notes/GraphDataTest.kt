package com.example.paxik.ui_config.notes

import com.example.paxik.data.Folder
import com.example.paxik.data.Link
import com.example.paxik.data.NodeType
import com.example.paxik.data.NoteBrief
import com.example.paxik.data.Space
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GraphDataTest {
    private val spaces = listOf(Space(1, "Главная"), Space(2, "Работа"))
    private val folders = listOf(
        Folder(id = 10, name = "Проекты", parentId = null, spaceId = 1),
        Folder(id = 11, name = "Архив", parentId = 10, spaceId = 1)
    )
    private val notes = listOf(
        NoteBrief(id = 100, title = "Идея", preview = "", folderId = 10, spaceId = 1),
        NoteBrief(id = 101, title = "", preview = "Первая строка\nвторая", folderId = null, spaceId = 2)
    )
    private val links = listOf(
        Link(1, NodeType.NOTE, 100, NodeType.SPACE, 2),
        Link(2, NodeType.NOTE, 100, NodeType.NOTE, 999)
    )

    private fun graph() = GraphData.build(spaces, folders, notes, links)

    private fun ref(type: NodeType, id: Long) = NodeRef(type, id)

    @Test
    fun buildCreatesNodesForAllTypesAndDropsDanglingLinks() {
        val data = graph()

        assertEquals(6, data.nodes.size)
        assertEquals(1, data.links.size)
        assertEquals(ref(NodeType.SPACE, 2), data.links.single().to)
    }

    @Test
    fun buildAssignsStructuralParents() {
        val data = graph()

        assertNull(data.byRef.getValue(ref(NodeType.SPACE, 1)).parent)
        assertEquals(
            ref(NodeType.SPACE, 1),
            data.byRef.getValue(ref(NodeType.FOLDER, 10)).parent
        )
        assertEquals(
            ref(NodeType.FOLDER, 10),
            data.byRef.getValue(ref(NodeType.FOLDER, 11)).parent
        )
        assertEquals(
            ref(NodeType.FOLDER, 10),
            data.byRef.getValue(ref(NodeType.NOTE, 100)).parent
        )
        assertEquals(
            ref(NodeType.SPACE, 2),
            data.byRef.getValue(ref(NodeType.NOTE, 101)).parent
        )
    }

    @Test
    fun untitledNoteUsesFirstLineOfPreview() {
        val node = graph().byRef.getValue(ref(NodeType.NOTE, 101))

        assertEquals("Первая строка", node.title)
    }

    @Test
    fun relationsListOutgoingAndIncoming() {
        val data = graph()

        val note = data.relationsOf(ref(NodeType.NOTE, 100))
        val space = data.relationsOf(ref(NodeType.SPACE, 2))

        assertEquals(listOf(ref(NodeType.SPACE, 2)), note.outgoing.map { it.ref })
        assertEquals(emptyList<NodeRef>(), note.incoming.map { it.ref })
        assertEquals(listOf(ref(NodeType.NOTE, 100)), space.incoming.map { it.ref })
    }

    @Test
    fun candidatesExcludeSelfAndLinkedAndPreferOwnSpace() {
        val candidates = graph().relationsOf(ref(NodeType.NOTE, 100)).candidates.map { it.ref }

        assertEquals(
            listOf(
                ref(NodeType.FOLDER, 11),
                ref(NodeType.FOLDER, 10),
                ref(NodeType.SPACE, 1),
                ref(NodeType.NOTE, 101)
            ),
            candidates
        )
    }
}
