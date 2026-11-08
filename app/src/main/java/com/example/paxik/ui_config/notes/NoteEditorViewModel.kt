package com.example.paxik.ui_config.notes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.paxik.data.AppDatabase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class NoteEditorViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).noteDao()
    private val noteId: Long = checkNotNull(savedStateHandle["noteId"])
    private var closing = false

    var title by mutableStateOf("")

    var body by mutableStateOf(TextFieldValue(""))
        private set

    var spans by mutableStateOf<List<Span>>(emptyList())
        private set

    var pending by mutableStateOf<Map<Fmt, Boolean>>(emptyMap())
        private set

    var loaded by mutableStateOf(false)
        private set

    var notFound by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            val note = dao.getById(noteId)
            if (note == null) {
                notFound = true
            } else {
                title = note.title
                body = TextFieldValue(note.text)
                spans = RichText.decode(note.spans, note.text)
            }
            loaded = true
            snapshotFlow { Triple(title, body.text, spans) }
                .drop(1)
                .debounce(300)
                .collect { (newTitle, newText, newSpans) ->
                    dao.updateContent(noteId, newTitle, newText, RichText.encode(newSpans))
                }
        }
    }

    fun onBodyChange(value: TextFieldValue) {
        val old = body
        if (value.text == old.text) {
            if (value.selection != old.selection) pending = emptyMap()
            body = value
            return
        }
        val (p, removed, inserted) = RichText.diff(old.text, value.text, value.selection.end)
        val typed = value.text.substring(p, p + inserted)
        if (removed == 0 && typed == "\n" && continueList(old.text, p)) return
        applyEdit(p, removed, typed, value.selection, value.composition, inherit = true)
    }

    fun onBodyTap(offset: Int) {
        val text = body.text
        if (text.isEmpty()) return
        val ls = RichText.lineStart(text, offset)
        val prefix = RichText.prefixAt(text, ls)
        if ((prefix == UNCHECKED || prefix == CHECKED) && offset <= ls + 1) {
            val glyph = if (prefix == UNCHECKED) "☑" else "☐"
            applyEdit(ls, 1, glyph, body.selection, null, inherit = false)
        }
    }

    fun isActive(fmt: Fmt): Boolean {
        val text = body.text
        val sel = body.selection
        if (fmt == Fmt.H1 || fmt == Fmt.H2) {
            val ls = RichText.lineStart(text, sel.min)
            val le = RichText.lineEnd(text, ls)
            return if (le > ls) RichText.covers(spans, fmt, ls, le) else pending[fmt] == true
        }
        if (sel.collapsed) {
            pending[fmt]?.let { return it }
            return sel.start > 0 && RichText.covers(spans, fmt, sel.start - 1, sel.start)
        }
        return RichText.covers(spans, fmt, sel.min, sel.max)
    }

    fun isLineKind(own: Set<String>): Boolean {
        val text = body.text
        val ls = RichText.lineStart(text, body.selection.min)
        return RichText.prefixAt(text, ls) in own
    }

    fun toggleInline(fmt: Fmt) {
        val sel = body.selection
        if (sel.collapsed) {
            pending = pending + (fmt to !isActive(fmt))
        } else {
            spans = if (RichText.covers(spans, fmt, sel.min, sel.max)) {
                RichText.remove(spans, fmt, sel.min, sel.max, body.text)
            } else {
                RichText.add(spans, fmt, sel.min, sel.max, body.text)
            }
        }
    }

    fun toggleHeading(fmt: Fmt) {
        val text = body.text
        val sel = body.selection
        val lines = selectedLineStarts().map { it to RichText.lineEnd(text, it) }
        val filled = lines.filter { it.second > it.first }
        if (filled.isEmpty()) {
            if (sel.collapsed) {
                val other = if (fmt == Fmt.H1) Fmt.H2 else Fmt.H1
                pending = pending + (fmt to (pending[fmt] != true)) + (other to false)
            }
            return
        }
        val allHave = filled.all { RichText.covers(spans, fmt, it.first, it.second) }
        var updated = spans
        for ((start, end) in filled) {
            updated = RichText.remove(updated, Fmt.H1, start, end, text)
            updated = RichText.remove(updated, Fmt.H2, start, end, text)
            if (!allHave) updated = RichText.add(updated, fmt, start, end, text)
        }
        spans = updated
    }

    fun toggleBullet() = toggleLinePrefix(setOf(BULLET), BULLET)

    fun toggleCheckbox() = toggleLinePrefix(setOf(UNCHECKED, CHECKED), UNCHECKED)

    fun close(onDone: () -> Unit) {
        if (closing) return
        closing = true
        viewModelScope.launch {
            if (title.isBlank() && body.text.isBlank()) {
                dao.deleteById(noteId)
            } else {
                dao.updateContent(noteId, title, body.text, RichText.encode(spans))
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (closing) return
        closing = true
        viewModelScope.launch {
            dao.deleteById(noteId)
            onDone()
        }
    }

    private fun continueList(text: String, p: Int): Boolean {
        val ls = RichText.lineStart(text, p)
        val prefix = RichText.prefixAt(text, ls) ?: return false
        if (p < ls + prefix.length) return false
        val le = RichText.lineEnd(text, ls)
        if (le == ls + prefix.length && p == le) {
            applyEdit(ls, prefix.length, "", TextRange(ls), null, inherit = false)
            return true
        }
        val next = if (prefix == BULLET) BULLET else UNCHECKED
        val insert = "\n" + next
        applyEdit(p, 0, insert, TextRange(p + insert.length), null, inherit = true)
        return true
    }

    private fun applyEdit(
        p: Int,
        removed: Int,
        insert: String,
        selection: TextRange,
        composition: TextRange?,
        inherit: Boolean
    ) {
        val oldText = body.text
        val newText = oldText.substring(0, p) + insert + oldText.substring(p + removed)
        var updated = RichText.shift(spans, p, removed, insert.length)
        if (insert.isNotEmpty()) {
            for (fmt in Fmt.values()) {
                val lineLevel = fmt == Fmt.H1 || fmt == Fmt.H2
                val inherited = p > 0 && RichText.covers(spans, fmt, p - 1, p)
                val desired = when {
                    !inherit -> false
                    lineLevel && insert.contains('\n') -> false
                    else -> pending[fmt] ?: inherited
                }
                updated = if (desired) {
                    RichText.add(updated, fmt, p, p + insert.length, newText)
                } else {
                    RichText.remove(updated, fmt, p, p + insert.length, newText)
                }
            }
        }
        spans = RichText.normalize(updated, newText)
        pending = emptyMap()
        body = TextFieldValue(newText, selection, composition)
    }

    private fun selectedLineStarts(): List<Int> {
        val text = body.text
        val sel = body.selection
        val endPos = if (sel.collapsed) sel.max else maxOf(sel.min, sel.max - 1)
        val result = ArrayList<Int>()
        var ls = RichText.lineStart(text, sel.min)
        while (ls <= endPos) {
            result.add(ls)
            val le = RichText.lineEnd(text, ls)
            if (le >= text.length) break
            ls = le + 1
        }
        return result
    }

    private fun toggleLinePrefix(own: Set<String>, target: String) {
        pending = emptyMap()
        val starts = selectedLineStarts()
        val allHave = starts.all { RichText.prefixAt(body.text, it) in own }
        var selStart = body.selection.start
        var selEnd = body.selection.end
        for (ls in starts.reversed()) {
            val current = RichText.prefixAt(body.text, ls)
            val replaceLength: Int
            val insert: String
            when {
                allHave -> {
                    replaceLength = current?.length ?: 0
                    insert = ""
                }
                current in own -> continue
                current != null -> {
                    replaceLength = current.length
                    insert = target
                }
                else -> {
                    replaceLength = 0
                    insert = target
                }
            }
            applyEdit(ls, replaceLength, insert, body.selection, null, inherit = false)
            val delta = insert.length - replaceLength
            selStart = shiftOffset(selStart, ls, delta)
            selEnd = shiftOffset(selEnd, ls, delta)
        }
        body = body.copy(selection = TextRange(selStart, selEnd))
    }

    private fun shiftOffset(offset: Int, lineStart: Int, delta: Int): Int =
        if (offset < lineStart) offset else maxOf(lineStart, offset + delta)
}