package com.example.paxik.ui_config.notes

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.paxik.data.AppDatabase
import com.example.paxik.data.Folder
import com.example.paxik.data.Note
import com.example.paxik.data.Space
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FolderOption(val id: Long?, val name: String, val parents: String)

class NotesViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val noteDao = db.noteDao()
    private val folderDao = db.folderDao()
    private val spaceDao = db.spaceDao()
    private val prefs = app.getSharedPreferences("pax", Context.MODE_PRIVATE)
    private val started = SharingStarted.WhileSubscribed(5000)

    private val savedSpaceId = MutableStateFlow(prefs.getLong("space_id", 1L))
    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId

    val spaces: StateFlow<List<Space>> =
        spaceDao.getAll().stateIn(viewModelScope, started, emptyList())

    val activeSpaceId: StateFlow<Long> =
        combine(spaces, savedSpaceId) { list, saved ->
            if (list.isEmpty() || list.any { it.id == saved }) saved else list.first().id
        }.stateIn(viewModelScope, started, savedSpaceId.value)

    private val allNotes = noteDao.getAll().stateIn(viewModelScope, started, emptyList())
    private val allFolders = folderDao.getAll().stateIn(viewModelScope, started, emptyList())

    val notes: StateFlow<List<Note>> =
        combine(allNotes, activeSpaceId, _currentFolderId) { list, spaceId, folderId ->
            list.filter { it.spaceId == spaceId && it.folderId == folderId }
        }.stateIn(viewModelScope, started, emptyList())

    val folders: StateFlow<List<Folder>> =
        combine(allFolders, activeSpaceId, _currentFolderId) { list, spaceId, folderId ->
            list.filter { it.spaceId == spaceId && it.parentId == folderId }
        }.stateIn(viewModelScope, started, emptyList())

    val path: StateFlow<List<Folder>> =
        combine(allFolders, _currentFolderId) { list, id -> pathTo(id, list.associateBy { it.id }) }
            .stateIn(viewModelScope, started, emptyList())

    val folderOptions: StateFlow<List<FolderOption>> =
        combine(allFolders, activeSpaceId, spaces) { list, spaceId, spaceList ->
            val spaceName = spaceList.firstOrNull { it.id == spaceId }?.name ?: "Главная"
            val byId = list.associateBy { it.id }
            val options = list
                .filter { it.spaceId == spaceId }
                .map { folder ->
                    FolderOption(
                        id = folder.id,
                        name = folder.name,
                        parents = pathTo(folder.parentId, byId).joinToString(" / ") { it.name }
                    )
                }
                .sortedBy {
                    val full = if (it.parents.isEmpty()) it.name else it.parents + " / " + it.name
                    full.lowercase()
                }
            listOf(FolderOption(null, spaceName, "")) + options
        }.stateIn(viewModelScope, started, listOf(FolderOption(null, "Главная", "")))

    fun openFolder(id: Long?) {
        _currentFolderId.value = id
    }

    fun selectSpace(id: Long) {
        savedSpaceId.value = id
        _currentFolderId.value = null
        prefs.edit().putLong("space_id", id).apply()
    }

    fun addSpace(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = spaceDao.insert(Space(name = name.trim()))
            selectSpace(id)
        }
    }

    fun renameSpace(id: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { spaceDao.rename(id, name.trim()) }
    }

    fun deleteSpace(id: Long) {
        val list = spaces.value
        if (list.size <= 1) return
        if (activeSpaceId.value == id) {
            selectSpace(list.first { it.id != id }.id)
        }
        viewModelScope.launch { spaceDao.deleteWithContent(id) }
    }

    fun createNote(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = noteDao.insert(
                Note(
                    title = "",
                    text = "",
                    folderId = _currentFolderId.value,
                    spaceId = activeSpaceId.value
                )
            )
            onCreated(id)
        }
    }

    fun moveNote(id: Long, folderId: Long?) {
        viewModelScope.launch { noteDao.moveToFolder(id, folderId) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { noteDao.deleteById(id) }
    }

    fun addFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            folderDao.save(
                Folder(
                    name = name.trim(),
                    parentId = _currentFolderId.value,
                    spaceId = activeSpaceId.value
                )
            )
        }
    }

    fun deleteFolder(id: Long) {
        val all = allFolders.value
        val ids = mutableListOf(id)
        var index = 0
        while (index < ids.size) {
            val parent = ids[index++]
            all.filter { it.parentId == parent }.forEach { ids.add(it.id) }
        }
        viewModelScope.launch { folderDao.deleteTree(ids) }
    }

    private fun pathTo(id: Long?, byId: Map<Long, Folder>): List<Folder> {
        val result = ArrayList<Folder>()
        var current = id
        while (current != null) {
            val folder = byId[current] ?: break
            result.add(0, folder)
            current = folder.parentId
        }
        return result
    }
}