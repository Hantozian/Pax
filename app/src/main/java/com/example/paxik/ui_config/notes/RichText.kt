package com.example.paxik.ui_config.notes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

enum class Fmt(val code: String) {
    BOLD("b"),
    ITALIC("i"),
    UNDERLINE("u"),
    STRIKE("s"),
    H1("1"),
    H2("2")
}

data class Span(val fmt: Fmt, val start: Int, val end: Int)

const val BULLET = "• "
const val UNCHECKED = "☐ "
const val CHECKED = "☑ "

object RichText {
    fun encode(spans: List<Span>): String =
        spans.joinToString(";") { "${it.fmt.code}:${it.start}:${it.end}" }

    fun decode(raw: String, text: String): List<Span> {
        if (raw.isBlank()) return emptyList()
        val result = ArrayList<Span>()
        for (part in raw.split(";")) {
            val items = part.split(":")
            if (items.size != 3) continue
            val fmt = Fmt.entries.firstOrNull { it.code == items[0] } ?: continue
            val start = items[1].toIntOrNull() ?: continue
            val end = items[2].toIntOrNull() ?: continue
            result.add(Span(fmt, start, end))
        }
        return normalize(result, text)
    }

    fun normalize(spans: List<Span>, text: String): List<Span> {
        val result = ArrayList<Span>()
        for (fmt in Fmt.entries) {
            val ranges = spans
                .filter { it.fmt == fmt }
                .map { it.start.coerceIn(0, text.length) to it.end.coerceIn(0, text.length) }
                .filter { it.second > it.first }
                .sortedBy { it.first }
            val merged = ArrayList<Pair<Int, Int>>()
            for (range in ranges) {
                val last = merged.lastOrNull()
                if (last != null && range.first <= last.second) {
                    merged[merged.size - 1] = last.first to maxOf(last.second, range.second)
                } else {
                    merged.add(range)
                }
            }
            val isHeading = fmt == Fmt.H1 || fmt == Fmt.H2
            for (range in merged) {
                if (!isHeading) {
                    result.add(Span(fmt, range.first, range.second))
                } else {
                    var from = range.first
                    for (i in range.first until range.second) {
                        if (text[i] == '\n') {
                            if (i > from) result.add(Span(fmt, from, i))
                            from = i + 1
                        }
                    }
                    if (range.second > from) result.add(Span(fmt, from, range.second))
                }
            }
        }
        return result
    }

    fun covers(spans: List<Span>, fmt: Fmt, start: Int, end: Int): Boolean =
        spans.any { it.fmt == fmt && it.start <= start && it.end >= end }

    fun add(spans: List<Span>, fmt: Fmt, start: Int, end: Int, text: String): List<Span> =
        if (end <= start) spans else normalize(spans + Span(fmt, start, end), text)

    fun remove(spans: List<Span>, fmt: Fmt, start: Int, end: Int, text: String): List<Span> {
        if (end <= start) return spans
        val result = ArrayList<Span>()
        for (span in spans) {
            if (span.fmt != fmt || span.end <= start || span.start >= end) {
                result.add(span)
            } else {
                if (span.start < start) result.add(Span(fmt, span.start, start))
                if (span.end > end) result.add(Span(fmt, end, span.end))
            }
        }
        return normalize(result, text)
    }

    fun shift(spans: List<Span>, p: Int, removed: Int, inserted: Int): List<Span> {
        val q = p + removed
        val delta = inserted - removed
        val result = ArrayList<Span>()
        for (span in spans) {
            val a = span.start
            val b = span.end
            when {
                b <= p -> result.add(span)
                a >= q -> result.add(Span(span.fmt, a + delta, b + delta))
                a < p && b > q -> result.add(Span(span.fmt, a, b + delta))
                a >= p && b <= q -> {}
                a < p -> result.add(Span(span.fmt, a, p))
                else -> result.add(Span(span.fmt, p + inserted, b + delta))
            }
        }
        return result
    }

