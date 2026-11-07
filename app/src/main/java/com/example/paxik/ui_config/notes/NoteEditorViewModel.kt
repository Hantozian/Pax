package com.example.paxik.ui_config.notes

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
    var text by mutableStateOf("")

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
                text = note.text
            }
            loaded = true
            snapshotFlow { title to text }
                .drop(1)
                .debounce(300)
                .collect { (newTitle, newText) ->
                    dao.updateContent(noteId, newTitle, newText)
                }
        }
    }

    fun close(onDone: () -> Unit) {
        if (closing) return
        closing = true
        viewModelScope.launch {
            if (title.isBlank() && text.isBlank()) {
                dao.deleteById(noteId)
            } else {
                dao.updateContent(noteId, title, text)
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
}