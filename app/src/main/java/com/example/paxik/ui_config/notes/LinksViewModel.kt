package com.example.paxik.ui_config.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.paxik.data.AppDatabase
import com.example.paxik.data.Link
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LinksViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val linkDao = db.linkDao()

    val graph: StateFlow<GraphData> =
        combine(
            db.spaceDao().getAll(),
            db.folderDao().getAll(),
            db.noteDao().getAll(),
            linkDao.getAll()
        ) { spaces, folders, notes, links ->
            GraphData.build(spaces, folders, notes, links)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GraphData.EMPTY)

    fun addLink(from: NodeRef, to: NodeRef) {
        if (from == to) return
        viewModelScope.launch {
            linkDao.insert(
                Link(fromType = from.type, fromId = from.id, toType = to.type, toId = to.id)
            )
        }
    }

    fun removeLink(from: NodeRef, to: NodeRef) {
        viewModelScope.launch { linkDao.delete(from.type, from.id, to.type, to.id) }
    }
}
