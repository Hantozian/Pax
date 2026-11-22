package com.example.paxik.ui_config.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RichTextTest {
    @Test
    fun encodeAndDecodeRoundTrip() {
        val text = "abc\nhello"
        val spans = listOf(Span(Fmt.BOLD, 0, 3), Span(Fmt.H1, 4, 9))

        val encoded = RichText.encode(spans)

        assertEquals("b:0:3;1:4:9", encoded)
        assertEquals(spans, RichText.decode(encoded, text))
    }

    @Test
    fun decodeSkipsBrokenParts() {
        val result = RichText.decode("b:0:3;x:1:2;i:oops;u:1", "abcdef")

        assertEquals(listOf(Span(Fmt.BOLD, 0, 3)), result)
    }

    @Test
    fun normalizeMergesOverlappingAndAdjacentRanges() {
        val spans = listOf(
            Span(Fmt.BOLD, 0, 4),
            Span(Fmt.BOLD, 3, 7),
            Span(Fmt.BOLD, 7, 9)
        )

        assertEquals(listOf(Span(Fmt.BOLD, 0, 9)), RichText.normalize(spans, "abcdefghij"))
    }

    @Test
    fun normalizeClampsToTextAndDropsEmptyRanges() {
        val spans = listOf(Span(Fmt.ITALIC, 2, 50), Span(Fmt.BOLD, 3, 3))

        assertEquals(listOf(Span(Fmt.ITALIC, 2, 5)), RichText.normalize(spans, "abcde"))
    }

    @Test
    fun normalizeSplitsHeadingsByLine() {
        val result = RichText.normalize(listOf(Span(Fmt.H1, 0, 5)), "ab\ncd")

        assertEquals(listOf(Span(Fmt.H1, 0, 2), Span(Fmt.H1, 3, 5)), result)
    }

    @Test
    fun shiftExpandsSpanWhenInsertingInside() {
        val result = RichText.shift(listOf(Span(Fmt.BOLD, 2, 6)), p = 4, removed = 0, inserted = 3)

        assertEquals(listOf(Span(Fmt.BOLD, 2, 9)), result)
    }

    @Test
    fun shiftMovesSpanWhenDeletingBefore() {
        val result = RichText.shift(listOf(Span(Fmt.BOLD, 2, 6)), p = 0, removed = 2, inserted = 0)

        assertEquals(listOf(Span(Fmt.BOLD, 0, 4)), result)
    }

    @Test
    fun shiftRemovesSpanWhenFullyDeleted() {
        val result = RichText.shift(listOf(Span(Fmt.BOLD, 2, 6)), p = 1, removed = 6, inserted = 0)

        assertEquals(emptyList<Span>(), result)
    }

    @Test
    fun diffFindsInsertion() {
        assertEquals(Triple(2, 0, 1), RichText.diff("abc", "abXc", cursor = 3))
    }

    @Test
    fun diffFindsReplacement() {
        assertEquals(Triple(3, 2, 1), RichText.diff("hello", "help", cursor = 4))
    }

    @Test
    fun lineBoundaries() {
        val text = "ab\ncd\nef"

        assertEquals(0, RichText.lineStart(text, 0))
        assertEquals(3, RichText.lineStart(text, 4))
        assertEquals(5, RichText.lineEnd(text, 3))
        assertEquals(text.length, RichText.lineEnd(text, 6))
    }

    @Test
    fun prefixDetection() {
        val text = BULLET + "item\n" + UNCHECKED + "todo\n" + CHECKED + "done\nplain"
        val second = text.indexOf('\n') + 1
        val third = text.indexOf('\n', second) + 1
        val fourth = text.indexOf('\n', third) + 1

        assertEquals(BULLET, RichText.prefixAt(text, 0))
        assertEquals(UNCHECKED, RichText.prefixAt(text, second))
        assertEquals(CHECKED, RichText.prefixAt(text, third))
        assertNull(RichText.prefixAt(text, fourth))
    }
}
