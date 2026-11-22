package com.example.paxik.ui_config.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraphLayoutTest {
    @Test
    fun emptyGraphHasNoPositions() {
        assertTrue(GraphLayout.compute(emptyList<Long>(), emptyList()).isEmpty())
    }

    @Test
    fun singleNodeIsCentered() {
        val result = GraphLayout.compute(listOf(7L), emptyList())

        assertEquals(GraphLayout.SIZE / 2, result.getValue(7L).x, 0.001f)
        assertEquals(GraphLayout.SIZE / 2, result.getValue(7L).y, 0.001f)
    }

    @Test
    fun everyNodeGetsPositionInsideCanvas() {
        val ids = listOf("a", "b", "c", "d")
        val edges = listOf("a" to "b", "b" to "c", "x" to "a")

        val result = GraphLayout.compute(ids, edges)

        assertEquals(ids.toSet(), result.keys)
        assertTrue(result.values.all { it.x in 20f..GraphLayout.SIZE - 20f })
        assertTrue(result.values.all { it.y in 20f..GraphLayout.SIZE - 20f })
    }
}
