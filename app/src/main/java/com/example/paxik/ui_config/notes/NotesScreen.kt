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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
    moveOptions: List<FolderOption>,
    currentFolderId: Long?,
    selectedNoteIds: Set<Long>,
    selectedFolderIds: Set<Long>,
    onOpenMenu: () -> Unit,
    onOpenGraph: () -> Unit,
    onOpenFolder: (Long?) -> Unit,
    onCreateNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onToggleNote: (Long) -> Unit,
    onToggleFolder: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMoveSelected: (Long?) -> Unit,
    onAddFolder: (String) -> Unit
) {
    var showFolderSheet by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val selectedCount = selectedNoteIds.size + selectedFolderIds.size
    val selecting = selectedCount > 0

    BackHandler(enabled = currentFolderId != null) {
        onOpenFolder(path.dropLast(1).lastOrNull()?.id)
    }
    BackHandler(enabled = selecting) { onClearSelection() }

    Scaffold(
        topBar = {
            if (selecting) {
                TopAppBar(
                    title = { Text("Выбрано: $selectedCount") },
                    navigationIcon = {
                        IconButton(onClick = onClearSelection) {
                            Icon(Icons.Default.Close, contentDescription = "Отменить выделение")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(spaceName) },
                    navigationIcon = {
                        IconButton(onClick = onOpenMenu) {
                            Icon(Icons.Default.Menu, contentDescription = "Пространства")
                        }
                    },
                    actions = {
                        TextButton(onClick = onOpenGraph) { Text("Граф") }
                    }
                )
            }
        },
        bottomBar = {
            if (selecting) {
                BottomAppBar(
                    actions = {
                        TextButton(onClick = onSelectAll) { Text("Выбрать все") }
                        TextButton(onClick = { showMoveSheet = true }) { Text("Переместить") }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!selecting) {
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
                        selected = folder.id in selectedFolderIds,
                        onClick = {
                            if (selecting) onToggleFolder(folder.id) else onOpenFolder(folder.id)
                        },
                        onLongClick = { onToggleFolder(folder.id) }
                    )
                }
                items(notes, key = { "note-${it.id}" }) { note ->
                    NoteCard(
                        note = note,
                        selected = note.id in selectedNoteIds,
                        onClick = {
                            if (selecting) onToggleNote(note.id) else onOpenNote(note.id)
                        },
                        onLongClick = { onToggleNote(note.id) }
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

    if (showMoveSheet) {
        MoveNoteSheet(
            options = moveOptions,
            currentFolderId = currentFolderId,
            onSelect = onMoveSelected,
            onDismiss = { showMoveSheet = false }
        )
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            title = "Удалить выбранное?",
            message = buildString {
                append("Выбрано элементов: $selectedCount. Они будут удалены без возможности восстановления.")
                if (selectedFolderIds.isNotEmpty()) {
                    append(" Папки удаляются вместе со всеми вложенными папками и заметками.")
                }
            },
            onConfirm = {
                showDeleteDialog = false
                onDeleteSelected()
            },
            onDismiss = { showDeleteDialog = false }
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