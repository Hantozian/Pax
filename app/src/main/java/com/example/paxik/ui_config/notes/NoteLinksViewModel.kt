package com.example.paxik.ui_config.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.paxik.data.AppDatabase
import com.example.paxik.data.Link
import com.example.paxik.data.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Relations(
    val outgoing: List<Note>,
    val incoming: List<Note>,
    val candidates: List<Note>
)

class NoteLinksViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val noteDao = db.noteDao()
    private val linkDao = db.linkDao()
    private val noteId: Long = checkNotNull(savedStateHandle["noteId"])

    val relations: StateFlow<Relations> =
        combine(noteDao.getAll(), linkDao.getAll()) { notes, links ->
            val me = notes.firstOrNull { it.id == noteId }
            if (me == null) {
                Relations(emptyList(), emptyList(), emptyList())
            } else {
                val byId = notes.associateBy { it.id }
                val outgoing = links.filter { it.fromId == noteId }.mapNotNull { byId[it.toId] }
                val incoming = links.filter { it.toId == noteId }.mapNotNull { byId[it.fromId] }
                val linkedIds = outgoing.map { it.id }.toSet()
                val candidates = notes.filter {
                    it.spaceId == me.spaceId && it.id != noteId && it.id !in linkedIds
                }
                Relations(outgoing, incoming, candidates)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            Relations(emptyList(), emptyList(), emptyList())
        )

    fun addLink(toId: Long) {
        if (toId == noteId) return
        viewModelScope.launch { linkDao.insert(Link(fromId = noteId, toId = toId)) }
    }

    fun removeOutgoing(toId: Long) {
        viewModelScope.launch { linkDao.delete(noteId, toId) }
    }

    fun removeIncoming(fromId: Long) {
        viewModelScope.launch { linkDao.delete(fromId, noteId) }
    }
}