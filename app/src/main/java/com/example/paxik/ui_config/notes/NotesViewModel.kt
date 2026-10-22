package com.example.paxik.ui_config.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.paxik.data.AppDatabase
import com.example.paxik.data.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).noteDao()

    val notes: StateFlow<List<Note>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getNote(id: Long): Note? = notes.value.firstOrNull { it.id == id }

    fun addNote(title: String, text: String) = saveNote(null, title, text)

    fun saveNote(id: Long?, title: String, text: String) {
        if (title.isBlank() && text.isBlank()) return
        viewModelScope.launch {
            dao.save(Note(id = id ?: 0, title = title.trim(), text = text.trim()))
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }
}