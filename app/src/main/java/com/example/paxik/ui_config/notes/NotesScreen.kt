package com.example.paxik.ui_config.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.paxik.data.Folder
import com.example.paxik.data.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    spaceName: String,
    notes: List<Note>,
    folders: List<Folder>,
    path: List<Folder>,
    folderOptions: List<FolderOption>,
    currentFolderId: Long?,
    onOpenMenu: () -> Unit,
    onOpenFolder: (Long?) -> Unit,
    onCreateNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onMoveNote: (Long, Long?) -> Unit,
    onAddFolder: (String) -> Unit,
    onDeleteFolder: (Long) -> Unit
) {
    var showFolderSheet by remember { mutableStateOf(false) }
    var noteForActions by remember { mutableStateOf<Note?>(null) }
    var noteToMove by remember { mutableStateOf<Note?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }

    BackHandler(enabled = currentFolderId != null) {
        onOpenFolder(path.dropLast(1).lastOrNull()?.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(spaceName) },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Пространства")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallFloatingActionButton(onClick = { showFolderSheet = true }) {
                    Text("📁")
                }
                FloatingActionButton(onClick = onCreateNote) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить заметку")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (path.isNotEmpty()) {
                Breadcrumbs(rootName = spaceName, path = path, onOpenFolder = onOpenFolder)
            }

            if (folders.isEmpty() && notes.isEmpty()) {
                Text(text = "Здесь пока пусто", modifier = Modifier.padding(16.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders, key = { "folder-${it.id}" }) { folder ->
                    FolderCard(
                        folder = folder,
                        onClick = { onOpenFolder(folder.id) },
                        onLongClick = { folderToDelete = folder }
                    )
                }
                items(notes, key = { "note-${it.id}" }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onOpenNote(note.id) },
                        onLongClick = { noteForActions = note }
                    )
                }
            }
        }
    }

    if (showFolderSheet) {
        NameSheet(
            title = "Новая папка",
            confirmText = "Создать",
            onConfirm = onAddFolder,
            onDismiss = { showFolderSheet = false }
        )
    }

    noteForActions?.let { note ->
        NoteActionsSheet(
            note = note,
            onMove = { noteToMove = note },
            onDelete = { noteToDelete = note },
            onDismiss = { noteForActions = null }
        )
    }

    noteToMove?.let { note ->
        MoveNoteSheet(
            options = folderOptions,
            currentFolderId = note.folderId,
            onSelect = { target -> onMoveNote(note.id, target) },
            onDismiss = { noteToMove = null }
        )
    }

    noteToDelete?.let { note ->
        ConfirmDeleteDialog(
            title = "Удалить заметку?",
            message = "«${note.title.ifBlank { note.text.ifBlank { "Без названия" } }}» будет удалена без возможности восстановления.",
            onConfirm = {
                onDeleteNote(note.id)
                noteToDelete = null
            },
            onDismiss = { noteToDelete = null }
        )
    }

    folderToDelete?.let { folder ->
        ConfirmDeleteDialog(
            title = "Удалить папку?",
            message = "Папка «${folder.name}» будет удалена вместе со всеми вложенными папками и заметками.",
            onConfirm = {
                onDeleteFolder(folder.id)
                folderToDelete = null
            },
            onDismiss = { folderToDelete = null }
        )
    }
}

@Composable
private fun Breadcrumbs(
    rootName: String,
    path: List<Folder>,
    onOpenFolder: (Long?) -> Unit
) {
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { onOpenFolder(null) }) { Text(rootName) }
        path.forEach { folder ->
            Text("›")
            TextButton(onClick = { onOpenFolder(folder.id) }) { Text(folder.name) }
        }
    }
}