    fun diff(old: String, new: String, cursor: Int): Triple<Int, Int, Int> {
        var suffix = 0
        val tail = new.length - cursor
        val cursorUsable = cursor in 0..new.length &&
                tail <= old.length &&
                old.endsWith(new.substring(cursor))
        if (cursorUsable) {
            suffix = tail
        } else {
            val maxSuffix = minOf(old.length, new.length)
            while (suffix < maxSuffix &&
                old[old.length - 1 - suffix] == new[new.length - 1 - suffix]
            ) {
                suffix++
            }
        }
        val maxPrefix = minOf(old.length, new.length) - suffix
        var prefix = 0
        while (prefix < maxPrefix && old[prefix] == new[prefix]) prefix++
        return Triple(prefix, old.length - suffix - prefix, new.length - suffix - prefix)
    }

    fun lineStart(text: String, pos: Int): Int {
        val p = pos.coerceIn(0, text.length)
        return if (p == 0) 0 else text.lastIndexOf('\n', p - 1) + 1
    }

    fun lineEnd(text: String, pos: Int): Int {
        val p = pos.coerceIn(0, text.length)
        val index = text.indexOf('\n', p)
        return if (index < 0) text.length else index
    }

    fun prefixAt(text: String, lineStart: Int): String? = when {
        text.startsWith(BULLET, lineStart) -> BULLET
        text.startsWith(UNCHECKED, lineStart) -> UNCHECKED
        text.startsWith(CHECKED, lineStart) -> CHECKED
        else -> null
    }

    fun buildStyled(text: String, spans: List<Span>, doneColor: Color): AnnotatedString {
        val length = text.length
        val safe = spans
            .map { Span(it.fmt, it.start.coerceIn(0, length), it.end.coerceIn(0, length)) }
            .filter { it.end > it.start }

        val glyphs = ArrayList<Pair<Int, Int>>()
        val dones = ArrayList<Pair<Int, Int>>()
        var ls = 0
        while (ls <= length) {
            val found = text.indexOf('\n', ls)
            val le = if (found < 0) length else found
            val checked = text.startsWith(CHECKED, ls)
            if (checked || text.startsWith(UNCHECKED, ls)) {
                glyphs.add(ls to ls + 1)
                if (checked && le > ls + 2) dones.add(ls + 2 to le)
            }
            if (le >= length) break
            ls = le + 1
        }

        val points = java.util.TreeSet<Int>()
        points.add(0)
        points.add(length)
        safe.forEach {
            points.add(it.start)
            points.add(it.end)
        }
        glyphs.forEach {
            points.add(it.first)
            points.add(it.second)
        }
        dones.forEach {
            points.add(it.first)
            points.add(it.second)
        }

        val builder = AnnotatedString.Builder(text)
        val ordered = points.toList()
        for (i in 0 until ordered.size - 1) {
            val a = ordered[i]
            val b = ordered[i + 1]
            if (b <= a) continue
            val active = safe.filter { it.start <= a && it.end >= b }.map { it.fmt }
            val glyph = glyphs.any { it.first <= a && it.second >= b }
            val done = dones.any { it.first <= a && it.second >= b }
            val style = styleFor(active, glyph, done, doneColor) ?: continue
            builder.addStyle(style, a, b)
        }
        return builder.toAnnotatedString()
    }

    private fun styleFor(
        active: List<Fmt>,
        glyph: Boolean,
        done: Boolean,
        doneColor: Color
    ): SpanStyle? {
        val bold = Fmt.BOLD in active || Fmt.H1 in active || Fmt.H2 in active
        val italic = Fmt.ITALIC in active
        val decorations = ArrayList<TextDecoration>()
        if (Fmt.UNDERLINE in active) decorations.add(TextDecoration.Underline)
        if (Fmt.STRIKE in active || done) decorations.add(TextDecoration.LineThrough)
        val size: TextUnit = when {
            Fmt.H1 in active -> 28.sp
            Fmt.H2 in active -> 22.sp
            glyph -> 22.sp
            else -> TextUnit.Unspecified
        }
        if (!bold && !italic && decorations.isEmpty() && size == TextUnit.Unspecified && !done) {
            return null
        }
        return SpanStyle(
            fontWeight = if (bold) FontWeight.Bold else null,
            fontStyle = if (italic) FontStyle.Italic else null,
            textDecoration = when (decorations.size) {
                0 -> null
                1 -> decorations[0]
                else -> TextDecoration.combine(decorations)
            },
            fontSize = size,
            color = if (done) doneColor else Color.Unspecified
        )
    }
}